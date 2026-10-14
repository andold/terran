package kr.andold.terran.solar.mppt.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.andold.terran.ApplicationContextProvider;
import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
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
public class SolarMpptBackupJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 60L;

	@Override
	public STATUS call() throws Exception {
		log.info("{} SolarMpptBackupJob::call()", Utility.indentStart());
		long started = System.currentTimeMillis();

		Worker worker = (Worker) ApplicationContextProvider.getBean(Worker.class);
		int result = worker.run(this);

		log.info("{} 『{}:#{}』 SolarMpptBackupJob::call() - {}", Utility.indentEnd(), STATUS.SUCCESS, result, Utility.toStringPastTimeReadable(started));
		return STATUS.SUCCESS;
	}

	@RequiredArgsConstructor
	@Service
	public static class Worker {
		private final SolarMpptService service;

		protected int run(SolarMpptBackupJob job) {
			log.info("{} SolarMpptBackupJob.Worker::run()", Utility.indentStart());
			long started = System.currentTimeMillis();

			String fullpath = SolarMpptService.getApplicationDataPath();
			List<SolarMpptDomain> domains = service.search(null);
			String json = Utility.toStringJsonLine(domains);
			Utility.write(String.format("%s/solar-mppt.json", fullpath), json);
			int result = Utility.size(domains);

			log.info("{} 『{}』 SolarMpptBackupJob.Worker::run() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
			return result;
		}

	}

}
