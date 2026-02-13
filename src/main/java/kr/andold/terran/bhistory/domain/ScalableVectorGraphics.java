package kr.andold.terran.bhistory.domain;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class ScalableVectorGraphics {
	@Getter @Setter private Integer width;
	@Getter @Setter private Integer height;

	@Getter @Setter private List<ScalableVectorGraphicsEvent> listEvent;
	@Getter @Setter private List<ScalableVectorGraphicsTimeLine> listTimeLine;

}
