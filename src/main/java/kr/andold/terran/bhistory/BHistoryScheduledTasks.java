package kr.andold.terran.bhistory;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import kr.andold.terran.bhistory.service.BackupJob;
import kr.andold.terran.service.JobService;
import kr.andold.terran.service.ZookeeperClient;
import kr.andold.utils.Utility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableScheduling
public class BHistoryScheduledTasks {
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
