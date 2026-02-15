package kr.andold.terran.solar.mppt;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import kr.andold.terran.service.JobService;
import kr.andold.terran.service.ZookeeperClient;
import kr.andold.terran.solar.mppt.service.SolarMpptBackupJob;
import kr.andold.terran.solar.mppt.service.SolarMpptCrawlDetailsJob;
import kr.andold.terran.solar.mppt.service.SolarMpptMonitoringJob;
import kr.andold.terran.solar.mppt.service.SolarMpptTsdbAggregateJob;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableScheduling
public class SolarMpptScheduledTasks {
	@Autowired private ZookeeperClient zookeeperClient;

	// 매5분
	@Scheduled(cron = "0 */5 * * * *")
	public void minutely5() {
		log.trace("{} minutely5()", Utility.indentStart());

		if (zookeeperClient.isMaster()) {
			SolarMpptCrawlDetailsJob.regist(JobService.getQueue2());
			SolarMpptMonitoringJob.regist(JobService.getQueue2());
		}

		log.trace("{} minutely5()", Utility.indentEnd());
	}

	// 매시
	@Scheduled(cron = "0 0 * * * *")
	public void hourly() {
		log.debug("{} hourly()", Utility.indentStart());
		long started = System.currentTimeMillis();
		
		if (zookeeperClient.isMaster()) {
			JobService.getQueue2().add(SolarMpptTsdbAggregateJob.builder()
					.start(LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.HOURS))
					.end(LocalDateTime.now().truncatedTo(ChronoUnit.HOURS))
					.build());
		}

		log.debug("{} hourly() - {}", Utility.indentEnd(), Utility.toStringPastTimeReadable(started));
	}

	// 매일
	@Scheduled(cron = "0 0 0 * * *")
	public void daily() {
		log.debug("{} daily()", Utility.indentStart());
		long started = System.currentTimeMillis();

		if (zookeeperClient.isMaster()) {
			JobService.getQueue2().add(SolarMpptBackupJob.builder().build());
//			JobService.getQueue2().add(SolarMpptCrawlDetailsJob.RestartDriverJob.builder().build());
			SolarMpptCrawlDetailsJob.RestartDriverJob.regist(JobService.getQueue2());
			JobService.getQueue2().add(SolarMpptTsdbAggregateJob.builder()
					.start(LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.DAYS))
					.end(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS))
					.build());
		}

		log.debug("{} daily() - {}", Utility.indentEnd(), Utility.toStringPastTimeReadable(started));
	}

}
