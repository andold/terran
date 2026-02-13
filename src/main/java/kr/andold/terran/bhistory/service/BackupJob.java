package kr.andold.terran.bhistory.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Service
public class BackupJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 300L;	//	TimeUnit.SECONDS
	@Getter @Setter private String userDataPath;

	@Autowired private BigHistoryService service;

	@Override
	public STATUS call() throws Exception {
		log.info("{} BackupJob::call() - 『{}』『{}』", Utility.indentStart(), userDataPath, timeout);
		long started = System.currentTimeMillis();

		BackupJob that = (BackupJob) ApplicationContextProvider.getBean(BackupJob.class);
		that.setUserDataPath(getUserDataPath());
		STATUS result = that.main();
		
		log.info("{} 『#{}』 BackupJob::call() - 『{}』『{}』『{}』", Utility.indentEnd(), result, userDataPath, timeout, Utility.toStringPastTimeReadable(started));
		return result;
	}

	private STATUS main() throws Exception {
		log.info("{} BackupJob::main() - 『{}』『{}』", Utility.indentStart(), userDataPath, timeout);

		List<BigHistoryEntity> histories = service.search(null);
		String text = Utility.toStringJsonLine(histories);
		String filename = String.format("%s/bhistory.json", userDataPath);
		Utility.write(filename, text);

		log.info("{} 『{}』『{}』 BackupJob::main() - 『{}』『{}』", Utility.indentStart(), filename, Utility.ellipsis(text, 32, 32), userDataPath, timeout);
		return STATUS.SUCCESS;
	}

}
