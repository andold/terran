package kr.andold.terran.bhistory.domain;

import java.util.List;

import kr.andold.terran.bhistory.entity.BigHistoryEntity;
import lombok.Getter;
import lombok.Setter;

public class ChronologyRow {
	@Getter
	@Setter
	private Double title;

	@Getter
	@Setter
	private List<ChronologyColumn> columns;

	public String getTitleHtml() {		return BigHistoryEntity.toHtmlTime(getTitle());	}

}
