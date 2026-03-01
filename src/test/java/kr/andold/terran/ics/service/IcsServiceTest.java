package kr.andold.terran.ics.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IcsServiceTest {
	@Autowired private IcsService service;

	@BeforeEach
	public void before() {
		log.info(Utility.HR);
	}

	@Test
	public void downloadIcs() {
		assertThat(service);
		String ics = service.downloadIcs(1028);
		log.info("ics = {}", Utility.ellipsisEscape(ics, 64, 64));
	}

}
