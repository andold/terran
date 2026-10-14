package kr.andold.terran.bhistory.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.andold.terran.ApplicationContextProvider;
import kr.andold.terran.bhistory.entity.BigHistoryEntity;
import kr.andold.utils.Utility;
import kr.andold.utils.job.JobInterface;
import kr.andold.utils.job.STATUS;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class BackupJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 300L;	//	TimeUnit.SECONDS
	@Getter @Setter private String userDataPath;

	@Override
	public STATUS call() throws Exception {
		log.info("{} BackupJob::call() - 『{}』『{}』", Utility.indentStart(), userDataPath, timeout);
		long started = System.currentTimeMillis();

		Worker worker = (Worker) ApplicationContextProvider.getBean(Worker.class);
		STATUS result = worker.run(this);

		log.info("{} 『#{}』 BackupJob::call() - 『{}』『{}』『{}』", Utility.indentEnd(), result, userDataPath, timeout, Utility.toStringPastTimeReadable(started));
		return result;
	}

	@RequiredArgsConstructor
	@Service
	public static class Worker {
		private final BigHistoryService service;

		protected STATUS run(BackupJob job) throws Exception {
			log.info("{} BackupJob.Worker::run() - 『{}』『{}』", Utility.indentStart(), job.userDataPath, job.timeout);

			List<BigHistoryEntity> histories = service.search(null);
			String text = Utility.toStringJsonLine(histories);
			String filename = String.format("%s/bhistory.json", job.userDataPath);
			Utility.write(filename, text);

			log.info("{} 『{}』『{}』 BackupJob.Worker::run() - 『{}』『{}』", Utility.indentEnd(), filename, Utility.ellipsis(text, 32, 32), job.userDataPath, job.timeout);
			return STATUS.SUCCESS;
		}

	}

}
