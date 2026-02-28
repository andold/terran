package kr.andold.terran.bookmark.domain;

import kr.andold.terran.bookmark.entity.Bookmark;
import kr.andold.utils.Utility;
import lombok.Getter;
import lombok.Setter;

public class BookmarkParameter extends Bookmark {
	@Getter @Setter private Boolean force;
	@Getter @Setter private Boolean expand;	// children
	@Getter @Setter private String keyword;

	@Override
	public String toString() {
		return Utility.toStringJson(this);
	}
	
}
