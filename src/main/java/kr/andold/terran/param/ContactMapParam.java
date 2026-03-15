package kr.andold.terran.param;

import java.util.List;

import kr.andold.terran.domain.ContactMapDomain;
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
public class ContactMapParam extends ContactMapDomain {
	private String keyword;
	private List<Integer> vcardIds;

}
