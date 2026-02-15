package kr.andold.terran.solar.mppt.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import kr.andold.terran.ApplicationContextProvider;
import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
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
public class SolarMpptBackupJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 60L;

	@Autowired private SolarMpptService service;
	
	@Getter private static String userDataPath;
	@Value("${user.data.path:/home/andold/data/test-exercise}")
	public void setUserDataPath(String value) {
		log.info("{} setUserDataPath(『{}』)", Utility.indentMiddle(), value);
		userDataPath = value;
	}

	@Override
	public STATUS call() throws Exception {
		log.info("{} SolarMpptBackupJob::call()", Utility.indentStart());
		long started = System.currentTimeMillis();

		SolarMpptBackupJob that = (SolarMpptBackupJob) ApplicationContextProvider.getBean(SolarMpptBackupJob.class);
		int result = that.main();

		log.info("{} 『{}:#{}』 SolarMpptBackupJob::call() - {}", Utility.indentEnd(), STATUS.SUCCESS, result, Utility.toStringPastTimeReadable(started));
		return STATUS.SUCCESS;
	}

	protected int main() {
		log.info("{} backup()", Utility.indentStart());
		long started = System.currentTimeMillis();

		String fullpath = getUserDataPath();
		List<SolarMpptDomain> domains = service.search(null);
		String json = Utility.toStringJsonLine(domains);
		Utility.write(String.format("%s/solar-mppt.json", fullpath), json);
		int result = Utility.size(domains);

		log.info("{} 『{}』 backup() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return result;
	}

}
