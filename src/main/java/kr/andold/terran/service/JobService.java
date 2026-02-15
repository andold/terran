package kr.andold.terran.service;

import org.springframework.stereotype.Service;

import kr.andold.terran.solar.mppt.service.SolarMpptCrawlDetailsJob;
import kr.andold.utils.Utility;
import kr.andold.utils.job.STATUS;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JobService extends kr.andold.utils.job.JobService {
	long STARTED = System.currentTimeMillis();

	@Override
	public STATUS run() {
		log.trace("{} run()", Utility.indentStart());

		STATUS result = super.run();

		switch (result) {
		case FAIL_TIMEOUT_EXCEPTION:
//			JobService.getQueue3().add(SolarMpptCrawlDetailsJob.RestartDriverJob.builder().build());
			SolarMpptCrawlDetailsJob.RestartDriverJob.regist(JobService.getQueue3());
			log.info("{} 『{}』 run()", Utility.indentMiddle(), result);
			break;
		case ALEADY_DONE:
			break;
		default:
			log.info("{} 『{}』 run()", Utility.indentMiddle(), result);
			break;
		}

		log.trace("{} 『{}』 run()", Utility.indentEnd(), result);
		return result;
	}

}
