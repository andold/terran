package kr.andold.terran;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import kr.andold.terran.service.JobService;
import kr.andold.terran.service.ZookeeperClient;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableScheduling
public class ScheduledTasks {
	@Autowired private JobService jobService;
	@Autowired private ZookeeperClient zookeeperClient;

	@Scheduled(initialDelay = 1000 * 10, fixedDelay = Long.MAX_VALUE)
	public void once() {
		log.info("{} once()", Utility.indentStart());

		zookeeperClient.run();

		log.info("{} once()", Utility.indentEnd());
	}

	// 1초쉬고
	@Scheduled(initialDelay = 1000 * 16, fixedDelay = 1000)
	public void secondly() {
		jobService.run();
	}

	// 매분
	@Scheduled(cron = "0 * * * * *")
	public void minutely() {
		log.trace("{} minutely()", Utility.indentStart());
		long started = System.currentTimeMillis();

		log.info("{} {} {} minutely()", Utility.indentMiddle(), zookeeperClient.status(true), jobService.status());

		log.trace("{} minutely() - {}", Utility.indentEnd(), Utility.toStringPastTimeReadable(started));
	}

}
