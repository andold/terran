package kr.andold.terran.tsdb.service;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TsdbServiceTest {
	@BeforeEach
	public void before() {
		log.info(Utility.HR);
	}

	@Test
	public void testKey() {
		log.info("{}", String.format("%1$tF %1$tR.%s.%s", new Date(), "group", "member"));
		log.info("{}", String.format("%tF %<tR.%s.%s", new Date(), "group", "member"));
	}

}
