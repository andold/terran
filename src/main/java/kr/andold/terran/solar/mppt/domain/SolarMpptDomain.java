package kr.andold.terran.solar.mppt.domain;

import kr.andold.terran.solar.mppt.entity.SolarMpptEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SolarMpptDomain extends SolarMpptEntity {
	@Getter @Setter
	private String dummy;

}
