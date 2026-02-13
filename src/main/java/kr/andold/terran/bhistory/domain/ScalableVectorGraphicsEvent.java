package kr.andold.terran.bhistory.domain;

import kr.andold.terran.bhistory.entity.BigHistoryEntity;
import lombok.Getter;
import lombok.Setter;

public class ScalableVectorGraphicsEvent {
	@Getter @Setter private Integer x;
	@Getter @Setter private Integer y;
	@Getter @Setter private Integer dx;
	@Getter @Setter private Integer dy;
	@Getter @Setter private Integer duration;
	@Getter @Setter private Boolean vertical;
	@Getter @Setter private String textPath;

	@Getter
	@Setter
	private BigHistoryEntity bigHistory;

}
