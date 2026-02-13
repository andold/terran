package kr.andold.terran.bhistory.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class ScalableVectorGraphicsTimeLine {
	@Getter @Setter private Integer x;
	@Getter @Setter private Integer y;
	@Getter @Setter private Integer dx;
	@Getter @Setter private Integer dy;
	@Getter @Setter private String title;

}
