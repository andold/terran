package kr.andold.terran.bhistory.domain;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class Chronology {
	@Getter
	@Setter
	private Integer colspan;

	@Getter
	@Setter
	private List<ChronologyRow> rows;

}
