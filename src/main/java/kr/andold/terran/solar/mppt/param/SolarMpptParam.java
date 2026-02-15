package kr.andold.terran.solar.mppt.param;

import java.util.Date;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
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
public class SolarMpptParam extends SolarMpptDomain implements Pageable {
	@Getter @Setter private Date start;
	@Getter @Setter private Date end;

	@Getter @Setter private CrudList<SolarMpptDomain> crud;
	@Builder.Default private PageRequest page = PageRequest.of(0, 1024 * 1024, Sort.by(Order.asc("base")));

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
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		if (start != null) {
			sb.append(String.format("start: %tF %<tR %<tZ, ", start));
		}
		if (end != null) {
			sb.append(String.format("end: %tF %<tR %<tZ, ", end));
		}
		if (crud != null) {
			sb.append(String.format("crud: %s, ", crud.toString()));
		}
		sb.append(String.format("Page(%s)", page.toString()));
		sb.append("}");
		return sb.toString();
	}

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
}
