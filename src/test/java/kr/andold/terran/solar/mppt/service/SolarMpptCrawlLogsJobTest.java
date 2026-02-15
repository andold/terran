package kr.andold.terran.solar.mppt.service;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.andold.terran.solar.mppt.param.SolarMpptParam;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SolarMpptCrawlLogsJobTest {
	@Autowired private SolarMpptCrawlLogsJob job;


	@BeforeEach
	protected void setUp() throws Exception {
		log.info(Utility.HR);
	}

	@Test
	public void testMain2() throws Exception {
		SolarMpptParam result = job.main2(ZonedDateTime.now().minusHours(1));
		log.info("{}", result);
	}

}
