package kr.andold.terran.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.io.chain.ChainingTextStringParser;
import kr.andold.terran.domain.ContactDomain;
import kr.andold.terran.domain.ContactMapDomain;
import kr.andold.terran.entity.ContactMapEntity;
import kr.andold.terran.param.ContactMapParam;
import kr.andold.terran.repository.ContactMapRepository;
import kr.andold.utils.Utility;
import kr.andold.utils.persist.CrudList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ContactMapService {
	@Autowired
	private ContactMapRepository repository;

	public static CrudList<ContactMapDomain> differ(List<ContactMapDomain> befores, List<ContactMapDomain> afters) {
		Map<String, ContactMapDomain> mapBefore = makeMap(befores);
		Map<String, ContactMapDomain> mapAfter = makeMap(afters);
		
		List<ContactMapDomain> creates = new ArrayList<>();
		List<ContactMapDomain> duplicates = new ArrayList<>();
		List<ContactMapDomain> updated = new ArrayList<>();
		List<ContactMapDomain> removes = new ArrayList<>();
		CrudList<ContactMapDomain> result = CrudList.<ContactMapDomain>builder()
				.creates(creates)
				.duplicates(duplicates)
				.updates(updated)
				.removes(removes)
				.build();
		Date date = new Date();

		for (String key : mapAfter.keySet()) {
			ContactMapDomain after = mapAfter.get(key);
			ContactMapDomain before = mapBefore.get(key);
			if (before == null) {
				//	create
				if (after.getCreated() == null) {
					after.setCreated(date);
				}
				if (after.getUpdated() == null) {
					after.setUpdated(date);
				}
				creates.add(after);
				continue;
			}
			
			if (after.getValue() == null) {
				//	inherit before
				duplicates.add(after);
				continue;
			}
			if (before.getValue().equals(after.getValue())) {
				//	equals
				duplicates.add(after);
				continue;
			}
			
			after.setId(before.getId());
			after.setVcardId(before.getVcardId());
			updated.add(after);
		}
		for (String key : mapBefore.keySet()) {
			ContactMapDomain after = mapAfter.get(key);
			ContactMapDomain before = mapBefore.get(key);
			if (after == null) {
				//	remove
				removes.add(before);
				continue;
			}
		}

		return result;
	}

	public static Map<String, ContactMapDomain> makeMap(List<ContactMapDomain> domains) {
		Map<String, ContactMapDomain> map = new HashMap<String, ContactMapDomain>();
		if (domains == null) {
			return map;
		}

		Iterator<ContactMapDomain> iterator = domains.iterator();
		while(iterator.hasNext()){
			ContactMapDomain domain = iterator.next();
			if (domain == null) {
				iterator.remove();
				continue;
			}

			ContactMapDomain.correct(domain);
			
			String key = domain.getKey();
			if (key == null || key.isBlank()) {
				iterator.remove();
				continue;
			}

			map.put(key, domain);
		}

		return map;
	}

	public int batch(CrudList<ContactMapDomain> list) {
		int count = 0;

		List<ContactMapDomain> creates = list.getCreates();
		if (creates != null && !creates.isEmpty()) {
			List<ContactMapDomain> created = create(list.getCreates());
			count += Utility.size(created);
		}

		List<ContactMapDomain> remvoes = list.getRemoves();
		if (remvoes != null && !remvoes.isEmpty()) {
			count += remove(list.getRemoves());
		}

		List<ContactMapDomain> updates = list.getUpdates();
		if (updates != null && !updates.isEmpty()) {
			count += Utility.size(update(list.getUpdates()));
		}

		return count;
	}

	@Modifying
	public ContactMapDomain update(Integer id, ContactMapDomain param) {
		Optional<ContactMapEntity> before = repository.findById(id);
		if (before.isEmpty()) {
			return null;
		}

		ContactMapEntity entity = before.get();
		Utility.copyPropertiesNotNull(param, entity, "id");
		ContactMapEntity updated = repository.saveAndFlush(entity);
		return ContactMapDomain.of(updated);
	}

	@Modifying
	public List<ContactMapDomain> update(List<ContactMapDomain> domains) {
		List<ContactMapEntity> entities = new ArrayList<>();
		for (ContactMapDomain domain: domains) {
			entities.add(domain.toEntity());
		}

		List<ContactMapEntity> updated = repository.saveAllAndFlush(entities);
		List<ContactMapDomain> result = new ArrayList<>();
		for (ContactMapEntity entity : updated) {
			result.add(ContactMapDomain.of(entity));
		}

		return result;
	}

	@Modifying
	public int remove(List<ContactMapDomain> domains) {
		domains.clear();
		if (domains == null || domains.isEmpty()) {
			return 0;
		}

		log.info("{} remove(#{})", Utility.indentStart(), Utility.size(domains));
		long started = System.currentTimeMillis();

		List<ContactMapEntity> entities = new ArrayList<>();
		for (ContactMapDomain domain: domains) {
			entities.add(domain.toEntity());
		}
		repository.deleteAll(entities);
		repository.flush();

		log.info("{} 『#{}』 remove(#{}) - {}", Utility.indentEnd(), Utility.size(entities), Utility.size(entities), Utility.toStringPastTimeReadable(started));
		return Utility.size(entities);
	}

	@Modifying
	public List<ContactMapDomain> create(List<ContactMapDomain> domains) {
		if (domains == null || domains.isEmpty()) {
			return null;
		}

		log.info("{} create(#{})", Utility.indentStart(), Utility.size(domains));
		long started = System.currentTimeMillis();

		prepareCreate(domains);
		List<ContactMapEntity> entities = new ArrayList<>();
		for (ContactMapDomain domain: domains) {
			entities.add(domain.toEntity());
		}
		try {
			List<ContactMapEntity> created = repository.saveAllAndFlush(entities);
			List<ContactMapDomain> result = new ArrayList<>();
			for (ContactMapEntity entity : created) {
				result.add(ContactMapDomain.of(entity));
			}

			log.info("{} 『#{}』 create(#{}) - {}", Utility.indentEnd(), Utility.size(result), Utility.size(domains), Utility.toStringPastTimeReadable(started));
			return result;
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		List<ContactMapDomain> result = new ArrayList<>();
		for (ContactMapEntity entity : domains) {
			ContactMapEntity created = create(entity);
			result.add(ContactMapDomain.of(created));
		}
		log.info("{} 『#{}』 create(#{}) - {}", Utility.indentEnd(), null, Utility.size(domains), Utility.toStringPastTimeReadable(started));
		return null;
	}

	@Modifying
	public ContactMapEntity create(ContactMapEntity entity) {
		ContactMapEntity created = repository.saveAndFlush(entity);
		return created;
	}

	private List<ContactMapDomain> prepareCreate(List<ContactMapDomain> domains) {
		Date date = new Date();
		Iterator<ContactMapDomain> iterator = domains.iterator();
		while(iterator.hasNext()){
			ContactMapDomain domain = iterator.next();
			if (domain == null) {
				iterator.remove();
				continue;
			}

			String key = domain.getKey();
			if (key == null || key.isBlank()) {
				iterator.remove();
				continue;
			}

			domain.setId(null);
			if (domain.getValue() == null) {
				domain.setValue(0);
			}
			domain.setCreated(date);
			domain.setUpdated(date);
		}
		
		return domains;
	}

	public ContactDomain expand(ContactDomain domain) {
		List<ContactMapDomain> fromDatabase;
		if (domain.getId() == null) {
			fromDatabase = new ArrayList<>();
		} else {
			fromDatabase = search(ContactMapParam.builder().vcardId(domain.getId()).build());
		}

		List<ContactMapDomain> merged = merge(fromDatabase, domain.getMaps());
		merged = merge(merged, domain.getContent());
		domain.setMaps(merged);
		
		return domain;
	}

	public static Map<String, ContactMapDomain> makeMap(ContactDomain domain) {
		Map<String, ContactMapDomain> map = new HashMap<>();
		if (domain == null) {
			return map;
		}
		
		List<ContactMapDomain> maps = domain.getMaps();
		if (maps == null) {
			return map;
		}
		
		String fn = domain.getFn();
		if (fn == null) {
			fn = "N/A";
		}

		for (ContactMapDomain contactMap: maps) {
			map.put(String.format("%s.%s", fn, contactMap.getKey()), contactMap);
		}

		return map;
	}

	public Map<String, List<Object>> makeMapByField(ContactDomain domain) {
		Map<String, List<Object>> map = new HashMap<>();
		if (domain == null) {
			return map;
		}
		
		@SuppressWarnings("unchecked") List<List<Object>> fields = (List<List<Object>>)domain.getVcard().get(1);
		if (fields == null) {
			return map;
		}
		
		String fn = domain.getFn();
		if (fn == null) {
			fn = "N/A";
		}

		for (List<Object> field : fields) {
			map.put(String.format("%s.%s", fn, Utility.toStringJson(field)), field);
		}

		return map;
	}

	private List<ContactMapDomain> search(ContactMapParam param) {
		List<ContactMapEntity> entities = repository.search(param);
		List<ContactMapDomain> domains = new ArrayList<ContactMapDomain>();
		for (ContactMapEntity entity : entities) {
			ContactMapDomain domain = ContactMapDomain.of(entity);
			domains.add(domain);
		}

		return domains;
	}

	public static boolean isValidMaps(ContactDomain domain) {
		Map<String, ContactMapDomain> map = makeMap(domain.getMaps());
		List<Object> vcard = domain.getVcard();
		if (vcard == null || vcard.size() < 2) {
			return true;
		}

		@SuppressWarnings("unchecked") List<List<Object>> fields = (List<List<Object>>)vcard.get(1);
		Map<String, List<Object>> mapField = new HashMap<>();
		for (List<Object> field : fields) {
			mapField.put(Utility.toStringJson(field), field);
		}
		
		for (String key: mapField.keySet()) {
			ContactMapDomain item = map.get(key);
			if (item == null) {
				return false;
			}
		}

		return true;
	}

	public CrudList<ContactMapDomain> differ(ContactDomain contact) {
		List<ContactMapDomain> befores;
		if (contact.getId() == null) {
			befores = new ArrayList<>();
		} else {
			befores = search(ContactMapParam.builder().vcardId(contact.getId()).build());
		}

		List<ContactMapDomain> afters = mergeByVcard(contact.getMaps(), contact.getVcard());
		afters = merge(afters, contact.getContent());
		return differ(befores, afters);
	}

	public int put(CrudList<ContactMapDomain> crud) {
		List<ContactMapDomain> created = create(crud.getCreates());
		List<ContactMapDomain> updated = update(crud.getUpdates());
		int removed = remove(crud.getRemoves());
		
		return removed + Utility.size(created) + Utility.size(updated);
	}

	public static List<ContactMapDomain> convertContent2Maps(String content) {
		List<ContactMapDomain> list = new ArrayList<>();
		if (content == null) {
			return list;
		}

		ChainingTextStringParser parsed = Ezvcard.parse(content);
		if (parsed == null) {
			return list;
		}

		VCard vcard = parsed.first();
		if (vcard == null) {
			return list;
		}

		String json = Ezvcard.writeJson(vcard).go();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			@SuppressWarnings("unchecked") List<Object> listObject = objectMapper.readValue(json, List.class);
			if (Utility.size(listObject) < 2) {
				return list;
			}

			for (int cx = 1, sizex = listObject.size(); cx < sizex; cx++) {
				try {
					@SuppressWarnings("unchecked") List<Object> fields = (List<Object>) listObject.get(cx);
					for (int cy = 0, sizey = fields.size(); cy < sizey; cy++) {
						Object field = fields.get(cy);
						ContactMapDomain map = ContactMapDomain.builder()
								.key(Utility.toStringJson(field))
								.build();
						ContactMapDomain.correct(map);
						list.add(map);
					}
				} catch (Exception e) {
					log.warn("Exception:: {}", e.getLocalizedMessage(), e);
				}
			}

			return list;
		} catch (JsonProcessingException e) {
			log.warn("{} convertContent2Maps({}) - JsonProcessingException:: {}", Utility.ellipsisEscape(content, 32), e.getLocalizedMessage());
		}

		return list;
	}

	private static ContactMapDomain merge(ContactMapDomain before, ContactMapDomain after) {
		ContactMapDomain merge = ContactMapDomain.of(before);
		//	id: must be equal or null
		//	vcardId
		if (after.getVcardId() != null) {
			merge.setVcardId(after.getVcardId());
		}
		//	key: must be equal or null
		//	value
		if (after.getValue() != null) {
			merge.setValue(after.getValue());
		}
		//	created:
		//	updated:

		return merge;
	}

	public static List<ContactMapDomain> merge(List<ContactMapDomain> befores, List<ContactMapDomain> afters) {
		Map<String, ContactMapDomain> mapByKey = makeMap(befores);
		if (afters == null) {
			return new ArrayList<ContactMapDomain>(mapByKey.values());
		}

		for (ContactMapDomain after: afters) {
			ContactMapDomain before = mapByKey.get(after.getKey());
			if (before == null) {
				mapByKey.put(after.getKey(), after);
				continue;
			}
			
			ContactMapDomain merge = merge(before, after);
			mapByKey.put(after.getKey(), merge);
		}

		return new ArrayList<ContactMapDomain>(mapByKey.values());
	}

	public static List<ContactMapDomain> merge(List<ContactMapDomain> befores, String content) {
		List<ContactMapDomain> afters = convertContent2Maps(content);
		return merge(befores, afters);
	}

	public static List<ContactMapDomain> mergeByVcard(List<ContactMapDomain> befores, List<Object> vcard) {
		List<ContactMapDomain> afters = convertVcard2Maps(vcard);
		return merge(befores, afters);
	}

	private static List<ContactMapDomain> convertVcard2Maps(List<Object> listObject) {
		List<ContactMapDomain> maps = new ArrayList<>();
		if (listObject == null || listObject.size() < 2) {
			return maps;
		}

		@SuppressWarnings("unchecked") List<List<Object>> fields = (List<List<Object>>)listObject.get(1);
		for (Object field: fields) {
			String key = Utility.toStringJson(field);
			ContactMapDomain map = ContactMapDomain.builder()
					.key(key)
					.build();
			ContactMapDomain.correct(map);
			maps.add(map);
		}
		return maps;
	}

	public void sort(List<ContactMapDomain> domains) {
		domains.sort(new Comparator<ContactMapDomain>() {
			@Override
			public int compare(ContactMapDomain o1, ContactMapDomain o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});
	}

}
