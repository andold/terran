package kr.andold.terran.solar.mppt.service;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.andold.terran.ApplicationContextProvider;
import kr.andold.terran.service.JobService;
import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
import kr.andold.terran.solar.mppt.param.SolarMpptParam;
import kr.andold.terran.solar.mppt.service.SolarMpptCrawlDetailsJob.RestartDriverJob;
import kr.andold.utils.job.JobInterface;
import kr.andold.utils.job.STATUS;
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
public class SolarMpptMonitoringJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 60L;

	@Autowired private SolarMpptService service;

	@Override
	public STATUS call() throws Exception {
		log.info("{} SolarMpptMonitoringJob::call()", Utility.indentStart());
		long started = System.currentTimeMillis();

		SolarMpptMonitoringJob that = (SolarMpptMonitoringJob) ApplicationContextProvider.getBean(SolarMpptMonitoringJob.class);
		STATUS result = that.main();

		log.info("{} 『#{}』 SolarMpptMonitoringJob::call() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return result;
	}

	public static void regist(ConcurrentLinkedDeque<JobInterface> deque) {
		log.info("{} SolarMpptMonitoringJob::regist(『#{}』)", Utility.indentStart(), Utility.size(deque));

		if (containsOrModify(JobService.getQueue0())) {
			log.info("{} ALREADY-IN-0 SolarMpptMonitoringJob::regist(『#{}』)", Utility.indentEnd(), Utility.size(deque));
			return;
		}
		if (containsOrModify(JobService.getQueue1())) {
			log.info("{} ALREADY-IN-1 SolarMpptMonitoringJob::regist(『#{}』)", Utility.indentEnd(), Utility.size(deque));
			return;
		}
		if (containsOrModify(JobService.getQueue2())) {
			log.info("{} ALREADY-IN-2 SolarMpptMonitoringJob::regist(『#{}』)", Utility.indentEnd(), Utility.size(deque));
			return;
		}
		if (containsOrModify(JobService.getQueue3())) {
			log.info("{} ALREADY-IN-3 SolarMpptMonitoringJob::regist(『#{}』)", Utility.indentEnd(), Utility.size(deque));
			return;
		}

		deque.addLast(SolarMpptMonitoringJob.builder().build());
		log.info("{} REGISTERED SolarMpptMonitoringJob::regist(『#{}』)", Utility.indentEnd(), Utility.size(deque));
	}

	private static boolean containsOrModify(ConcurrentLinkedDeque<JobInterface> deque) {
		for (JobInterface job : deque) {
			if (containsOrModify(job)) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsOrModify(JobInterface job) {
		if (!(job instanceof SolarMpptMonitoringJob)) {
			return false;
		}

		return true;
	}

	protected STATUS main() throws Exception {
		log.info("{} SolarMpptMonitoringJob::main()", Utility.indentStart());
		long started = System.currentTimeMillis();

		try {
			ZonedDateTime start = ZonedDateTime.now().minusMinutes(20);
			List<SolarMpptDomain> domains = service.search(SolarMpptParam.builder().start(Date.from(start.toInstant())).build());
			if (domains == null || domains.isEmpty()) {
				RestartDriverJob.regist(JobService.getQueue1());
				SolarMpptCrawlDetailsJob.regist(JobService.getQueue1());

				log.info("{} 『{}』 SolarMpptMonitoringJob::main() - {}", Utility.indentEnd(), STATUS.SUCCESS, Utility.toStringPastTimeReadable(started));
				return STATUS.SUCCESS;
			}

			ZonedDateTime oneHoursAgo = ZonedDateTime.now().minusHours(1);
			List<SolarMpptDomain> oneHours = service.search(SolarMpptParam.builder().start(Date.from(oneHoursAgo.toInstant())).build());
			if (oneHours == null || oneHours.isEmpty()) {
				RestartDriverJob.regist(JobService.getQueue1());
				SolarMpptCrawlDetailsJob.regist(JobService.getQueue1());

				log.info("{} 『{}』 SolarMpptMonitoringJob::main() - {}", Utility.indentEnd(), STATUS.SUCCESS, Utility.toStringPastTimeReadable(started));
				return STATUS.SUCCESS;
			}
			boolean allTheSame = true;
			Float previousTemperature = null;
			for (SolarMpptDomain domain : oneHours) {
				Float temperature = domain.getTemperature();
				if (temperature == null) {
					continue;
				}

				if (previousTemperature == null) {
					previousTemperature = temperature;
				}

				if (Utility.compareFloat(previousTemperature, temperature, 0.01f) != 0) {
					allTheSame = false;
					break;
				}
			}
			if (allTheSame) {
				RestartDriverJob.regist(JobService.getQueue1());
				SolarMpptCrawlDetailsJob.regist(JobService.getQueue1());

				log.info("{} 『{}』 SolarMpptMonitoringJob::main() - {}", Utility.indentEnd(), STATUS.SUCCESS, Utility.toStringPastTimeReadable(started));
				return STATUS.SUCCESS;
			}
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		log.info("{} 『{}』 SolarMpptMonitoringJob::main() - {}", Utility.indentEnd(), STATUS.ALEADY_DONE, Utility.toStringPastTimeReadable(started));
		return STATUS.ALEADY_DONE;
	}

}
