package kr.andold.terran;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import kr.andold.terran.bookmark.domain.BookmarkParam;
import kr.andold.terran.bookmark.service.BookmarkService;
import kr.andold.terran.ics.service.IcsBackupJob;
import kr.andold.terran.service.ContactBackupJob;
import kr.andold.terran.service.JobService;
import kr.andold.terran.service.TerranService;
import kr.andold.terran.service.ZookeeperClient;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableScheduling
public class ScheduledTasks {
	@Autowired private BookmarkService bookmarkService;
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

	// 매시간
	@Scheduled(cron = "0 56 * * * *")
	public void hourly() {
		log.trace("{} hourly()", Utility.indentStart());
		long started = System.currentTimeMillis();

		if (zookeeperClient.isMaster()) {
			bookmarkService.aggreagateCount();
		}

		log.trace("{} hourly() - {}", Utility.indentEnd(), Utility.toStringPastTimeReadable(started));
	}

	// 매일
	@Scheduled(cron = "0 0 0 * * *")
	public void daily() {
		if (zookeeperClient.isMaster()) {
			JobService.getQueue2().offer(IcsBackupJob.builder().dataPath(TerranService.getApplicationDataPath()).build());
			JobService.getQueue2().offer(ContactBackupJob.builder().build());

			String yyyymmdd = LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);

			BookmarkParam param = bookmarkService.download();
			String text = Utility.toStringJsonPretty(param);
			String filenameCurrent = String.format("%s/bookmark.json", TerranService.getApplicationDataPath());
			String filenameYesterday = String.format("%s/bookmark-%s.json", TerranService.getApplicationDataPath(), yyyymmdd);
			rename(filenameCurrent, filenameYesterday);
			Utility.write(filenameCurrent, text);
		}
	}

	private void rename(String before, String after) {
		try {
			Path oldfile = Paths.get(before);
			Path newfile = Paths.get(after);
			Files.move(oldfile, newfile);
		} catch (IOException e) {
			log.error("{} IOException rename(『{}』, 『{}』) - 『{}』", Utility.indentMiddle(), before, after, e.getLocalizedMessage(), e);
		}
	}

}
