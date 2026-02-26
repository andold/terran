package kr.andold.terran.ics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.andold.terran.ApplicationContextProvider;
import kr.andold.utils.Utility;
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
public class BackupJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 60L;
	@Setter private String dataPath;

	@Autowired private IcsService icsService;

	@Override
	public STATUS call() throws Exception {
		log.info("{} BackupJob::call()", Utility.indentStart());
		long started = System.currentTimeMillis();

		BackupJob that = (BackupJob) ApplicationContextProvider.getBean(BackupJob.class);
		that.setDataPath(dataPath);
		STATUS result = that.main();

		log.info("{} {} BackupJob::call() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return result;
	}

	protected STATUS main() {
		log.info("{} BackupJob::main()", Utility.indentStart());
		long started = System.currentTimeMillis();

		//	calendar.ics
		String text = icsService.downloadIcs(1028);
		String filename = String.format("%s/calendar.ics", dataPath);
		Utility.write(filename, text);
		log.debug("{} BackupJob::main(『{}』) - 『{}』『{}』", Utility.indentMiddle(), dataPath, filename, Utility.ellipsisEscape(text, 32, 32));
		
		log.info("{} {} BackupJob::main() - {}", Utility.indentEnd(), STATUS.SUCCESS, Utility.toStringPastTimeReadable(started));
		return STATUS.SUCCESS;
	}

}