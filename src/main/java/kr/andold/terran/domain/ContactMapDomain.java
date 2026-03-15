package kr.andold.terran.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import kr.andold.terran.entity.ContactMapEntity;
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
public class ContactMapDomain extends ContactMapEntity {
	private ContactDomain parent;
	
	public ContactMapDomain(ContactMapEntity entity) {
		BeanUtils.copyProperties(entity, this);
	}

	public static ContactMapDomain of(ContactMapEntity entity) {
		ContactMapDomain domain = new ContactMapDomain(entity);

		return domain;
	}

	public static ContactMapDomain of(List<Object> field) {
		return ContactMapDomain.builder().key(Utility.toStringJson(field)).build();
	}

	public ContactMapEntity toEntity() {
		ContactMapEntity entity = new ContactMapEntity();
		BeanUtils.copyProperties(this, entity);
		return entity;
	}

	@Override
	public String toString() {
		return Utility.toStringJson(this);
	}

	public static ContactMapDomain correct(ContactMapDomain domain) {
		if (domain == null) {
			return domain;
		}
		
		String key = domain.getKey();
		if (key == null) {
			return domain;
		}
		
		// remove - 1041.org:["org",{"group":"item2"},"text","현대자동차"]",...
		domain.setKey(key.replaceFirst("^[0-9]+(\\.[a-z][a-z0-9\\-]*:)?", ""));
		return domain;
	}

	public static List<ContactMapDomain> anonymous(List<ContactMapDomain> maps) {
		for (ContactMapDomain map: maps) {
			map.setId(null);
			map.setVcardId(null);
		}
		return maps;
	}

	public static Integer safeValue(List<ContactMapDomain> maps) {
		Iterator<ContactMapDomain> iterator = maps.iterator();
		while(iterator.hasNext()){
			ContactMapDomain domain = iterator.next();
			if (domain == null) {
				iterator.remove();
				continue;
			}

			String key = domain.getKey();
			if (key == null) {
				iterator.remove();
				continue;
			}

			if (key.isBlank()) {
				return domain.getValue();
			}
		}

		return null;
	}

	public String toStringVcard(Integer priority) {
		if (Utility.compare(getValue(), priority) > 0) {
			return "";
		}

		try {
			String key = getKey();
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setSerializationInclusion(Include.NON_NULL);
			objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			@SuppressWarnings("unchecked") List<Object> list = objectMapper.readValue(key, List.class);
			String fname = toStringVcard(list.get(0));
			String attr = toStringVcard(list.get(1));
			String vtype = toStringVcard(list.get(2));
			StringBuffer stringBuffer = new StringBuffer("");
			for (int cx = 3, sizex = list.size(); cx < sizex; cx++) {
				Object item = list.get(cx);
				stringBuffer.append(String.format("%s,", toStringVcard(item)));
			}
			String value = stringBuffer.toString().replaceAll(",$", "");
			if (vtype.equalsIgnoreCase("text")) {
				value = Utility.toStringJson(value).replaceAll("(^\\\")|(\\\"$)", "");
			} else {
				log.debug("value is not text: 『{}』『{}』", vtype, value);
			}
			if (attr.isBlank()) {
				return String.format("%s:%s", fname, value);
			}

			return String.format("%s;%s:%s", fname, attr, value);
		} catch (Exception e) {
		}
		return "";
	}

	@SuppressWarnings("unchecked")
	private String toStringVcard(Object object) {
		if (object instanceof String) {
			return (String)object;
		}

		if (object instanceof ArrayList) {
			List<Object> list = (List<Object>)object;
			StringBuffer stringBuffer = new StringBuffer("");
			for (Object item : list) {
				stringBuffer.append(String.format("%s;", toStringVcard(item)));
			}
			return stringBuffer.toString().replaceAll(";$", "");
		}

		if (object instanceof HashMap) {
			Map<String, Object> map = (Map<String, Object>)object;
			StringBuffer stringBuffer = new StringBuffer("");
			for (String key : map.keySet()) {
				stringBuffer.append(String.format("%s=%s;", key, toStringVcard(map.get(key))));
			}
			return stringBuffer.toString().replaceAll(";$", "");
		}

		log.warn("instanceof Object: 『{}』", object);
		return object.toString();
	}

}
