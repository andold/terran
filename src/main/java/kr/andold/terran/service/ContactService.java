package kr.andold.terran.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.io.chain.ChainingTextStringParser;
import jakarta.transaction.Transactional;
import kr.andold.terran.domain.ContactDomain;
import kr.andold.terran.domain.ContactDownload;
import kr.andold.terran.domain.ContactMapDomain;
import kr.andold.terran.entity.ContactEntity;
import kr.andold.terran.entity.ContactMapEntity;
import kr.andold.terran.param.ContactParam;
import kr.andold.terran.repository.ContactRepository;
import kr.andold.terran.repository.ContactSpecification;
import kr.andold.utils.Utility;
import kr.andold.utils.persist.CrudList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ContactService {
	@Autowired
	private ContactRepository repository;

	@Autowired
	private ContactMapService contactMapService;

	private static final Sort DEFAULT_SORT = Sort.by(Direction.ASC, "fn", "value");
	
	public List<ContactDomain> search(ContactParam param) {
		List<ContactEntity> entities;
		if (param == null) {
			entities = repository.findAllByOrderByFnAsc();
		} else {
//			entities = repository.findByContentContainsOrderByFnAsc(param.getKeyword());
			entities = repository.findAll(ContactSpecification.searchWith(param), DEFAULT_SORT);
		}

		List<ContactDomain> domains = new ArrayList<ContactDomain>();
		for (ContactEntity entity : entities) {
			ContactDomain domain = ContactDomain.of(entity);
			expand(domain);
			domains.add(domain);
		}

		sort(domains);

		return domains;
	}

	private void sort(List<ContactDomain> domains) {
		domains.sort(new Comparator<ContactDomain>() {
			@Override
			public int compare(ContactDomain o1, ContactDomain o2) {
				return o1.getFn().compareTo(o2.getFn());
			}
		});

		for (ContactDomain domain : domains) {
			contactMapService.sort(domain.getMaps());
		}
	}

	private void expand(ContactDomain domain) {
		contactMapService.expand(domain);
		domain.setFn(safeFn(domain));
		domain.setValue(ContactDomain.safeValue(domain));
	}

	private String safeFn(List<Object> listObject) {
		if (listObject == null || listObject.size() < 2) {
			return "N/A";
		}

		@SuppressWarnings("unchecked")
		List<Object> fields = (List<Object>) listObject.get(1);
		String fn = fieldValue(fields, "fn", "N/A");

		return fn;
	}

	private String safeFn(ContactDomain domain) {
		if (domain == null) {
			return "N/A";
		}

		String fn = domain.getFn();
		if (fn != null && !fn.isBlank()) {
			return fn;
		}

		String fromVcard = safeFn(domain.getVcard());
		if (!"N/A".equalsIgnoreCase(fromVcard)) {
			return fromVcard;
		}

		String fromContent = safeFn(domain.getContent());

		return fromContent;
	}

	private String safeFn(String content) {
		ChainingTextStringParser parsed = Ezvcard.parse(content);
		if (parsed == null) {
			return "N/A";
		}

		VCard vcard = parsed.first();
		if (vcard == null) {
			return "N/A";
		}

		String json = Ezvcard.writeJson(vcard).go();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			@SuppressWarnings("unchecked")
			List<Object> fields = objectMapper.readValue(json, List.class);
			if (fields == null || fields.size() < 2) {
				return "N/A";
			}

			String fn = fieldValue((List<Object>) fields.get(1), "fn", "N/A");
			return fn;

		} catch (Exception e) {
		}

		return "N/A";
	}

	public int batch(ContactParam param) {
		if (param == null) {
			return 0;
		}

		int count = 0;
		List<ContactDomain> creates = param.getCreates();
		List<ContactDomain> updates = param.getUpdates();
		List<ContactDomain> removes = param.getRemoves();

		if (creates != null) {
			List<ContactDomain> created = create(creates);
			count += Utility.size(created);
		}
		if (removes != null) {
			count += delete(removes);
		}
		if (updates != null) {
			count += Utility.size(update(updates));
		}

		return count;
	}

	@Modifying
	private List<ContactDomain> update(List<ContactDomain> updates) {
		if (updates == null || updates.isEmpty()) {
			return new ArrayList<>();
		}

		log.info("{} update(#{})", Utility.indentStart(), Utility.size(updates));
		long started = System.currentTimeMillis();

		List<ContactEntity> entities = new ArrayList<ContactEntity>();
		CrudList<ContactMapDomain> crudContainer = CrudList.<ContactMapDomain>builder().build();
		for (ContactDomain contact : updates) {
			Optional<ContactEntity> entityReturn = read(contact.getId());
			if (entityReturn.isEmpty()) {
				continue;
			}
			ContactEntity entity = entityReturn.get();
			prepareUpdate(entity, contact);
			entities.add(entity);
			CrudList<ContactMapDomain> crud = contactMapService.differ(contact);
			if (crud.isEmpty()) {
				continue;
			}

			prepareMap(contact, crud);
			crudContainer.add(crud);
			log.info("{} update(#{}) - 『{}』『{}』", Utility.indentMiddle(), Utility.size(updates), crudContainer, crud);
		}
		contactMapService.put(crudContainer);

		List<ContactDomain> domains = new ArrayList<ContactDomain>();
		try {
			List<ContactEntity> list = repository.saveAllAndFlush(entities);
			for (ContactEntity entity : list) {
				ContactDomain domain = ContactDomain.of(entity);
				domains.add(domain);
			}
		} catch (Exception e) {
			for (ContactDomain domain : updates) {
				ContactDomain updated = update(domain.getId(), domain);
				domains.add(updated);
			}

			repository.flush();
		}

		log.info("{} 『#{}』 update(#{}) - {}", Utility.indentEnd(), Utility.size(domains), Utility.size(updates),
				Utility.toStringPastTimeReadable(started));
		return domains;
	}

	private void prepareUpdate(ContactEntity entity, ContactDomain domain) {
		Utility.copyPropertiesNotNull(domain, entity);
		entity.setUpdated(new Date());
	}

	private void prepareMap(ContactDomain contact, CrudList<ContactMapDomain> crud) {
		for (ContactMapDomain map : crud.getCreates()) {
			map.setVcardId(contact.getId());
		}
		for (ContactMapDomain map : crud.getUpdates()) {
			map.setVcardId(contact.getId());
		}
	}

	@Modifying
	public ContactDomain update(Integer id, ContactDomain afterDomain) {
		log.info("{} update({}, {})", Utility.indentStart(), id, afterDomain);
		long started = System.currentTimeMillis();

		Optional<ContactEntity> beforeEntity = read(id);
		if (!beforeEntity.isPresent()) {
			return null;
		}

		ContactDomain beforeDomain = ContactDomain.of(beforeEntity.get());
		ContactDomain mergedDomain = merge(beforeDomain, afterDomain);
		ContactEntity mergedEntity = ContactDomain.toEntity(mergedDomain);
		mergedEntity.setUpdated(new Date());
		try {
			ContactEntity updated = repository.saveAndFlush(mergedEntity);
			CrudList<ContactMapDomain> crud = contactMapService.differ(beforeDomain.getMaps(), mergedDomain.getMaps());
			crud.getRemoves().clear();
			int changedMap = contactMapService.batch(crud);
			ContactDomain result = ContactDomain.of(updated);
			expand(result);

			log.info("{} {}:#{} update({}, {}) - {}", Utility.indentStart(), result, changedMap, id, afterDomain,
					Utility.toStringPastTimeReadable(started));
			return result;
		} catch (Exception e) {
			log.warn("{} update({}, 『{}』) - 『{}』『{}』『{}』", Utility.indentMiddle(), id, afterDomain, beforeDomain,
					mergedDomain, mergedEntity);
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		log.warn("{} {} update({}, {}) - {}", Utility.indentStart(), null, id, afterDomain,
				Utility.toStringPastTimeReadable(started));
		return null;
	}

	private Optional<ContactEntity> read(Integer id) {
		return repository.findById(id);
	}

	@Transactional
	@Modifying
	private int delete(List<ContactDomain> removes) {
		List<ContactEntity> entities = new ArrayList<ContactEntity>();
		for (ContactDomain contact : removes) {
			ContactEntity entity = ContactDomain.toEntity(contact);
			entities.add(entity);
		}

		try {
			repository.deleteAll(entities);
			repository.flush();
			return Utility.size(entities);
		} catch (Exception e) {
			log.warn("{} delete(#{}) - Exception:: {}", Utility.indentMiddle(), Utility.size(removes),
					e.getLocalizedMessage());
			int deleted = 0;
			for (ContactEntity entity : entities) {
				try {
					repository.delete(entity);
					deleted++;
				} catch (Exception f) {
					log.warn("{} delete(#{}) - {} - Exception:: {}", Utility.indentMiddle(), Utility.size(removes),
							entity, f.getLocalizedMessage());
				}
			}
			repository.flush();
			return deleted;
		}
	}

	@Transactional
	@Modifying
	private List<ContactDomain> create(List<ContactDomain> creates) {
		if (creates == null || creates.isEmpty()) {
			return new ArrayList<>();
		}

		log.info("{} create(#{})", Utility.indentStart(), Utility.size(creates));
		long started = System.currentTimeMillis();
		
		List<ContactDomain> domains = new ArrayList<ContactDomain>();
		for (ContactDomain domain: creates) {
			ContactDomain created = create(domain);
			domains.add(created);
		}

		log.info("{} 『#{}』 create(#{}) - {}", Utility.indentEnd(), Utility.size(domains), Utility.size(creates),
				Utility.toStringPastTimeReadable(started));
		return domains;
	}

	private ContactDomain create(ContactDomain domain) {
		log.info("{} create(『{}』)", Utility.indentStart(), domain);
		long started = System.currentTimeMillis();

		try {
			prepareCreate(domain);
			ContactEntity entity = ContactDomain.toEntity(domain);
			ContactEntity createdEntity = repository.saveAndFlush(entity);
			ContactDomain created = ContactDomain.of(createdEntity);
			created.setMaps(domain.getMaps());
			CrudList<ContactMapDomain> crud = contactMapService.differ(created);
			prepareMap(created, crud);
			contactMapService.put(crud);
			log.info("{} 『{}』 create(『{}』) - {}", Utility.indentEnd(), created, domain, Utility.toStringPastTimeReadable(started));
			return created;
		} catch (Exception e) {
		}

		log.info("{} Exception:『{}』 create(『{}』) - {}", Utility.indentEnd(), domain, domain, Utility.toStringPastTimeReadable(started));
		return domain;
	}

	private void prepareCreate(ContactDomain domain) {
		Date date = new Date();
		domain.setId(null);
		if (domain.getValue() == null) {
			domain.setValue(0);
		}
		if (domain.getContent() == null) {
			domain.setContent("");
		}
		domain.setMaps(contactMapService.merge(domain.getMaps(), domain.getContent()));
		domain.setCreated(date);
		domain.setUpdated(date);
	}

	public ContactParam upload(MultipartFile file) throws UnsupportedEncodingException, IOException {
		log.info("{} upload({})", Utility.indentStart(), Utility.toStringJson(file, 64, 32));
		long started = System.currentTimeMillis();

		String text = Utility.extractStringFromText(file.getInputStream());
		String contentType = file.getContentType();
		switch (contentType) {
		case "text/vcard":
		case "text/x-vcard":
			ContactParam resultVcard = uploadVcard(text);

			log.info("{} {} - upload({}) - {}", Utility.indentEnd(), resultVcard, Utility.toStringJson(file, 32, 32),
					Utility.toStringPastTimeReadable(started));
			return resultVcard;

		case "application/json":
			ContactParam resultJson = uploadJson(text);

			log.info("{} {} - upload({}) - {}", Utility.indentEnd(), resultJson, Utility.toStringJson(file, 32, 32),
					Utility.toStringPastTimeReadable(started));
			return resultJson;

		default:
			log.warn("{} NOT_SUPPORT:{} upload({})", Utility.indentMiddle(), contentType,
					Utility.toStringJson(file, 64, 32));
			break;
		}

		log.info("{} {} - upload({}) - {}", Utility.indentEnd(), "NOT_SUPPORT", Utility.toStringJson(file, 32, 32),
				Utility.toStringPastTimeReadable(started));
		return null;
	}

	public ContactParam uploadJson(String text) {
		log.info("{} uploadJson(『{}』)", Utility.indentStart(), Utility.toStringJson(text, 32, 32));
		long started = System.currentTimeMillis();

		String[] lines = text.split("\n");
		List<ContactDomain> domains = new ArrayList<ContactDomain>();
		for (String line : lines) {
			ContactDomain domain = ContactDomain.of(line);
			if (domain == null) {
				continue;
			}
			List<ContactMapDomain> merges = ContactMapService.merge(domain.getMaps(), domain.getContent());
			domain.setMaps(merges);
			ContactDomain.anonymous(domain);
			expand(domain);
			domains.add(domain);
		}
		ContactParam result = differ(domains);

		log.info("{} {} - uploadJson(『{}』) - {}", Utility.indentEnd(), result, Utility.toStringJson(text, 32, 32),
				Utility.toStringPastTimeReadable(started));
		return result;
	}

	private void log(ContactDomain domain, String prefix) {
		List<Object> list = domain.getVcard();

		if (list == null) {
			return;
		}

		for (int cy = 1, sizey = list.size(); cy < sizey; cy++) {
			try {
				@SuppressWarnings("unchecked")
				List<Object> fieldsss = (List<Object>) list.get(cy);
				for (int cz = 0, sizez = fieldsss.size(); cz < sizez; cz++) {
					@SuppressWarnings("unchecked")
					List<Object> fields = (List<Object>) fieldsss.get(cz);
					log.info("{} 『{}』 fn:『{}』 maps:『#{}』 『{}』", Utility.indentMiddle(), prefix, domain.getFn(),
							Utility.size(domain.getMaps()), Utility.escape(Utility.toStringJson(fields, 64, 64)));
				}
			} catch (Exception e) {
				log.warn("{} 『{}』 fn:『{}』 maps:『#{}』 『{}』", Utility.indentMiddle(), prefix, domain.getFn(),
						Utility.size(domain.getMaps()), "EXCEPTION", e);
			}
		}

		List<ContactMapDomain> maps = domain.getMaps();
		for (int cx = 0, sizex = maps.size(); cx < sizex; cx++) {
			ContactMapDomain map = maps.get(cx);
			log.info("{} 『{}』 fn:『{}』 maps:『{}/{}』 『{}』", Utility.indentMiddle(), prefix, domain.getFn(), cx, sizex,
					map);
		}
	}

	public void log(List<ContactDomain> domains) {
		for (int cx = 0, sizex = domains.size(); cx < sizex; cx++) {
			ContactDomain domain = domains.get(cx);
			List<Object> list = domain.getVcard();

			log.info("{} upload(...) - 『{}/{}』『{}:{}』", Utility.indentMiddle(), cx, sizex, list.get(0),
					Utility.ellipsisEscape(Utility.toStringJson(domain), 64, 64));
			for (int cy = 1, sizey = list.size(); cy < sizey; cy++) {
				try {
					Object objectss = list.get(cy);
					@SuppressWarnings("unchecked")
					List<Object> fieldsss = (List<Object>) objectss;
					log.info("{} upload(...) - \t 『{}/{}:{}/{}』『{}:{}』", Utility.indentMiddle(), cx, sizex, cy, sizey,
							fieldsss.get(0), Utility.escape(Utility.toStringJson(objectss, 64, 64)));

					for (int cz = 0, sizez = fieldsss.size(); cz < sizez; cz++) {
						Object objects = fieldsss.get(cz);
						@SuppressWarnings("unchecked")
						List<Object> fields = (List<Object>) objects;
						log.info("{} upload(...) - \t 『{}/{}:{}/{}:{}/{}』『{}:{}』", Utility.indentMiddle(), cx, sizex,
								cy, sizey, cz, sizez, fields.get(0),
								Utility.escape(Utility.toStringJson(objects, 64, 64)));
						for (int cw = 0, sizew = fields.size(); cw < sizew; cw++) {
							Object field = fields.get(cw);
							log.info("{} upload(...) - \t 『{}/{}:{}/{}:{}/{}:{}/{}』『{}』", Utility.indentMiddle(), cx,
									sizex, cy, sizey, cz, sizez, cw, sizew,
									Utility.escape(Utility.toStringJson(field, 64, 64)));
						}
					}
				} catch (Exception e) {
					log.warn("{} upload(...) - \t 『{}/{}:{}』", Utility.indentMiddle(), cy, sizey, "EXCEPTION", e);
				}
			}
		}
	}

	public ContactParam uploadVcard(String text) {
		log.info("{} uploadVcard(『{}』)", Utility.indentStart(), Utility.toStringJson(text, 32, 32));
		long started = System.currentTimeMillis();

		List<VCard> vcards = Ezvcard.parse(text).all();
		List<ContactDomain> domains = new ArrayList<ContactDomain>();
		for (VCard vcard : vcards) {
			ContactDomain domain = ContactDomain.of(vcard);
			if (domain == null) {
				continue;
			}

			expand(domain);
			domains.add(domain);
		}

		ContactParam result = differ(domains);

		log.info("{} {} - uploadVcard(『{}』) - {}", Utility.indentEnd(), result, Utility.toStringJson(text, 32, 32),
				Utility.toStringPastTimeReadable(started));
		return result;
	}

	private ContactParam differ(List<ContactDomain> afters) {
		log.info("{} differ(#{})", Utility.indentStart(), Utility.size(afters));
		long started = System.currentTimeMillis();

		List<ContactDomain> befores = search(null);
		ContactParam result = differ(befores, afters);

		log.info("{} {} - differ(#{}) - {}", Utility.indentEnd(), result, Utility.size(afters),
				Utility.toStringPastTimeReadable(started));
		return result;
	}

	public static ContactParam differ(List<ContactDomain> beforeDomains, List<ContactDomain> afterDomains) {
		log.debug("{} differ(#{}, #{})", Utility.indentStart(), Utility.size(beforeDomains),
				Utility.size(afterDomains));
		long started = System.currentTimeMillis();

		Map<String, ContactDomain> mapBefore = makeMap(beforeDomains);
		Map<String, ContactDomain> mapAfter = makeMap(afterDomains);
		ContactParam result = ContactParam.builder().creates(new ArrayList<ContactDomain>())
				.duplicates(new ArrayList<ContactDomain>()).updates(new ArrayList<ContactDomain>())
				.removes(new ArrayList<ContactDomain>()).build();
		for (String key : mapBefore.keySet()) {
			ContactDomain before = mapBefore.get(key);
			ContactDomain after = mapAfter.get(key);
			if (after == null) {
				result.getRemoves().add(before);
				log.debug("{} differ(#{}, #{}) - {}", Utility.indentMiddle(), Utility.size(beforeDomains),
						Utility.size(afterDomains), key);
//				log(before, "remove");
				continue;
			}

			if (before.isNeedToUpdate(after)) {
				Utility.copyPropertiesNotNull(after, before, "maps", "id", "created", "updated");
				ContactDomain merged = merge(before, after);
				result.getUpdates().add(merged);
				log.debug("{} differ(#{}, #{}) - 『{}』『{}』『{}』『{}』", Utility.indentMiddle(),
						Utility.size(beforeDomains), Utility.size(afterDomains), key, merged, before, after);
				continue;
			}

			CrudList<ContactMapDomain> crudMap = ContactMapService.differ(before.getMaps(), after.getMaps());
			if (!crudMap.getUpdates().isEmpty() || !crudMap.getCreates().isEmpty()) {
				ContactDomain merged = merge(before, after);
				result.getUpdates().add(merged);
				log.debug("{} differ(#{}, #{}) - 『{}』『{}』『{}』『{}』", Utility.indentMiddle(), Utility.size(beforeDomains),
						Utility.size(afterDomains), key, merged, before, after);
//				log(before, "update");
				continue;
			}

			if (isSame(before, after)) {
				if (ContactMapService.isValidMaps(before)) {
					result.getDuplicates().add(before);
				} else {
					ContactDomain merged = merge(before, after);
					result.getUpdates().add(merged);
					log.debug("{} differ(#{}, #{}) - 『{}』『{}』『{}』『{}』", Utility.indentMiddle(),
							Utility.size(beforeDomains), Utility.size(afterDomains), key, merged, before, after);
//					log(before, "isSame update");
				}

				continue;
			}

			if (isSuperset(before, after)) {
				if (ContactMapService.isValidMaps(before)) {
					result.getDuplicates().add(before);
				} else {
					ContactDomain merged = merge(before, after);
					result.getUpdates().add(merged);
					log.debug("{} differ(#{}, #{}) - 『{}』『{}』『{}』『{}』", Utility.indentMiddle(),
							Utility.size(beforeDomains), Utility.size(afterDomains), key, merged, before, after);
				}

				continue;
			}

			ContactDomain merged = merge(before, after);
			result.getUpdates().add(merged);
			log.debug("{} differ(#{}, #{}) - 『{}』『{}』『{}』『{}』", Utility.indentMiddle(), Utility.size(beforeDomains),
					Utility.size(afterDomains), key, merged, before, after);
		}
		for (String key : mapAfter.keySet()) {
			ContactDomain after = mapAfter.get(key);
			ContactDomain before = mapBefore.get(key);
			if (before == null) {
				result.getCreates().add(after);
				log.debug("{} differ(#{}, #{}) - 『{}』『{}』", Utility.indentMiddle(), Utility.size(beforeDomains),
						Utility.size(afterDomains), key, after);
//				log(after, "create");
				continue;
			}
		}

		log.debug("{} {} - differ(#{}, #{}) - {}", Utility.indentEnd(), result, Utility.size(beforeDomains),
				Utility.size(afterDomains), Utility.toStringPastTimeReadable(started));
		return result;
	}

	// field key is const key = `${clone.id}.${x[0]}:${JSON.stringify(x)}`;
	private static ContactDomain merge(ContactDomain beforeDomain, ContactDomain afterDomain) {
		ContactDomain mergedDomain = ContactDomain.of(beforeDomain);
		// id: must equal
		// fn
		if (afterDomain.getFn() != null) {
			mergedDomain.setFn(afterDomain.getFn());
		}
		// value
		if (afterDomain.getValue() != null) {
			mergedDomain.setValue(afterDomain.getValue());
		}
		// content: deprecated
		// create:
		// updated:
		// maps from maps
		List<ContactMapDomain> maps = new ArrayList<>();
		mergedDomain.setMaps(maps);

		List<ContactMapDomain> merges = ContactMapService.merge(beforeDomain.getMaps(), afterDomain.getMaps());

		// maps from vcard
		merges = ContactMapService.mergeByVcard(merges, beforeDomain.getVcard());
		merges = ContactMapService.mergeByVcard(merges, afterDomain.getVcard());

		// maps from content
		merges = ContactMapService.merge(merges, beforeDomain.getContent());
		merges = ContactMapService.merge(merges, afterDomain.getContent());

		for (ContactMapDomain map : merges) {
			map.setVcardId(beforeDomain.getId());
		}

		mergedDomain.setMaps(merges);
		return mergedDomain;
	}

	private List<Object> mergeField(List<List<Object>> beforeFields, List<List<Object>> afterFields) {
		Map<String, Object> map = new HashMap<>();
		for (List<Object> field : afterFields) {
			String key = keyField(null, field);
			map.put(key, field);
		}
		for (List<Object> field : beforeFields) {
			String key = keyField(null, field);
			map.put(key, field);
		}

		List<Object> fields = new ArrayList<Object>(map.values());
		return fields;
	}

	private static String keyField(String fn, List<Object> field) {
		if (fn == null) {
			String key = String.format("%s.%s:%s", fn, field.get(0), Utility.toStringJson(field));
			return key;
		}

		String key = String.format("%s.%s:%s", fn, field.get(0), Utility.toStringJson(field));
		return key;
	}

	public static Map<String, List<Object>> makeMapByField(List<List<Object>> fields, String fn) {
		Map<String, List<Object>> map = new HashMap<String, List<Object>>();
		if (fields == null) {
			return map;
		}

		for (List<Object> field : fields) {
			String key = keyField(fn, field);
			map.put(key, field);
		}

		return map;
	}

	private static boolean isSuperset(ContactDomain beforeDomain, ContactDomain afterDomain) {
		Map<String, ContactMapDomain> beforeMapByMaps = ContactMapService.makeMap(beforeDomain);
		Map<String, ContactMapDomain> afterMapByMaps = ContactMapService.makeMap(afterDomain);
		for (String key : afterMapByMaps.keySet()) {
			ContactMapDomain before = beforeMapByMaps.get(key);
			if (before == null) {
				log.info("{} isSuperset(..., ...) - 『{}』", Utility.indentMiddle(), key);
				return false;
			}

			ContactMapDomain after = afterMapByMaps.get(key);
			Integer bv = before.getValue();
			Integer av = after.getValue();
			if (av == null) {
				continue;
			}

			if (Utility.compare(bv, av) != 0) {
				return false;
			}
		}

		return true;
	}

	private static boolean isSame(ContactDomain before, ContactDomain after) {
		return before.toString().compareToIgnoreCase(after.toString()) == 0;
	}

	private static Map<String, ContactDomain> makeMap(List<ContactDomain> list) {
		Map<String, ContactDomain> map = new HashMap<String, ContactDomain>();
		if (list == null) {
			return map;
		}

		for (ContactDomain domain : list) {
			String key = key(domain);
			map.put(key, domain);
		}

		return map;
	}

	@SuppressWarnings("unchecked")
	private static String key(ContactDomain domain) {
		if (domain.getFn() != null) {
			return domain.getFn();
		}

		List<Object> vcard = domain.getVcard();
		if (vcard == null) {
			return "N/A";
		}

		List<Object> listObject = (List<Object>) vcard.get(1);

		if (Utility.size(listObject) > 0) {
			String key = fieldValue(listObject, "fn", "N/A");
			return key;
		}

		for (Object object : listObject) {
			if (!(object instanceof ArrayList)) {
				continue;
			}

			List<Object> list = (List<Object>) object;
			if (list.size() < 4) {
				continue;
			}

			Object first = list.get(0);
			if (!(first instanceof String)) {
				continue;
			}

			String name = (String) first;
			if (name.compareToIgnoreCase("fn") != 0) {
				continue;
			}

			Object fourth = list.get(3);
			if (!(fourth instanceof String)) {
				continue;
			}

			String fname = (String) fourth;
			String key = String.format("%s", fname);
			return key;
		}

		return Double.toString(Math.random());
	}

	@SuppressWarnings("unchecked")
	private static String fieldValue(List<Object> listObject, String field, String defaultValue) {
		for (Object object : listObject) {
			if (!(object instanceof ArrayList)) {
				continue;
			}

			List<Object> list = (List<Object>) object;
			if (list.size() < 4) {
				continue;
			}

			Object first = list.get(0);
			if (!(first instanceof String)) {
				continue;
			}

			String name = (String) first;
			if (name.compareToIgnoreCase(field) != 0) {
				continue;
			}

			Object fourth = list.get(3);
//			if (!(fourth instanceof String)) {
//				continue;
//			}

			String fname = (String) fourth.toString();
			return fname;
		}

		return defaultValue;
	}

	public String download() {
		StringBuffer stringBuffer = new StringBuffer("");
		List<ContactDomain> contacts = search(null);
		if (contacts == null) {
			return stringBuffer.toString();
		}

		for (ContactDomain contact : contacts) {
			stringBuffer.append(ContactDownload.of(contact).toString());
			stringBuffer.append("\n");
		}

		return stringBuffer.toString();
	}

	public String downloadVCard(Integer priority) {
		if (priority == null) {
			priority = Integer.MAX_VALUE;
		}

		StringBuffer stringBuffer = new StringBuffer("");
		List<ContactDomain> domains = search(null);
		if (domains == null) {
			// 데이터 없음
			return stringBuffer.toString();
		}

		Iterator<ContactDomain> iterator = domains.iterator();
		while (iterator.hasNext()) {
			ContactDomain domain = iterator.next();
			if (Utility.compare(domain.getValue(), priority) > 0) {
				continue;
			}

			stringBuffer.append(domain.toStringVcard(priority));
		}

		return stringBuffer.toString();
	}

	private String downloadVCard(int priority, ContactDomain domain) {
		List<ContactMapDomain> maps = domain.getMaps();
		Map<String, ContactMapDomain> map = contactMapService.makeMap(maps);
		if (priority(map, domain.getId().toString(), 0) > priority) {
			return "";
		}

		List<Object> vcardFromJson = domain.getVcard();
		@SuppressWarnings("unchecked")
		List<List<Object>> fields = (List<List<Object>>) vcardFromJson.get(1);
		Iterator<List<Object>> iterator = fields.iterator();
		while (iterator.hasNext()) {
			List<Object> field = iterator.next();
			if (priority(map, domain, field, 0) > priority) {
				iterator.remove();
			}
		}
		String json = Utility.toStringJson(vcardFromJson);
		VCard vcard = Ezvcard.parseJson(json).first();
		String result = Ezvcard.write(vcard).go();
		return result;
	}

	private int priority(Map<String, ContactMapDomain> map, ContactDomain domain, List<Object> field,
			int defaultValue) {
		String key = String.format("%d.%s:%s", domain.getId(), field.get(0), Utility.toStringJson(field));
		return priority(map, key, defaultValue);
	}

	private int priority(Map<String, ContactMapDomain> map, String key, int defaultValue) {
		ContactMapEntity entity = map.get(key);
		if (entity == null) {
			return defaultValue;
		}

		Integer value = entity.getValue();
		if (value == null) {
			return defaultValue;
		}

		return value.intValue();
	}

	public int put(CrudList<ContactDomain> crud) {
		if (crud == null || crud.isEmpty()) {
			return 0;
		}

		log.info("{} put(『{}』)", Utility.indentStart(), crud);
		long started = System.currentTimeMillis();

		List<ContactDomain> created = create(crud.getCreates());
		List<ContactDomain> updated = update(crud.getUpdates());
		int removed = remove(crud.getRemoves());
		int result = removed + Utility.size(created) + Utility.size(updated);

		log.info("{} 『#{}』 put(『{}』) - {}", Utility.indentEnd(), result, crud,
				Utility.toStringPastTimeReadable(started));
		return result;
	}

	@Modifying
	private int remove(List<ContactDomain> domains) {
		domains.clear();
		if (domains == null || domains.isEmpty()) {
			return 0;
		}

		log.info("{} remove(#{})", Utility.indentStart(), Utility.size(domains));
		long started = System.currentTimeMillis();

		List<ContactEntity> entities = new ArrayList<>();
		for (ContactDomain domain : domains) {
			entities.add(domain.toEntity());
		}
		repository.deleteAll(entities);
		repository.flush();

		log.info("{} 『#{}』 remove(#{}) - {}", Utility.indentEnd(), Utility.size(domains), Utility.size(entities),
				Utility.toStringPastTimeReadable(started));
		return Utility.size(entities);
	}

}
