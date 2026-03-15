package kr.andold.terran.param;

import java.util.List;

import kr.andold.terran.domain.ContactDomain;
import kr.andold.utils.Utility;
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
public class ContactParam extends ContactDomain {
	private String keyword;

	private List<ContactDomain> creates;
	private List<ContactDomain> duplicates;
	private List<ContactDomain> updates;
	private List<ContactDomain> removes;

	@Override
	public String toString() {
		return String.format("ContactParam(creates: #%d, duplicates: #%d, updates: #%d, removes: #%d)", Utility.size(creates), Utility.size(duplicates),
			Utility.size(updates), Utility.size(removes));
	}
}
