package kr.andold.terran.bookmark.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import kr.andold.terran.bookmark.domain.BookmarkParam;
import kr.andold.terran.bookmark.domain.BookmarkParam.BookmarkDifferResult;
import kr.andold.terran.bookmark.domain.BookmarkParameter;
import kr.andold.terran.bookmark.entity.Bookmark;
import kr.andold.terran.bookmark.service.BookmarkService;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("bookmark")
public class ApiBookmarkController {
	@Autowired private BookmarkService service;

	@PostMapping(value = {"search"})
	public List<Bookmark> search(@RequestBody BookmarkParameter parameter) {
		log.info("{} search({})", Utility.indentStart(), parameter);
		long started = System.currentTimeMillis();

		List<Bookmark> list = service.search(parameter);

		log.info("{} {} - search({}) - {}", Utility.indentEnd(), Utility.size(list), parameter, Utility.toStringPastTimeReadable(started));
		return list;
	}

	@PostMapping(value = {""})
	public Bookmark create(@RequestBody Bookmark bookmark) {
		log.info("{} create({})", Utility.indentStart(), bookmark);
		long started = System.currentTimeMillis();

		Bookmark created = service.create(bookmark);

		log.info("{} {} - create({}) - {}", Utility.indentEnd(), created, bookmark, Utility.toStringPastTimeReadable(started));
		return created;
	}

	@PostMapping(value = {"batch"})
	public int batch(@RequestBody BookmarkDifferResult param) {
		log.info("{} batch(...)", Utility.indentStart());
		long started = System.currentTimeMillis();

		int removed = service.batch(param);

		log.info("{} {} - batch(...) - {}", Utility.indentEnd(), removed, Utility.toStringPastTimeReadable(started));
		return removed;
	}

	@GetMapping(value = {"{id}"})
	public Bookmark read(@PathVariable Integer id) {
		log.info("{} read({})", Utility.indentStart(), id);
		long started = System.currentTimeMillis();

		Bookmark bookmark = null;
		if (id == null || id.intValue() < 0) {
			bookmark = service.root();
		} else {
			bookmark = service.read(id);
		}

		log.info("{} {} - read({}) - {}", Utility.indentEnd(), bookmark, id, Utility.toStringPastTimeReadable(started));
		return bookmark;
	}

	@PutMapping(value = {"{id}"})
	public Bookmark update(@PathVariable Integer id, @RequestBody BookmarkParameter bookmark) {
		log.info("{} update({}, {})", Utility.indentStart(), id, bookmark);

		Bookmark after = service.update(id, bookmark, bookmark.getForce());

		log.info("{} {} - update({}, {})", Utility.indentEnd(), after, id, bookmark);
		return after;
	}

	@PutMapping(value = {"{id}/count"})
	public Bookmark updateCountIncrease(@PathVariable Integer id) {
		log.info("{} updateCountIncrease({})", Utility.indentStart(), id);

		Bookmark after = service.updateCountIncrease(id);

		log.info("{} {} - updateCountIncrease({})", Utility.indentEnd(), after, id);
		return after;
	}

	@DeleteMapping(value = {"{id}"})
	public boolean delete(@PathVariable Integer id) {
		log.info("{} delete({})", Utility.indentStart(), id);

		boolean result = service.delete(id);

		log.info("{} #{} - delete({})", Utility.indentEnd(), result, id);
		return result;
	}

	@GetMapping(value = {"sample"})
	public Bookmark sample() {
		log.info("{} sample()", Utility.indentStart());

		Bookmark sample = Bookmark.sample();

		log.info("{} {} - sample()", Utility.indentEnd(), sample);
		return sample;
	}

	@GetMapping(value = {"/download"})
	@ResponseBody
	public String download(HttpServletResponse httpServletResponse) throws UnsupportedEncodingException {
		log.info("{} download({})", Utility.indentStart());

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
		String filename = URLEncoder.encode(String.format("bookmark-%s.json", simpleDateFormat.format(Calendar.getInstance().getTime())), "UTF-8").replaceAll("\\+", "%20");
		httpServletResponse.setHeader("Content-Disposition", "attachment; filename=" + filename);

		BookmarkParam param = service.download();
		String response = Utility.toStringJsonPretty(param);

		log.info("{} {} - download({})", Utility.indentEnd(), Utility.ellipsisEscape(response, 32, 32));
		return response;
	}

	@PostMapping(value = {"upload"})
	public BookmarkDifferResult upload(@RequestPart(value = "file") MultipartFile file) {
		log.info("{} upload({})", Utility.indentStart(), "MultipartFile file");
		long started = System.currentTimeMillis();

		BookmarkDifferResult result = service.upload(file);

		log.info("{} #{} - upload({}) - {}", Utility.indentEnd(), Utility.toStringJson(result, 32, 32), "MultipartFile file", Utility.toStringPastTimeReadable(started));
		return result;
	}

	@GetMapping(value = {"control/aggregate-count"})
	public long aggreagateCount() {
		log.info("{} aggreagateCount()", Utility.indentStart());
		long started = System.currentTimeMillis();

		long result = service.aggreagateCount();

		log.info("{} #{} - aggreagateCount() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return result;
	}

	@GetMapping(value = {"control/decrease-count-half"})
	public long decreaseCountHalf() {
		log.info("{} decreaseCountHalf()", Utility.indentStart());
		long started = System.currentTimeMillis();

		long result = service.decreaseCountHalf();

		log.info("{} #{} - decreaseCountHalf() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return result;
	}

	@PostMapping(value = {"control/deduplicate"})
	public BookmarkDifferResult deduplicate() {
		log.info("{} deduplicate({})", Utility.indentStart());
		long started = System.currentTimeMillis();

		BookmarkDifferResult result = service.deduplicate();

		log.info("{} #{} - deduplicate({}) - {}", Utility.indentEnd(), Utility.toStringJson(result, 32, 32), Utility.toStringPastTimeReadable(started));
		return result;
	}

}
