package kr.andold.terran.bhistory;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import kr.andold.terran.bhistory.service.BackupJob;
import kr.andold.terran.bhistory.service.JobService;
import kr.andold.terran.bhistory.service.ZookeeperClient;
import kr.andold.utils.Utility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableScheduling
public class ScheduledTasks {
	@Autowired private JobService jobService;
	@Autowired private ZookeeperClient zookeeperClient;

	@Getter private static String userDataPath;
	@Value("${application.data.path}")
	public void setUserDataPath(String value) {
		log.info("{} setUserDataPath({})", Utility.indentMiddle(), value);
		userDataPath = value;
		File directory = new File(value);
		if (!directory.exists()) {
			log.info("{} NOT EXIST PATH setDataPath({})", Utility.indentMiddle(), value);
			directory.mkdir();
		}
	}

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
		jobService.status(zookeeperClient.status(true));
	}

	// 매일
	@Scheduled(cron = "0 0 0 * * *")
	public void daily() {
		log.info("{} daily()", Utility.indentStart());

		if (zookeeperClient.isMaster()) {
			JobService.getQueue2().offer(BackupJob.builder().userDataPath(getUserDataPath()).build());
		}

		log.info("{} daily()", Utility.indentEnd());
	}

}
