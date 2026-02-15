package kr.andold.terran.solar.mppt.service;

import java.time.LocalDateTime;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.andold.terran.solar.mppt.param.SolarMpptParam;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SolarMpptServiceTest {
	@Autowired private SolarMpptService service;

	@BeforeEach
	public void before() {
		log.info(Utility.HR);
	}

	@Test
	public void testCrawl() {
		Date start = Date.from(LocalDateTime.now().minusHours(1).toInstant(Utility.ZONE_OFFSET_KST));
		SolarMpptParam solarMpptParam = SolarMpptParam.builder().start(start).build();
		service.crawl(solarMpptParam);
	}

	@Test
	public void createDriver() {
		ChromeDriverWrapper driver = SolarMpptService.createDriver(false, "t");
		driver.get(SolarMpptService.URL);
		Utility.sleep(4000);
		driver.quit();
	}

}
