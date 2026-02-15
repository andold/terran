package kr.andold.terran.solar.mppt.interfaces;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import kr.andold.utils.persist.CrudList;

public interface CrudController<Param, Domain> {
	@ResponseBody
	@PostMapping(value = {"search"})
	Param search(@RequestBody Param param);

	@ResponseBody
	@PutMapping(value = { "{id}" })
	Param update(@PathVariable Integer id, @RequestBody Domain domain);

	@ResponseBody
	@GetMapping(value = {"download"})
	String download();

	@PostMapping(value = "upload")
	CrudList<Domain> upload(@RequestParam MultipartFile file);
	
}
