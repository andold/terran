package kr.andold.terran.ics.service;

import org.springframework.stereotype.Service;

import kr.andold.terran.ApplicationContextProvider;
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
public class IcsBackupJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 60L;
	@Setter private String dataPath;

	@Override
	public STATUS call() throws Exception {
		log.info("{} IcsBackupJob::call()", Utility.indentStart());
		long started = System.currentTimeMillis();

		Worker worker = (Worker) ApplicationContextProvider.getBean(Worker.class);
		STATUS result = worker.run(this);

		log.info("{} {} IcsBackupJob::call() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return result;
	}

	@RequiredArgsConstructor
	@Service
	public static class Worker {
		private final IcsService icsService;

		protected STATUS run(IcsBackupJob job) {
			log.info("{} IcsBackupJob.Worker::run()", Utility.indentStart());
			long started = System.currentTimeMillis();

			//	calendar.ics
			String text = icsService.downloadIcs(1028);
			String filename = String.format("%s/calendar.ics", job.dataPath);
			Utility.write(filename, text);
			log.debug("{} IcsBackupJob.Worker::run(『{}』) - 『{}』『{}』", Utility.indentMiddle(), job.dataPath, filename, Utility.ellipsisEscape(text, 32, 32));

			log.info("{} {} IcsBackupJob.Worker::run() - {}", Utility.indentEnd(), STATUS.SUCCESS, Utility.toStringPastTimeReadable(started));
			return STATUS.SUCCESS;
		}

	}

}
