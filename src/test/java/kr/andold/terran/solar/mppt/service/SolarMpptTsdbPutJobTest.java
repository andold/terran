package kr.andold.terran.solar.mppt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
import kr.andold.terran.tsdb.domain.TsdbDomain;
import kr.andold.utils.Utility;
import kr.andold.utils.persist.CrudList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SolarMpptTsdbPutJobTest {
	@Autowired private SolarMpptTsdbPutJob job;
	@Autowired private SolarMpptService service;

	@BeforeEach
	protected void setUp() throws Exception {
		log.info(Utility.HR);
	}

	@Test
	public void testMain() throws Exception {
		String filename = "solar-mppt-20250317.json";
		String text = Utility.readClassPathFile(filename);
		CrudList<SolarMpptDomain> crudSolarMpptDomain = service.upload(text);
		job.setCrud(crudSolarMpptDomain);
		CrudList<TsdbDomain> result = job.main();
		log.info("{}", result);
	}

}
