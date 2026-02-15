package kr.andold.terran.solar.mppt.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.andold.terran.ApplicationContextProvider;
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
public class SolarMpptTsdbAggregateJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 300L;
	@Getter @Setter private LocalDateTime start;
	@Getter @Setter private LocalDateTime end;

	@Autowired private TsdbService service;

	@Override
	public STATUS call() throws Exception {
		log.info("{} SolarMpptTsdbAggregateJob::call()", Utility.indentStart());
		long started = System.currentTimeMillis();

		SolarMpptTsdbAggregateJob that = (SolarMpptTsdbAggregateJob) ApplicationContextProvider.getBean(SolarMpptTsdbAggregateJob.class);
		that.setStart(start);
		that.setEnd(end);
		SolarMpptDomain result = that.main();

		log.info("{} 『#{}』 SolarMpptTsdbAggregateJob::call() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return STATUS.SUCCESS;
	}

	protected SolarMpptDomain main() throws Exception {
		log.info("{} SolarMpptTsdbAggregateJob::main({} ~ {})", Utility.indentStart(), start, end);
		long started = System.currentTimeMillis();

		LocalDateTime start1h = start.truncatedTo(ChronoUnit.HOURS);
		LocalDateTime end1h = end.plusHours(1).truncatedTo(ChronoUnit.HOURS);
		List<TsdbDomain> basis1h = service.search(TsdbParam.builder()
				.group(TsdbDomain.TSDB_GROUP_BASIS)
				.start(Date.from(start1h.toInstant(Utility.ZONE_OFFSET_KST)))
				.end(Date.from(end1h.toInstant(Utility.ZONE_OFFSET_KST)))
				.build());
		List<TsdbDomain> afters1h = aggregate1h(basis1h);
		List<TsdbDomain> befores1h = service.search(TsdbParam.builder()
				.group(TsdbDomain.TSDB_GROUP_1H)
				.start(Date.from(start1h.toInstant(Utility.ZONE_OFFSET_KST)))
				.end(Date.from(end1h.toInstant(Utility.ZONE_OFFSET_KST)))
				.build());
		CrudList<TsdbDomain> crudBasis1h = service.differ(befores1h, afters1h);
		int resultBasis1h = service.batch(crudBasis1h);
		log.info("{} SolarMpptTsdbAggregateJob::main({} ~ {}) - 『{}』『#{}』", Utility.indentMiddle(), start, end, crudBasis1h, resultBasis1h);
		
		LocalDateTime start1d = start.truncatedTo(ChronoUnit.DAYS);
		LocalDateTime end1d = end.plusDays(1).truncatedTo(ChronoUnit.DAYS);
		List<TsdbDomain> basis1d = service.search(TsdbParam.builder()
				.group(TsdbDomain.TSDB_GROUP_BASIS)
				.start(Date.from(start1d.toInstant(Utility.ZONE_OFFSET_KST)))
				.end(Date.from(end1d.toInstant(Utility.ZONE_OFFSET_KST)))
				.build());
		List<TsdbDomain> afters1d = aggregate1d(basis1d);
		List<TsdbDomain> befores1d = service.search(TsdbParam.builder()
				.group(TsdbDomain.TSDB_GROUP_1D)
				.start(Date.from(start1d.toInstant(Utility.ZONE_OFFSET_KST)))
				.end(Date.from(end1d.toInstant(Utility.ZONE_OFFSET_KST)))
				.build());
		CrudList<TsdbDomain> crudBasis1d = service.differ(befores1d, afters1d);
		int resultBasis1d = service.batch(crudBasis1d);
		log.info("{} SolarMpptTsdbAggregateJob::main({} ~ {}) - 『{}』『#{}』", Utility.indentMiddle(), start, end, crudBasis1d, resultBasis1d);
		
		log.info("{} 『{}』 SolarMpptTsdbAggregateJob::main({} ~ {}) - {}", Utility.indentEnd(), resultBasis1h, start, end, Utility.toStringPastTimeReadable(started));
		return null;
	}

	public static List<TsdbDomain> aggregate1h(List<TsdbDomain> domains) {
		log.info("{} aggregate1h(#{})", Utility.indentStart(), Utility.size(domains));
		long started = System.currentTimeMillis();

		Map<String, Map<String, Integer>> mapMember = new HashMap<>();
		for (TsdbDomain domain : domains) {
			LocalDateTime ldt = LocalDateTime.ofInstant(domain.getBase().toInstant(), Utility.ZONE_ID_KST).truncatedTo(ChronoUnit.HOURS);
			String key = String.format("%s.%s.%s", ldt.format(DateTimeFormatter.ISO_DATE_TIME), domain.getGroup(), domain.getMember());
			Map<String, Integer> member = mapMember.get(key);
			if (member == null) {
				member = new HashMap<>();
				mapMember.put(key, member);
			}
			
			String value = domain.getValue();
			Integer count = member.get(value);
			if (count == null) {
				member.put(value, 1);
				continue;
			}

			member.put(value, count + 1);
		}

		List<TsdbDomain> aggregated = new ArrayList<>();
		// 최빈
		for (String key : mapMember.keySet()) {
			String[] tokens = key.split("\\.");
			String value = "";
			Map<String, Integer> mapValue = mapMember.get(key);
			int cx = 0;
			for (String keyValue : mapValue.keySet()) {
				int cy =  mapValue.get(keyValue);
				if (cx < cy) {
					cx = cy;
					value = keyValue;
				}
			}
			TsdbDomain domain = TsdbDomain.builder()
					.base(Date.from(LocalDateTime.parse(tokens[0], DateTimeFormatter.ISO_DATE_TIME).toInstant(Utility.ZONE_OFFSET_KST)))
					.group(TsdbDomain.TSDB_GROUP_1H)
					.member(tokens[2])
					.value(value)
					.build();
			aggregated.add(domain);
		}

		log.info("{} 『#{}』 aggregate1h(#{}) - {}", Utility.indentEnd(), Utility.size(aggregated), Utility.size(domains), Utility.toStringPastTimeReadable(started));
		return aggregated;
	}

	public static List<TsdbDomain> aggregate1d(List<TsdbDomain> domains) {
		log.info("{} aggregate1d(#{})", Utility.indentStart(), Utility.size(domains));
		long started = System.currentTimeMillis();

		Map<String, String> mapMember = new HashMap<>();
		for (TsdbDomain domain : domains) {
			LocalDateTime ldt = LocalDateTime.ofInstant(domain.getBase().toInstant(), Utility.ZONE_ID_KST).truncatedTo(ChronoUnit.DAYS);
			String member = domain.getMember();
			String value = domain.getValue();
			switch (member) {
			case TsdbDomain.TSDB_MEMBER_TEMPERATURE:
			case TsdbDomain.TSDB_MEMBER_VOLTAGE:
				String keyMin = String.format("%s.%s.%s%s", ldt.format(DateTimeFormatter.ISO_DATE_TIME), domain.getGroup(), member, TsdbDomain.TSDB_MEMBER_MEMBER_POSTFIX_MIN);
				putIfLess(mapMember, keyMin, value);
				break;
			default:
				break;
			}

			String key = String.format("%s.%s.%s", ldt.format(DateTimeFormatter.ISO_DATE_TIME), domain.getGroup(), member);
			putIfGreater(mapMember, key, value);
		}

		List<TsdbDomain> aggregated = new ArrayList<>();
		for (String key : mapMember.keySet()) {
			String[] tokens = key.split("\\.");
			String value = mapMember.get(key);
			TsdbDomain domain = TsdbDomain.builder()
					.base(Date.from(LocalDateTime.parse(tokens[0], DateTimeFormatter.ISO_DATE_TIME).toInstant(Utility.ZONE_OFFSET_KST)))
					.group(TsdbDomain.TSDB_GROUP_1D)
					.member(tokens[2])
					.value(value)
					.build();
			aggregated.add(domain);
		}

		log.info("{} 『{}』 aggregate1d(#{}) - {}", Utility.indentEnd(), Utility.size(aggregated), Utility.size(domains), Utility.toStringPastTimeReadable(started));
		return aggregated;
	}

	private static void putIfLess(Map<String, String> map, String key, String current) {
		String previous = map.get(key);

		Float fCurrent = Utility.parseFloat(current, null);
		Float fPrevious = Utility.parseFloat(previous, null);

		if (fCurrent == null) {
			return;
		}

		// max
		if (fPrevious == null || fCurrent < fPrevious) {
			map.put(key, current);
			return;
		}
	}
	
	private static void putIfGreater(Map<String, String> map, String key, String current) {
		String previous = map.get(key);

		Float fCurrent = Utility.parseFloat(current, null);
		Float fPrevious = Utility.parseFloat(previous, null);

		if (fCurrent == null) {
			return;
		}

		// max
		if (fPrevious == null || fCurrent > fPrevious) {
			map.put(key, current);
			return;
		}
	}
	
}
