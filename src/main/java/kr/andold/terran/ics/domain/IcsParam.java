package kr.andold.terran.ics.domain;

import java.util.List;

import kr.andold.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IcsParam extends IcsComponentDomain {
	// calendar
	private String title;
	private String description;

	// component
	private String keyword;

	private List<IcsComponentDomain> creates;
	private List<IcsComponentDomain> duplicates;
	private List<IcsComponentDomain> updates;
	private List<IcsComponentDomain> removes;

	@Override
	public String toString() {
		return String.format("ContactParam(vcalendarId: %d, keyword: %s, creates: #%d, duplicates: #%d, updates: #%d, removes: #%d, %s)", getVcalendarId(),
			getKeyword(), Utility.size(creates), Utility.size(duplicates), Utility.size(updates), Utility.size(removes), super.toString());
	}

	public String toString(int size) {
		return Utility.ellipsis(toString(),  size);
	}

	public IcsParam prepareForSearch() {
		if (getKeyword() == null) {
			setKeyword("");
		}
		if (getStart() == null) {
			setStart(DEFAULT_START);
		}
		if (getEnd() == null) {
			setEnd(DEFAULT_END);
		}

		return this;
	}

}
