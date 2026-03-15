package kr.andold.terran.domain;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.io.chain.ChainingJsonStringParser;
import ezvcard.io.chain.ChainingTextStringParser;
import kr.andold.terran.entity.ContactEntity;
import kr.andold.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ContactDomain extends ContactEntity {
	private List<ContactMapDomain> maps;
	@Deprecated private List<Object> vcard;

	public static ContactEntity toEntity(ContactDomain param) {
		List<Object> vcardListObject = param.getVcard();
		String content = param.getContent();
		if (vcardListObject != null) {
			String json = Utility.toStringJson(vcardListObject);
			ChainingJsonStringParser parsed = Ezvcard.parseJson(json);
			if (parsed != null) {
				VCard vcard = parsed.first();
				if (vcard != null) {
					content = Ezvcard.write(vcard).go();
				}
			}
		}
		return ContactEntity.builder()
			.id(param.getId())
			.fn(param.getFn())
			.value(param.getValue())
			.content(content)
			.created(param.getCreated())
			.updated(param.getUpdated())
			.build();
	}

	public ContactEntity toEntity() {
		return toEntity(this);
	}

	public ContactDomain(ContactEntity entity) {
		BeanUtils.copyProperties(entity, this);
	}

	@Override
	public String toString() {
		return Utility.toStringJson(this);
	}

	public static ContactDomain of(ContactDomain before) {
		ContactDomain domain = new ContactDomain();
		BeanUtils.copyProperties(before, domain);
		return domain;
	}

	public static ContactDomain of(ContactEntity entity) {
		ContactDomain domain = new ContactDomain(entity);
		String content = entity.getContent();
		if (content == null) {
			return domain;
		}

		ChainingTextStringParser parsed = Ezvcard.parse(content);
		if (parsed == null) {
			return domain;
		}

		VCard vcard = parsed.first();
		if (vcard == null) {
			return domain;
		}

		String json = Ezvcard.writeJson(vcard).go();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			@SuppressWarnings("unchecked") List<Object> listObject = objectMapper.readValue(json, List.class);
			domain.setVcard(listObject);
			if (Utility.size(listObject) < 2) {
				return domain;
			}

			for (int cx = 1, sizex = listObject.size(); cx < sizex; cx++) {
				try {
					@SuppressWarnings("unchecked") List<Object> fields = (List<Object>) listObject.get(cx);
					for (int cy = 0, sizey = fields.size(); cy < sizey; cy++) {
						@SuppressWarnings("unchecked") List<Object> field = (List<Object>) fields.get(cy);
						String fname = (String)field.get(0);
						if (!fname.equalsIgnoreCase("fn")) {
							continue;
						}

						domain.setFn((String)field.get(field.size() - 1));
					}
				} catch (Exception e) {
					log.warn("Exception:: {}", e.getLocalizedMessage(), e);
				}
			}

			return domain;
		} catch (JsonProcessingException e) {
			log.warn("{} Contact2Domain({}) - JsonProcessingException:: {}", entity, e.getLocalizedMessage());
		}

		return domain;
	}

	public static ContactDomain of(String json) {
		ContactDomain domain = ofByContactDomain(json);
		ContactDownload download = ofByContactDownload(json);
		if (domain == null && download == null) {
			return null;
		}
		
		if (domain == null) {
			return download.to();
		}

		if (download == null) {
			return domain;
		}

		if (!domain.getMaps().isEmpty() && !download.getMaps().isEmpty() && domain.getMaps().get(0).getKey() == null && download.getMaps().get(0).getField() != null) {
			return download.to();
		}

		return domain;
	}

	private static ContactDownload ofByContactDownload(String json) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			ContactDownload download = objectMapper.readValue(json, ContactDownload.class);
			return download;
		} catch (Exception e) {
			try {
				ContactDownload downloadUtf8 = objectMapper.readValue(URLDecoder.decode(json, "UTF-8"), ContactDownload.class);
				return downloadUtf8;
			} catch (Exception f) {
				e.printStackTrace();
				f.printStackTrace();
			}
		}

		return null;
	}

	private static ContactDomain ofByContactDomain(String json) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			ContactDomain domain = objectMapper.readValue(json, ContactDomain.class);
			for (ContactMapDomain map : domain.getMaps()) {
				ContactMapDomain.correct(map);
			}
			domain.setValue(safeValue(domain));
			return domain;
		} catch (Exception e) {
			try {
				ContactDomain domainUtf8 = objectMapper.readValue(URLDecoder.decode(json, "UTF-8"), ContactDomain.class);
				for (ContactMapDomain map : domainUtf8.getMaps()) {
					ContactMapDomain.correct(map);
				}
				return domainUtf8;
			} catch (Exception f) {
				e.printStackTrace();
				f.printStackTrace();
			}
		}

		return null;
	}

	public static Integer safeValue(ContactDomain domain) {
		if (domain == null) {
			return null;
		}

		if (domain.getValue() != null) {
			return domain.getValue();
		}

		return ContactMapDomain.safeValue(domain.getMaps());
	}

	private static String safeFn(List<List<Object>> fields) {
		if (fields == null) {
			return "N/A";
		}

		for (List<Object> field: fields) {
			if (field.size() < 4) {
				continue;
			}

			Object first = field.get(0);
			if (!(first instanceof String)) {
				continue;
			}

			String name = (String)first;
			if (name.compareToIgnoreCase("fn") != 0) {
				continue;
			}

			Object fourth = field.get(3);
			String fname = (String)fourth.toString();
			return fname;
		}

		//	fn이 없다, 그럼 n으로
		for (List<Object> field: fields) {
			if (field.size() < 4) {
				continue;
			}

			Object first = field.get(0);
			if (!(first instanceof String)) {
				continue;
			}

			String name = (String)first;
			if (name.compareToIgnoreCase("n") != 0) {
				continue;
			}

			@SuppressWarnings("unchecked")
			List<Object> fourth = (List<Object>)field.get(3);
			String fname = "";
			for (Object token: fourth) {
				fname += token.toString();
			}
			return fname;
		}

		return "N/A";
	}

	public static ContactDomain of(VCard vcard) {
		ContactDomain domain = new ContactDomain();
		String json = Ezvcard.writeJson(vcard).go();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			@SuppressWarnings("unchecked") List<Object> listObject = objectMapper.readValue(json, List.class);
			if (listObject == null || listObject.size() < 2) {
				log.warn("{} INVALID of({}) - {}", Utility.indentMiddle(), Utility.ellipsisEscape(json, 32, 32));
				return domain;
			}

			List<ContactMapDomain> maps = new ArrayList<>();
			@SuppressWarnings("unchecked") List<List<Object>> fields = (List<List<Object>>)listObject.get(1);
			for (Object field: fields) {
				String key = Utility.toStringJson(field);
				ContactMapDomain map = ContactMapDomain.builder()
						.key(key)
						.build();
				maps.add(map);
			}
			String fn = safeFn(fields);
			return ContactDomain.builder()
					.fn(fn)
					.maps(maps)
					.build();
		} catch (JsonProcessingException e) {
			log.warn("{} of({}) - JsonProcessingException:: {}", Utility.indentMiddle(), vcard, e.getLocalizedMessage());
		}

		return domain;
	}

	public static ContactDomain anonymous(ContactDomain domain) {
		domain.setId(null);
		ContactMapDomain.anonymous(domain.getMaps());
		return domain;
	}

	public String toStringVcard(Integer priority) {
		if (Utility.compare(getValue(), priority) > 0) {
			return "";
		}

		StringBuffer stringBuffer = new StringBuffer("BEGIN:VCARD\n");
		for (ContactMapDomain map: getMaps()) {
			String line = map.toStringVcard(priority);
			if (line == null || line.isBlank() || line.matches("[\\s]+")) {
				// blank line skip
				continue;
			}

			stringBuffer.append(String.format("%s\n", map.toStringVcard(priority)));
		}

		stringBuffer.append("END:VCARD\n");
		return stringBuffer.toString();
	}

	public int compareTo(ContactDomain domain) {
		if (domain == null) {
			return 1;
		}

		int compared = Utility.compare(getFn(), domain.getFn());
		if (compared != 0) {
			return compared;
		}

		// vcf import시에는 value가 null이다
		if (getValue() == 0 && domain.getValue() == null) {
			compared = 0;
		} else {
			compared = Utility.compare(getValue(), domain.getValue());
		}

		if (compared != 0) {
			return compared;
		}

		return 0;
	}

	public boolean isNeedToUpdate(ContactDomain domain) {
		if (domain == null) {
			return false;
		}

		int compared = Utility.compare(getFn(), domain.getFn());
		if (compared != 0) {
			return true;
		}

		if (domain.getValue() == null) {
			return false;
		}

		compared = Utility.compare(getValue(), domain.getValue());

		if (compared != 0) {
			return true;
		}

		return false;
	}

}
