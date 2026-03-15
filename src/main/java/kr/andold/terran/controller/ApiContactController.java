package kr.andold.terran.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import kr.andold.terran.domain.ContactDomain;
import kr.andold.terran.domain.ContactMapDomain;
import kr.andold.terran.param.ContactParam;
import kr.andold.terran.service.ContactMapService;
import kr.andold.terran.service.ContactService;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("contact")
public class ApiContactController {
	@Autowired private ContactService service;
	@Autowired private ContactMapService mapService;

	@GetMapping(value = {""})
	public List<ContactDomain> index() {
		log.info("{} index({})", Utility.indentStart());

		List<ContactDomain> list = service.search(null);

		log.info("{} #{} - index({})", Utility.indentEnd(), Utility.size(list));
		return list;
	}

	@PostMapping(value = {"search"})
	public List<ContactDomain> search(@RequestBody ContactParam param) {
		log.info("{} search({})", Utility.indentStart(), param);

		List<ContactDomain> list = service.search(param);

		log.info("{} #{} - search({})", Utility.indentEnd(), Utility.size(list), param);
		return list;
	}

	@PostMapping(value = {"batch"})
	public int batch(@RequestBody ContactParam param) {
		log.info("{} batch(...)", Utility.indentStart());
		long started = System.currentTimeMillis();

		int result = service.batch(param);

		log.info("{} {} - batch(...) - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return result;
	}

	@PutMapping(value = {"{id}"})
	public ContactDomain update(@PathVariable Integer id, @RequestBody ContactDomain param) {
		log.info("{} update({}, {})", Utility.indentStart(), id, param);
		long started = System.currentTimeMillis();

		ContactDomain updated = service.update(id, param);

		log.info("{} {} - update({}, {}) - {}", Utility.indentEnd(), updated, id, param, Utility.toStringPastTimeReadable(started));
		return updated;
	}

	@PostMapping(value = "upload")
	public ContactParam upload(@RequestPart(value = "file") MultipartFile file) throws UnsupportedEncodingException, IOException {
		log.info("{} upload(#{})", Utility.indentStart(), size(file));

		ContactParam result = service.upload(file);

		log.info("{} upload(#{})", Utility.indentEnd(), size(file));
		return result;
	}

	private int size(MultipartFile file) {
		return (int)((file == null) ? -1 : file.getSize());
	}

	@GetMapping(value = {"download"})
	public String downloadJson(HttpServletResponse httpServletResponse) throws UnsupportedEncodingException {
		log.info("{} downloadJson()", Utility.indentStart());

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
		String filename = URLEncoder.encode(String.format("andold-contact-%s.json", simpleDateFormat.format(Calendar.getInstance().getTime())), "UTF-8").replaceAll("\\+", "%20");
		httpServletResponse.setHeader("Content-Disposition", "attachment; filename=" + filename);

		String response = service.download();

		log.info("{} {} - downloadJson()", Utility.indentEnd(), Utility.ellipsisEscape(response, 32, 32));
		return response;
	}
	
	@GetMapping(value = {"download-vcard/{priority}"})
	public String downloadVCard(@PathVariable Integer priority, HttpServletResponse httpServletResponse) throws UnsupportedEncodingException {
		log.info("{} downloadVCard({})", Utility.indentStart(), priority);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
		String filename = URLEncoder.encode(String.format("andold-contact-%s.vcf", simpleDateFormat.format(Calendar.getInstance().getTime())), "UTF-8").replaceAll("\\+", "%20");
		httpServletResponse.setHeader("Content-Disposition", "attachment; filename=" + filename);

		String response = service.downloadVCard(priority);

		log.info("{} {} - downloadVCard({})", Utility.indentEnd(), Utility.ellipsisEscape(response, 32), priority);
		return response;
	}
	
	@PutMapping(value = {"map/{id}"})
	public ContactMapDomain updateMap(@PathVariable Integer id, @RequestBody ContactMapDomain param) {
		log.info("{} updateMap({}, {})", Utility.indentStart(), id, param);
		long started = System.currentTimeMillis();

		ContactMapDomain updated = mapService.update(id, param);

		log.info("{} {} - updateMap({}, {}) - {}", Utility.indentEnd(), updated, id, param, Utility.toStringPastTimeReadable(started));
		return updated;
	}

}
