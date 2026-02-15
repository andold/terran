package kr.andold.terran.solar.mppt.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SolarMpptTsdbCreateJobTest {
	@BeforeEach
	public void before() {
		log.info(Utility.HR);
	}

	@Test
	public void testMain() {
		
	}

	@Test
	public void testAggregate1h() {
		LocalDateTime ldtNow = LocalDateTime.now();
		LocalDateTime ldtTruncatedToDays = ldtNow.truncatedTo(ChronoUnit.DAYS);
		LocalDateTime ldtTruncatedToHours = ldtNow.truncatedTo(ChronoUnit.HOURS);
		log.info("{} {} {}", ldtNow, ldtTruncatedToHours, ldtTruncatedToDays);
	}

}
