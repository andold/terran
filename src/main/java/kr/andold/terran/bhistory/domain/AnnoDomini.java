package kr.andold.terran.bhistory.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnnoDomini {
	private Long year;
	private Long month;
	private Long day;

	private Long hour;
	private Long minute;
	private Long second;

	private Long offset;
	
}
