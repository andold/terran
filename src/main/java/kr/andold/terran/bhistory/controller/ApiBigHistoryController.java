package kr.andold.terran.bhistory.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kr.andold.terran.bhistory.domain.BigHistoryCreateRequest;
import kr.andold.terran.bhistory.domain.BigHistoryDateTime;
import kr.andold.terran.bhistory.domain.BigHistorySearchRequest;
import kr.andold.terran.bhistory.entity.BigHistoryEntity;
import kr.andold.terran.bhistory.service.BigHistoryService;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("bhistory")
public class ApiBigHistoryController {
	@Autowired private BigHistoryService service;

	@GetMapping("{id}")
    public @ResponseBody BigHistoryEntity getBigHistory(@PathVariable Integer id, @RequestBody(required=false) BigHistorySearchRequest request) {
		log.info("{} getBigHistory()", Utility.indentStart());
		long started = System.currentTimeMillis();
	
		BigHistoryEntity bigHistory = service.read(id, request);

		log.info("{} #{} - getBigHistory() - {}", Utility.indentEnd(), Utility.toStringJson(bigHistory, 32), Utility.toStringPastTimeReadable(started));
		return bigHistory;
    }

	@PostMapping(value = {""})
	public @ResponseBody BigHistoryEntity bigHistoryCreate(@RequestBody BigHistoryEntity bigHistory) {
		log.info("{} bigHistoryCreate({})", Utility.indentStart(), Utility.toStringJson(bigHistory));
		BigHistoryEntity after = service.create(bigHistory);
		log.info("{} {} - bigHistoryCreate({})", Utility.indentEnd(), after, Utility.toStringJson(bigHistory));
		return after;
	}

	@PutMapping(value = {"{id}"})
	public @ResponseBody BigHistoryEntity bigHistoryUpdate(@PathVariable Integer id, @RequestBody BigHistoryCreateRequest bigHistory) {
		log.info("{} bigHistoryUpdate({})", Utility.indentStart(), Utility.toStringJson(bigHistory));
		BigHistoryEntity updated = service.update(id, bigHistory);
		log.info("{} {} - bigHistoryUpdate({})", Utility.indentEnd(), Utility.toStringJson(updated, 32), Utility.toStringJson(bigHistory, 32));
		return updated;
	}


	@PostMapping(value = {"search"})
	public List<BigHistoryEntity> search(@RequestBody BigHistorySearchRequest request) {
		log.info("{} search({})", Utility.indentStart(), request);

		List<BigHistoryEntity> list = service.search(request);

		log.info("{} #{} - search({})", Utility.indentEnd(), Utility.size(list), request);
		return list;
	}

	@GetMapping(value = {"/download"})
	@ResponseBody
	public String download() {
		log.info("{} download()", Utility.indentStart());

		List<BigHistoryEntity> list = service.search(null);
		String response = Utility.toStringJsonLine(list);

		log.info("{} 『{}』 download()", Utility.indentEnd(), Utility.ellipsis(response, 64));
		return response;
	}

	@PostMapping(value = {"upload"})
	public @ResponseBody Map<String, List<BigHistoryEntity>> bigHistoryUpload(@RequestParam MultipartFile file, String content) {
		log.info("{} bigHistoryUpload({}, {})", Utility.indentStart(), "...", Utility.toStringJson(content, 64));
		long started = System.currentTimeMillis();

		Map<String, List<BigHistoryEntity>> map = service.upload(file, content);

		log.info("{} {} bigHistoryUpload({}, {}) - {}", Utility.indentEnd(), Utility.size(map), "...", Utility.toStringJson(content, 64), Utility.toStringPastTimeReadable(started));
		return map;
	}

	@DeleteMapping(value = {"{id}"})
	public @ResponseBody BigHistoryEntity bigHistoryDelete(@PathVariable Integer id) {
		log.info("{} bigHistoryDelete({})", Utility.indentStart(), id);
		BigHistoryEntity deleted = service.delete(id);
		log.info("{} {} - bigHistoryDelete({})", Utility.indentEnd(), deleted, id);
		return deleted;
	}

	@GetMapping("parse-date-time")
    public @ResponseBody BigHistoryDateTime parseDateTime(String text) {
		log.info("{} parseDateTime({})", Utility.indentStart(), text);
		long started = System.currentTimeMillis();
	
		BigHistoryDateTime bigHistoryDateTime = BigHistoryDateTime.of(text);

		log.info("{} #{} - parseDateTime({}) - {}", Utility.indentEnd(), Utility.toStringJson(bigHistoryDateTime), text, Utility.toStringPastTimeReadable(started));
		return bigHistoryDateTime;
    }

}
