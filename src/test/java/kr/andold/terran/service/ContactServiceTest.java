package kr.andold.terran.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import kr.andold.terran.domain.ContactDomain;
import kr.andold.terran.param.ContactParam;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContactServiceTest {
	@BeforeEach
	public void beforeEach() {
		log.info("{} ContactServiceTest", Utility.indentStart());
	}

	@Test
	public void testCompareUpdate() throws Exception {
		List<ContactDomain> befores = new ArrayList<>();
		List<ContactDomain> afters = new ArrayList<>();
		befores.add(ContactDomain.builder()
				.fn("test")
				.value(2)
				.build());
		afters.add(ContactDomain.builder()
				.fn("test")
				.value(3)
				.build());
		ContactParam result = ContactService.differ(befores, afters);
		log.info("{}", result);
	}

	//@Test
	@SuppressWarnings({"rawtypes"})
	public void testToJsonToList() throws IllegalAccessException, InvocationTargetException, JsonMappingException, JsonProcessingException {
		String filename = "list-contact-20230313-short.vcf";
		//filename = "list-contact-20230313.vcf";
		String input = Utility.readClassPathFile(filename).replaceAll("[\r\n]+", "\n");
		List<VCard> vcards = Ezvcard.parse(input).all();
		String json = Ezvcard.writeJson(vcards).go();
		List result = new ObjectMapper().readValue(json, List.class);
		log.info("#{} {}", Utility.size(vcards), Utility.toStringJson(result));
	}

	//@Test
	@SuppressWarnings({"unused"})
	public void testByJson() throws IllegalAccessException, InvocationTargetException, JsonMappingException, JsonProcessingException {
		String filename = "list-contact-20230313-short.vcf";
		//filename = "list-contact-20230313.vcf";
		String input = Utility.readClassPathFile(filename).replaceAll("[\r\n]+", "\n");
		List<VCard> vcards = Ezvcard.parse(input).all();
		log.info("{} {}", Utility.indentMiddle(), vcards);
		//log.info("{} {}", Utility.indentMiddle(), vcards.toString());
		String json = Ezvcard.writeJson(vcards).go();
		//log.info("{} {}", Utility.indentMiddle(), json);
		Object result = testByJson(json);
		//log.info("{} {}", Utility.indentMiddle(), Utility.toStringJson(result));
	}

	@SuppressWarnings("rawtypes")
	private Object testByJson(String json) {
		try {
			Map result = new ObjectMapper().readValue(json, Map.class);
			return result;
		} catch (JsonProcessingException e) {
			try {
				List result = new ObjectMapper().readValue(json, List.class);
				return result;
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	@Test
	public void testJson() throws IllegalAccessException, InvocationTargetException, JsonMappingException, JsonProcessingException {
		String filename = "list-contact-20230313-short.vcf";
		//filename = "list-contact-20230313.vcf";
		String input = Utility.readClassPathFile(filename).replaceAll("[\r\n]+", "\n");
		VCard vcard = Ezvcard.parse(input).first();
		String jsonVCard = Ezvcard.writeJson(vcard).go();

		VCard vcardCandidate = Ezvcard.parseJson(jsonVCard).first();
		String jsonVCardCandidate = Ezvcard.writeJson(vcardCandidate).go();
		log.info("{}", jsonVCard.equalsIgnoreCase(jsonVCardCandidate));

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<?> list = objectMapper.readValue(jsonVCard, List.class);
		String jsonList = Utility.toStringJson(list);
		VCard vcardCandidateList = Ezvcard.parseJson(jsonList).first();
		String jsonVCardCandidateList = Ezvcard.writeJson(vcardCandidateList).go();
		log.info("{}", jsonVCard.equalsIgnoreCase(jsonVCardCandidateList));

	}

	@Test
	public void testHtml() throws IllegalAccessException, InvocationTargetException, JsonMappingException, JsonProcessingException {
		String filename = "list-contact-20230313-short.vcf";
		//filename = "list-contact-20230313.vcf";
		String input = Utility.readClassPathFile(filename).replaceAll("[\r\n]+", "\n");
		List<VCard> vcards = Ezvcard.parse(input).all();
		for(VCard vcard: vcards) {
			String htmlVCard = Ezvcard.writeHtml(vcard).go();
			log.info("html: {}", htmlVCard);
		}
	}

	@Test
	public void testXml() throws IllegalAccessException, InvocationTargetException, JsonMappingException, JsonProcessingException {
		String filename = "list-contact-20230313-short.vcf";
		//filename = "list-contact-20230313.vcf";
		String input = Utility.readClassPathFile(filename).replaceAll("[\r\n]+", "\n");
		List<VCard> vcards = Ezvcard.parse(input).all();
		for(VCard vcard: vcards) {
			String xmlVCard = Ezvcard.writeXml(vcard).go();
			log.info("xml: {}", xmlVCard);
		}
	}

	@Test
	public void testJsons() throws IllegalAccessException, InvocationTargetException, JsonMappingException, JsonProcessingException {
		String filename = "list-contact-20230313-short.vcf";
		filename = "list-contact-20230313.vcf";
		String input = Utility.readClassPathFile(filename).replaceAll("[\r\n]+", "\n");
		List<VCard> vcards = Ezvcard.parse(input).all();
		for(VCard vcard: vcards) {
			testJsons(vcard);
		}
	}

	private void testJsons(VCard vcard) throws JsonMappingException, JsonProcessingException {
		// vcard => json
		String jsonVCard = Ezvcard.writeJson(vcard).go();

		// json => list
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<?> list = objectMapper.readValue(jsonVCard, List.class);

		// list => json
		String jsonList = Utility.toStringJson(list);

		// json => vcard
		VCard vcardCandidateList = Ezvcard.parseJson(jsonList).first();
		
		// vcard => json
		String jsonVCardCandidateList = Ezvcard.writeJson(vcardCandidateList).go();

		if (!jsonVCard.equalsIgnoreCase(jsonVCardCandidateList)) {
			log.info("origin: {}", jsonVCard);
			log.info("after:  {}", jsonVCardCandidateList);
		}
	}

	@Test
	public void test() throws IllegalAccessException, InvocationTargetException {
		String filename = "list-contact-20230313-short.vcf";
		//filename = "list-contact-20230313.vcf";
		String input = Utility.readClassPathFile(filename).replaceAll("[\r\n]+", "\n");
		List<VCard> vcards = Ezvcard.parse(input).all();
		Map<Object, Integer> map = new HashMap<Object, Integer>();
		String string = testValue(vcards, map);
		log.info("#{} {}", Utility.size(vcards), string);
	}

	@SuppressWarnings("rawtypes")
	private String testValue(Object object, Map<Object, Integer> set) throws IllegalAccessException, InvocationTargetException {
		if (object == null) {
			return "";
		}
		
		Integer count = set.get(object);
		if (count == null) {
			count = 0;
		}
		if (count > 8) {
			return "";
		}
		
		StringBuffer sb = new StringBuffer("");
		if (object instanceof List) {
			set.put(object, count + 1);
			List list = (List)object;
			sb.append("[");
			for (Object child : list) {
				sb.append(testValue(child, set));
				sb.append(", ");
			}
			sb.append("]");

			return sb.toString();
		}
		if (object instanceof Map) {
			set.put(object, count + 1);
			Map map = (Map)object;
			sb.append("{");
			for (Object key : map.keySet()) {
				sb.append(key);
				sb.append(": ");
				sb.append(testValue(map.get(key), set));
				sb.append(", ");
			}
			sb.append("}");

			return sb.toString();
		}
		if (object instanceof String) {
			set.put(object, count + 1);
			return (String)object;
		}

		return testGetter(object, set);
	}

	private String testGetter(Object object, Map<Object, Integer> set) throws IllegalAccessException, InvocationTargetException {
		if (object == null) {
			return "";
		}
		
		Integer count = set.get(object);
		if (count == null) {
			count = 0;
		}
		if (count > 8) {
			return "";
		}
		set.put(object, count + 1);
		
		String classSimpleName = object.getClass().getSimpleName();
		StringBuffer sb = new StringBuffer("");
		sb.append("{");
		Method[] methods = object.getClass().getMethods();
		List<Method> getters = new ArrayList<Method>();
		for (Method method : methods) {
			String name = method.getName();
			if (name.startsWith("get") && method.getParameterTypes().length == 0) {
				getters.add(method);
			}

		}
		if (getters.isEmpty()) {
			log.info("{} {}", Utility.indentMiddle(), classSimpleName);
			return "";
		}
		for (Method method : methods) {
			String name = method.getName();
			if (name.startsWith("get") && method.getParameterTypes().length == 0) {
				String mname = name.substring(3);
				final Object valueChild = method.invoke(object);

				if (classSimpleName.equals("Class")) {
					continue;
				}
				switch (mname) {
					case "Class":
						continue;
					case "SupportedVersions", "Version":
						//						continue;

					case "TelephoneNumbers":
						log.info("{}", valueChild);
					default:
						break;
				}
				//log.info("{}:{}", classSimpleName, mname);
				sb.append(mname);
				sb.append(": ");
				sb.append(testValue(valueChild, set));
				sb.append(", ");
			}
		}

		sb.append("}");

		return sb.toString();
	}

	@SuppressWarnings("rawtypes")
	private String testValue(Object object, Set<Object> set) throws IllegalAccessException, InvocationTargetException {
		if (object == null || set.contains(object)) {
			return "";
		}

		StringBuffer sb = new StringBuffer("");
		if (object instanceof List) {
			set.add(object);
			List list = (List)object;
			sb.append("[");
			for (Object child : list) {
				sb.append(testValue(child, set));
				sb.append(", ");
			}
			sb.append("]");

			return sb.toString();
		}
		if (object instanceof Map) {
			set.add(object);
			Map map = (Map)object;
			sb.append("{");
			for (Object key : map.keySet()) {
				sb.append(key);
				sb.append(": ");
				sb.append(testValue(map.get(key), set));
				sb.append(", ");
			}
			sb.append("}");

			return sb.toString();
		}
		if (object instanceof String) {
			set.add(object);
			return (String)object;
		}

		return testGetter(object, set);
	}

	private String testGetter(Object object, Set<Object> set) throws IllegalAccessException, InvocationTargetException {
		if (object == null) {
			return "";
		}

		if (set.contains(object)) {
			return "";
		}

		set.add(object);

		String classSimpleName = object.getClass().getSimpleName();
		StringBuffer sb = new StringBuffer("");
		sb.append("{");
		Method[] methods = object.getClass().getMethods();
		List<Method> getters = new ArrayList<Method>();
		for (Method method : methods) {
			String name = method.getName();
			if (name.startsWith("get") && method.getParameterTypes().length == 0) {
				getters.add(method);
			}

		}
		if (getters.isEmpty()) {
			log.info("{} {}", Utility.indentMiddle(), classSimpleName);
			return "";
		}
		for (Method method : methods) {
			String name = method.getName();
			if (name.startsWith("get") && method.getParameterTypes().length == 0) {
				String mname = name.substring(3);
				final Object valueChild = method.invoke(object);

				if (classSimpleName.equals("Class")) {
					continue;
				}
				switch (mname) {
					case "Class":
						continue;
					case "SupportedVersions", "Version":
						//						continue;

					case "TelephoneNumbers":
						log.info("{}", valueChild);
					default:
						break;
				}
				//log.info("{}:{}", classSimpleName, mname);
				sb.append(mname);
				sb.append(": ");
				sb.append(testValue(valueChild, set));
				sb.append(", ");
			}
		}

		sb.append("}");

		return sb.toString();
	}

}
