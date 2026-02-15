package kr.andold.terran.solar.mppt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.andold.utils.Utility;
import kr.andold.utils.job.STATUS;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SolarMpptMonitoringJobTest {
	@Autowired private SolarMpptMonitoringJob job;

	@BeforeEach
	protected void setUp() throws Exception {
		log.info(Utility.HR);
	}

	@Test
	public void main() throws Exception {
		STATUS result = job.call();
		log.info("{}", result);
	}

}
