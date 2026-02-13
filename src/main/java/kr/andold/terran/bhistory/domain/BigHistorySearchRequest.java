package kr.andold.terran.bhistory.domain;

import kr.andold.terran.bhistory.entity.BigHistoryEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BigHistorySearchRequest extends BigHistoryEntity {
	private String keyword;
	private Integer rem;
	private Integer width;
	private Integer height;
	private Long positionId;
	private Boolean expandChildren;

}
