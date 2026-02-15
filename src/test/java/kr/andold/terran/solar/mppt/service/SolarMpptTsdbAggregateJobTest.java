package kr.andold.terran.solar.mppt.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
import kr.andold.terran.tsdb.domain.TsdbDomain;
import kr.andold.terran.tsdb.service.TsdbService;
import kr.andold.utils.Utility;
import kr.andold.utils.persist.CrudList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SolarMpptTsdbAggregateJobTest {
	@Autowired private SolarMpptTsdbAggregateJob job;
	@Autowired private TsdbService service;

	@BeforeEach
	public void before() {
		log.info(Utility.HR);
	}

	@Test
	public void testDifferTsdb() {
		List<TsdbDomain> befores = new ArrayList<>();
		befores.add(TsdbDomain.builder()
				.base(Date.from(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant(Utility.ZONE_OFFSET_KST)))
				.group(TsdbDomain.TSDB_GROUP_1D)
				.member(TsdbDomain.TSDB_MEMBER_CHARGE)
				.value(null)
				.build());
		List<TsdbDomain> afters = new ArrayList<>();
		afters.add(TsdbDomain.builder()
				.base(Date.from(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant(Utility.ZONE_OFFSET_KST)))
				.group(TsdbDomain.TSDB_GROUP_1D)
				.member(TsdbDomain.TSDB_MEMBER_CHARGE)
				.value("0.1")
				.build());
		CrudList<TsdbDomain> result = service.differ(befores, afters);
		log.info("{}", result);
		log.info("{}", result.getUpdates());
		log.info("{}", result.getUpdates().get(0));
	}

	@Test
	public void testMain() throws Exception {
		job.setStart(LocalDateTime.now().minusDays(7));
		job.setEnd(LocalDateTime.now());
		SolarMpptDomain result = job.main();
		log.info("{}", result);
	}

	@Test
	public void testAggregate1d() {
		List<TsdbDomain> domains = new ArrayList<>();
		domains.add(TsdbDomain.builder()
				.base(new Date())
				.group(TsdbDomain.TSDB_GROUP_BASIS)
				.member(TsdbDomain.TSDB_MEMBER_CHARGE)
				.build());
		for (int cx = 0; cx < 128; cx++) {
			domains.add(TsdbDomain.builder()
					.base(new Date())
					.group(TsdbDomain.TSDB_GROUP_BASIS)
					.member(TsdbDomain.TSDB_MEMBER_CHARGE)
					.value(Float.toString(0.0f + 0.1f * cx))
					.build());
		}
		List<TsdbDomain> result = SolarMpptTsdbAggregateJob.aggregate1d(domains);
		log.info("{}", result);
	}

}
