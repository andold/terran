package kr.andold.terran.solar.mppt.service;

import org.springframework.web.multipart.MultipartFile;

public class Utility extends kr.andold.utils.Utility {
	public static int size(MultipartFile file) {
		try {
			return file.getBytes().length;
		} catch (Exception e) {
		}

		return -1;
	}

}
