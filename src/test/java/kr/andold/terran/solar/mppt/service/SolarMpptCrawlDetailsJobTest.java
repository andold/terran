package kr.andold.terran.solar.mppt.service;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import kr.andold.utils.Utility;
import kr.andold.utils.job.STATUS;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SolarMpptCrawlDetailsJobTest {
	@Autowired private Environment env;
	
	@BeforeEach
	protected void setUp() throws Exception {
		log.info(Utility.HR);
		log.info("{}", Arrays.asList(env.getActiveProfiles()).isEmpty());
	}

	@Test
	public void testSolarMpptCrawlDetailsJob() throws Exception {
		STATUS result = SolarMpptCrawlDetailsJob.builder().build().call();
		log.info("{} {}", Utility.indentEnd(), result);
	}

}
