package kr.andold.terran.service;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UtilsTest {
	@Test
	public void testPutMap() throws IllegalAccessException, InvocationTargetException, JsonMappingException, JsonProcessingException {
		String filename = "list-contact-20230313-short.vcf";
		filename = "list-contact-20230313.vcf";
		String input = Utility.readClassPathFile(filename).replaceAll("[\r\n]+", "\n");
		List<VCard> vcards = Ezvcard.parse(input).all();
		Map<String, Object> mapSource = new HashMap<String, Object>();
		Utils.putMap(mapSource, "vcards", vcards);
		log.info("{} testPutMap() - #{}", Utility.indentMiddle(), Utility.size(mapSource));
	}

	@Test
	public void testToList() throws IllegalAccessException, InvocationTargetException, JsonMappingException, JsonProcessingException {
		String filename = "list-contact-20230313-short.vcf";
		filename = "list-contact-20230313.vcf";
		String input = Utility.readClassPathFile(filename).replaceAll("[\r\n]+", "\n");
		List<VCard> vcards = Ezvcard.parse(input).all();
		List<Object> list = Utils.toList(vcards);
		log.info("{} testToList() - #{}", Utility.indentMiddle(), Utility.size(list));
	}

}
