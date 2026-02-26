package kr.andold.terran.ics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import kr.andold.terran.ics.service.BackupJob;
import kr.andold.terran.ics.service.IcsService;
import kr.andold.terran.service.JobService;
import kr.andold.terran.service.ZookeeperClient;

@Configuration
@EnableScheduling
public class ScheduledTasks {
	@Autowired private ZookeeperClient zookeeperClient;

	// 매일
	@Scheduled(cron = "0 0 0 * * *")
	public void daily() {
		if (zookeeperClient.isMaster()) {
			JobService.getQueue2().offer(BackupJob.builder().dataPath(IcsService.getUserDataPath()).build());
		}
	}

}
