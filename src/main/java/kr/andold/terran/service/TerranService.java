package kr.andold.terran.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import kr.andold.terran.solar.mppt.service.Utility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TerranService {
	@Getter private static String applicationDataPath;
	@Value("${application.data.path}")
	public void setApplicationDataPath(String value) {
		log.info("{} setApplicationDataPath(『{}』)", Utility.indentMiddle(), value);
		applicationDataPath = value;
	}

}
