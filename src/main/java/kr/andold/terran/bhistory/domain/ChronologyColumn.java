package kr.andold.terran.bhistory.domain;

import kr.andold.terran.bhistory.entity.BigHistoryEntity;
import lombok.Getter;
import lombok.Setter;

public class ChronologyColumn {
	@Getter
	@Setter
	private Integer rowspan;

	@Getter
	@Setter
	private Integer colspan;

	@Getter
	@Setter
	private BigHistoryEntity bigHistory;

}
