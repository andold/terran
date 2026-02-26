package kr.andold.terran.ics.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import kr.andold.terran.ics.domain.IcsCalendarDomain;
import kr.andold.terran.ics.domain.IcsComponentDomain;
import kr.andold.terran.ics.domain.IcsParam;
import kr.andold.terran.ics.service.IcsBackupJob;
import kr.andold.terran.ics.service.IcsService;
import kr.andold.terran.service.JobService;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("ics")
public class ApiIcsController {
	@Autowired private IcsService service;

	@PostMapping(value = {"calendar"})
	public IcsCalendarDomain createCalendar(@RequestBody IcsCalendarDomain param) {
		log.info("{} createCalendar({})", Utility.indentStart(), param);
		IcsCalendarDomain created = service.createCalendar(param);
		log.info("{} {} - createCalendar({})", Utility.indentEnd(), created, param);
		return created;
	}

	@PostMapping(value = {"calendar/search"})
	public List<IcsCalendarDomain> searchCalendar(@RequestBody IcsParam param) {
		log.info("{} searchCalendar({})", Utility.indentStart(), param);

		List<IcsCalendarDomain> list = service.searchCalendar(param);

		log.info("{} #{} - searchCalendar({})", Utility.indentEnd(), Utility.size(list), param);
		return list;
	}

	@PostMapping(value = "{vcalendarId}/upload")
	public IcsParam upload(@RequestPart MultipartFile file, @PathVariable(required = false) Integer vcalendarId) {
		log.info("{} upload(...)", Utility.indentStart());

		IcsParam result = service.upload(file, vcalendarId);

		log.info("{} upload(...)", Utility.indentEnd());
		return result;
	}

	@GetMapping(value = "{vcalendarId}/download-ics")
	public String downloadIcs(@PathVariable Integer vcalendarId, HttpServletResponse httpServletResponse) throws UnsupportedEncodingException {
		log.info("{} downloadIcs({})", Utility.indentStart(), vcalendarId);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
		String filename = URLEncoder.encode(String.format("calendar-%s.ics", simpleDateFormat.format(Calendar.getInstance().getTime())), "UTF-8").replaceAll("\\+", "%20");
		httpServletResponse.setHeader("Content-Disposition", "attachment; filename=" + filename);

		String response = service.downloadIcs(vcalendarId);

		log.info("{} 『{}』 - downloadIcs({})", Utility.indentEnd(), Utility.ellipsisEscape(response, 32), vcalendarId);
		return response;
	}

	@PostMapping(value = "{vcalendarId}/deduplicate")
	public IcsParam deduplicate(@PathVariable Integer vcalendarId) {
		log.info("{} deduplicate({})", Utility.indentStart(), vcalendarId);

		IcsParam result = service.deduplicate(vcalendarId);

		log.info("{} {} deduplicate({})", Utility.indentEnd(), Utility.toStringJson(result, 32), vcalendarId);
		return result;
	}

	@PostMapping(value = {"batch"})
	public int batch(@RequestBody IcsParam param) throws ParseException {
		log.info("{} batch(...)", Utility.indentStart());
		long started = System.currentTimeMillis();

		int result = service.batch(param);

		log.info("{} {} - batch(...) - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return result;
	}

	@PostMapping(value = {"search"})
	public List<IcsComponentDomain> search(@RequestBody IcsParam param) {
		log.info("{} search({})", Utility.indentStart(), param.toString(64));

		List<IcsComponentDomain> list = service.search(param);

		log.info("{} #{} - search({})", Utility.indentEnd(), Utility.size(list), param.toString(64));
		return list;
	}

	@DeleteMapping(value = {"{id}"})
	public IcsComponentDomain remove(@PathVariable Integer id) {
		log.info("{} delete({})", Utility.indentStart(), id);
		long started = System.currentTimeMillis();

		IcsComponentDomain removed = service.remove(id);

		log.info("{} #{} - delete({}) - {}", Utility.indentEnd(), removed, id, Utility.toStringPastTimeReadable(started));
		return removed;
	}

	@GetMapping(value = "backup")
	public void backup() {
		log.info("{} backup()", Utility.indentStart());

		JobService.getQueue1().offer(IcsBackupJob.builder().dataPath(IcsService.getUserDataPath()).build());

		log.info("{} backup()", Utility.indentEnd());
	}

}
