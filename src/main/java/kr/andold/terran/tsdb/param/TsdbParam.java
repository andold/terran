package kr.andold.terran.tsdb.param;

import java.util.Date;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import kr.andold.terran.tsdb.domain.TsdbDomain;
import kr.andold.utils.persist.CrudList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TsdbParam extends TsdbDomain implements Pageable {
	@Getter @Setter private Date start;
	@Getter @Setter private Date end;
	@Builder.Default @Getter @Setter private Boolean includeEnd = false;

	@Getter @Setter private CrudList<TsdbDomain> crud;
	@Builder.Default private PageRequest page = PageRequest.of(0, 1024 * 1024, Sort.by(Order.asc("base")));

	@Override
	public int getPageNumber() {
		return page.getPageNumber();
	}
	@Override
	public int getPageSize() {
		return page.getPageSize();
	}
	@Override
	public long getOffset() {
		return page.getOffset();
	}
	@Override
	public Sort getSort() {
		return page.getSort();
	}
	@Override
	public Pageable next() {
		return page.next();
	}
	@Override
	public Pageable previousOrFirst() {
		return page.previousOrFirst();
	}
	@Override
	public Pageable first() {
		return page.first();
	}
	@Override
	public Pageable withPage(int pageNumber) {
		return page.withPage(pageNumber);
	}
	@Override
	public boolean hasPrevious() {
		return page.hasPrevious();
	}

	@Override
	public String toString() {
		return String.format("{%tF %<tR, %tF %<tR, %s, %s}", start, end, crud, super.toString());
	}

}
