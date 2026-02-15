package kr.andold.terran.tsdb.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import kr.andold.terran.solar.mppt.interfaces.CrudController;
import kr.andold.terran.solar.mppt.service.Utility;
import kr.andold.terran.tsdb.domain.TsdbDomain;
import kr.andold.terran.tsdb.param.TsdbParam;
import kr.andold.terran.tsdb.service.TsdbService;
import kr.andold.utils.persist.CrudList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("tsdb")
public class TsdbController implements CrudController<TsdbParam, TsdbDomain> {
	@Autowired private HttpServletResponse httpServletResponse;
	@Autowired private TsdbService service;

	@Override
	public TsdbParam search(TsdbParam param) {
		log.info("{} search({})", Utility.indentStart(), param);
		long started = System.currentTimeMillis();

		TsdbParam result = service.searchWithPageable(param);

		log.info("{} 『{}』 - search({}) - {}", Utility.indentEnd(), result, param, Utility.toStringPastTimeReadable(started));
		return result;
	}

	@Override
	public TsdbParam update(Integer id, TsdbDomain domain) {
		log.info("{} update({}, {})", Utility.indentStart(), id, domain);
		long started = System.currentTimeMillis();

		TsdbParam result = service.update(domain);

		log.info("{} 『{}』 - update({}, {}) - {}", Utility.indentEnd(), result, id, domain, Utility.toStringPastTimeReadable(started));
		return result;
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
	public CrudList<TsdbDomain> upload(MultipartFile file) {
		log.info("{} upload(#{})", Utility.indentStart(), Utility.size(file));

		CrudList<TsdbDomain> result = service.upload(file);

		log.info("{} upload(#{})", Utility.indentEnd(), Utility.size(file));
		return result;
	}

}
