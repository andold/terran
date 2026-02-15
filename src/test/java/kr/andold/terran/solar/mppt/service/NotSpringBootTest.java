package kr.andold.terran.solar.mppt.service;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotSpringBootTest {
	@BeforeEach
	protected void setUp() throws Exception {
		log.info(Utility.HR);
	}

	@Test
	public void testRun() {
		log.info("{}", Float.parseFloat("0"));
		log.info("{}", Float.parseFloat("0.0"));
		log.info("{}", Pattern.matches("[^\\-]+", "-°C"));
		log.info("{}", Pattern.matches("[^--]+", "-°C"));
		log.info("{}", Pattern.matches("[^-]+", "-°C"));
		log.info("{}", Pattern.matches("[^\\-]+", "°C"));
		log.info("{}", Pattern.matches("[^--]+", "°C"));
		log.info("{}", Pattern.matches("[^-]+", "°C"));
		log.info("{}", Pattern.matches("[^a-z]+", "-°C"));
		log.info("{}", Pattern.matches(".+", "-°C"));
		log.info("{}", Utility.parseFloat("21.5°C".replaceAll("[^0-9\\.]", ""), null));
	}

}
