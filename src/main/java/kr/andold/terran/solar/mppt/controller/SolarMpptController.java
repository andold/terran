package kr.andold.terran.solar.mppt.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import kr.andold.terran.service.JobService;
import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
import kr.andold.terran.solar.mppt.interfaces.CrudController;
import kr.andold.terran.solar.mppt.param.SolarMpptParam;
import kr.andold.terran.solar.mppt.service.SolarMpptBackupJob;
import kr.andold.terran.solar.mppt.service.SolarMpptCrawlDetailsJob;
import kr.andold.terran.solar.mppt.service.SolarMpptCrawlLogsJob;
import kr.andold.terran.solar.mppt.service.SolarMpptService;
import kr.andold.terran.solar.mppt.service.SolarMpptTsdbAggregateJob;
import kr.andold.terran.solar.mppt.service.SolarMpptTsdbPutJob;
import kr.andold.terran.solar.mppt.service.Utility;
import kr.andold.utils.persist.CrudList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("solar-mppt")
public class SolarMpptController implements CrudController<SolarMpptParam, SolarMpptDomain> {
	@Autowired private HttpServletResponse httpServletResponse;
	@Autowired private SolarMpptService service;

	@PostMapping(value = {"aggregate"})
	public void aggregate(@RequestBody SolarMpptParam param) {
		log.info("{} aggregate({})", Utility.indentStart(), param);

		SolarMpptTsdbAggregateJob job = SolarMpptTsdbAggregateJob.builder()
				.start(LocalDateTime.ofInstant(param.getStart().toInstant(), Utility.ZONE_ID_KST))
				.end(LocalDateTime.ofInstant(param.getEnd().toInstant(), Utility.ZONE_ID_KST))
				.build();
		JobService.getQueue0().add(job);

		log.info("{} aggregate({})", Utility.indentEnd(), param);
	}
	
	@ResponseBody
	@GetMapping(value = {"test"})
	public String test(@RequestParam Integer command) {
		log.info("{} test({})", Utility.indentStart(), command);

		String result = "";
		if (command == null)  {
			log.info("{} 『NULL:{}』 test({})", result, command, Utility.indentEnd());
			return result;
		}

		switch (command) {
		case 0:
//			JobService.getQueue2().add(SolarMpptCrawlDetailsJob.RestartDriverJob.builder().build());
			SolarMpptCrawlDetailsJob.RestartDriverJob.regist(JobService.getQueue2());
			result = "SUCCESS";
			break;
		case 1:
			JobService.getQueue1().add(SolarMpptTsdbAggregateJob.builder()
					.start(LocalDateTime.now().minusMonths(1))
					.end(LocalDateTime.now())
					.build());
			result = "SUCCESS";
			break;
		default:
			result = "NOT SUPPORT";
			break;
		}

		log.info("{} 『{}』 test({})", result, command, Utility.indentEnd());
		return result;
	}
	
	@ResponseBody
	@PostMapping(value = {"crawl"})
	public SolarMpptParam craw(@RequestBody SolarMpptParam param) {
		log.info("{} crawl({})", Utility.indentStart(), param);

		SolarMpptParam result = service.crawl(param);
		JobService.getQueue0().add(SolarMpptCrawlLogsJob.builder().start(ZonedDateTime.ofInstant(param.getStart().toInstant(), Utility.ZONE_ID_KST)).build());

		log.info("{} {} - crawl({})", Utility.indentEnd(), result, param);
		return result;
	}
	
	@PostMapping(value = {"crawl/detail"})
	public void crawDetail() {
		log.info("{} crawDetail()", Utility.indentStart());

		JobService.getQueue0().add(new SolarMpptCrawlDetailsJob());

		log.info("{} crawDetail()", Utility.indentEnd());
	}
	@PostMapping(value = {"crawl/clean"})
	public void crawClean() {
		log.info("{} crawClean()", Utility.indentStart());

//		JobService.getQueue0().add(SolarMpptCrawlDetailsJob.RestartDriverJob.builder().build());
		SolarMpptCrawlDetailsJob.RestartDriverJob.regist(JobService.getQueue0());

		log.info("{} crawClean()", Utility.indentEnd());
	}
	
	@ResponseBody
	@GetMapping(value = {"backup"})
	public void backup() {
		log.info("{} backup()", Utility.indentStart());

		SolarMpptBackupJob solarMpptBackupJob = SolarMpptBackupJob.builder().build();
		JobService.getQueue1().add(solarMpptBackupJob);

		log.info("{} backup()", Utility.indentEnd());
	}
	
	@Override
	public SolarMpptParam search(SolarMpptParam param) {
		log.info("{} search({})", Utility.indentStart(), param);
		long started = System.currentTimeMillis();

		SolarMpptParam result = service.searchWithPageable(param);

		log.info("{} 『{}』 - search({}) - {}", Utility.indentEnd(), result, param, Utility.toStringPastTimeReadable(started));
		return result;
	}

	@Override
	public SolarMpptParam update(Integer id, SolarMpptDomain domain) {
		return service.update(domain);
	}

	@Override
	public String download() {
		log.info("{} download()", Utility.indentStart());

		String response = "";
		try {
			String filename = URLEncoder.encode(String.format("solar-mppt-%s.json", LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)), "UTF-8").replaceAll("\\+", "%20");
			httpServletResponse.setHeader("Content-Disposition", "attachment; filename=" + filename);

			response = service.download();
		} catch (UnsupportedEncodingException e) {
			log.error("UnsupportedEncodingException:: {}", e.getMessage(), e);
			response = e.getMessage();
		}

		log.info("{} {} - download()", Utility.indentEnd(), Utility.ellipsisEscape(response, 32, 32));
		return response;
	}

	@Override
	public CrudList<SolarMpptDomain> upload(MultipartFile file) {
		log.info("{} upload(#{})", Utility.indentStart(), Utility.size(file));

		CrudList<SolarMpptDomain> result = service.upload(file);
		JobService.getQueue1().add(SolarMpptTsdbPutJob.builder().crud(result).build());

		log.info("{} upload(#{})", Utility.indentEnd(), Utility.size(file));
		return result;
	}

}
