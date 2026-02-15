package kr.andold.terran.solar.mppt.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.andold.terran.ApplicationContextProvider;
import kr.andold.terran.service.JobService;
import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
import kr.andold.terran.tsdb.domain.TsdbDomain;
import kr.andold.terran.tsdb.param.TsdbParam;
import kr.andold.terran.tsdb.service.TsdbService;
import kr.andold.utils.job.JobInterface;
import kr.andold.utils.job.STATUS;
import kr.andold.utils.persist.CrudList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Service
public class SolarMpptTsdbCreateJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 60L;
	@Getter @Setter private SolarMpptDomain domain;

	@Autowired private TsdbService service;

	@Override
	public STATUS call() throws Exception {
		log.info("{} SolarMpptTsdbCreateJob::call()", Utility.indentStart());
		long started = System.currentTimeMillis();

		SolarMpptTsdbCreateJob that = (SolarMpptTsdbCreateJob) ApplicationContextProvider.getBean(SolarMpptTsdbCreateJob.class);
		that.setDomain(domain);
		SolarMpptDomain result = that.main();

		log.info("{} 『#{}』 SolarMpptTsdbCreateJob::call() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return STATUS.SUCCESS;
	}

	protected SolarMpptDomain main() throws Exception {
		log.info("{} SolarMpptTsdbCreateJob::main() - 『{}』", Utility.indentStart(), domain);
		long started = System.currentTimeMillis();

		Date date = domain.getBase();
		List<TsdbDomain> beforesBasis = service.search(TsdbParam.builder().group(TsdbDomain.TSDB_GROUP_BASIS).base(date).build());
		List<TsdbDomain> aftersBasis = new ArrayList<>();
		if (domain.getTemperature() >= 0) {
			aftersBasis.add(TsdbDomain.builder().group(TsdbDomain.TSDB_GROUP_BASIS).base(date).member(TsdbDomain.TSDB_MEMBER_TEMPERATURE).value(domain.getTemperature().toString()).build());
		}
		if (domain.getDischarge() >= 0) {
			aftersBasis.add(TsdbDomain.builder().group(TsdbDomain.TSDB_GROUP_BASIS).base(date).member(TsdbDomain.TSDB_MEMBER_DISCHARGE).value(domain.getDischarge().toString()).build());
		}
		if (domain.getCharge() >= 0) {
			aftersBasis.add(TsdbDomain.builder().group(TsdbDomain.TSDB_GROUP_BASIS).base(date).member(TsdbDomain.TSDB_MEMBER_CHARGE).value(domain.getCharge().toString()).build());
		}
		if (domain.getVoltage() >= 0) {
			aftersBasis.add(TsdbDomain.builder().group(TsdbDomain.TSDB_GROUP_BASIS).base(date).member(TsdbDomain.TSDB_MEMBER_VOLTAGE).value(domain.getVoltage().toString()).build());
		}
		CrudList<TsdbDomain> crudBasis = service.differ(beforesBasis, aftersBasis);
		int resultBasis = service.batch(crudBasis);
		log.info("{} SolarMpptTsdbCreateJob::main() - 『{}』『{}:{}:{}』", Utility.indentMiddle(), domain, "", crudBasis, resultBasis);
		
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), Utility.ZONE_ID_KST);
		JobService.getQueue3().add(SolarMpptTsdbAggregateJob.builder().start(ldt).end(ldt).build());

		log.info("{} 『{}』 SolarMpptTsdbCreateJob::main() - 『{}』 - {}", Utility.indentEnd(), null, domain, Utility.toStringPastTimeReadable(started));
		return null;
	}

}
