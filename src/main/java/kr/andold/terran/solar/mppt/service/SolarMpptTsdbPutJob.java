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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@SuperBuilder
@NoArgsConstructor
@Slf4j
@Service
public class SolarMpptTsdbPutJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 300L;
	@Getter @Setter private CrudList<SolarMpptDomain> crud;

	@Autowired private TsdbService service;

	@Override
	public STATUS call() throws Exception {
		log.info("{} SolarMpptTsdbCreateJob::call()", Utility.indentStart());
		long started = System.currentTimeMillis();

		SolarMpptTsdbPutJob that = (SolarMpptTsdbPutJob) ApplicationContextProvider.getBean(SolarMpptTsdbPutJob.class);
		that.setCrud(crud);
		CrudList<TsdbDomain> result = that.main();

		log.info("{} 『#{}』 SolarMpptTsdbCreateJob::call() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return STATUS.SUCCESS;
	}

	protected CrudList<TsdbDomain> main() throws Exception {
		log.info("{} SolarMpptTsdbPutJob::main() - 『{}』", Utility.indentStart(), crud);
		long started = System.currentTimeMillis();

		Date minBase = null;
		Date maxBase = null;
		List<TsdbDomain> afters = new ArrayList<>();
		List<SolarMpptDomain> creates = crud.getCreates();
		if (creates != null) {
			for (SolarMpptDomain solarMpptDomain : creates) {
				List<TsdbDomain> tsdbDomains = TsdbDomain.of(solarMpptDomain);
				afters.addAll(tsdbDomains);
				minBase = Utility.min(minBase, solarMpptDomain.getBase());
				maxBase = Utility.max(maxBase, solarMpptDomain.getBase());
			}
		}
		List<SolarMpptDomain> duplicates = crud.getDuplicates();
		if (duplicates != null) {
			for (SolarMpptDomain solarMpptDomain : crud.getDuplicates()) {
				List<TsdbDomain> tsdbDomains = TsdbDomain.of(solarMpptDomain);
				afters.addAll(tsdbDomains);
				minBase = Utility.min(minBase, solarMpptDomain.getBase());
				maxBase = Utility.max(maxBase, solarMpptDomain.getBase());
			}
		}
		List<SolarMpptDomain> updates = crud.getUpdates();
		if (updates != null) {
			for (SolarMpptDomain solarMpptDomain : crud.getUpdates()) {
				List<TsdbDomain> tsdbDomains = TsdbDomain.of(solarMpptDomain);
				afters.addAll(tsdbDomains);
				minBase = Utility.min(minBase, solarMpptDomain.getBase());
				maxBase = Utility.max(maxBase, solarMpptDomain.getBase());
			}
		}
		List<SolarMpptDomain> removes = crud.getRemoves();
		if (removes != null) {
			for (SolarMpptDomain solarMpptDomain : removes) {
				List<TsdbDomain> tsdbDomains = TsdbDomain.of(solarMpptDomain);
				afters.addAll(tsdbDomains);
				minBase = Utility.min(minBase, solarMpptDomain.getBase());
				maxBase = Utility.max(maxBase, solarMpptDomain.getBase());
			}
		}
		
		List<TsdbDomain> befores = service.search(TsdbParam.builder()
				.start(minBase)
				.end(maxBase)
				.includeEnd(true)
				.group(TsdbDomain.TSDB_GROUP_BASIS)
				.build());
		CrudList<TsdbDomain> crudTsdbDomain = service.differ(befores, afters);
		log.info("{} SolarMpptTsdbPutJob::main() - 『{}』『{}』『#{}』『#{}』", Utility.indentStart(), crud, crudTsdbDomain, Utility.size(befores), Utility.size(afters));
		int changes = service.batch(crudTsdbDomain);

		JobService.getQueue3().add(SolarMpptTsdbAggregateJob.builder()
				.start(LocalDateTime.ofInstant(minBase.toInstant(), Utility.ZONE_ID_KST))
				.end(LocalDateTime.ofInstant(maxBase.toInstant(), Utility.ZONE_ID_KST))
				.build());
		
		log.info("{} 『{}:{}』 SolarMpptTsdbPutJob::main() - 『{}』 - {}", Utility.indentEnd(), crudTsdbDomain, changes, crud, Utility.toStringPastTimeReadable(started));
		return crudTsdbDomain;
	}

}
