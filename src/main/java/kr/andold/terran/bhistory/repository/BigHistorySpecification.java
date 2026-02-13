package kr.andold.terran.bhistory.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import kr.andold.terran.bhistory.domain.BigHistorySearchRequest;
import kr.andold.terran.bhistory.entity.BigHistoryEntity;

public class BigHistorySpecification {

	public static Specification<BigHistoryEntity> searchWith(BigHistorySearchRequest form) {
		return ((root, query, builder) -> {
	        List<Predicate> predicates = new ArrayList<>();
	        if (StringUtils.hasText(form.getTitle())) {
	            predicates.add(builder.like(root.get("title"), "%" + form.getTitle() + "%"));
	        }
	        if (StringUtils.hasText(form.getKeyword())) {
	            predicates.add(builder.like(root.get("keyword"), "%" + form.getKeyword() + "%"));
	        }
	        if (form.getStart() != null) {
	            predicates.add(builder.greaterThanOrEqualTo(root.get("end"), form.getStart()));
	        }
	        if (form.getEnd() != null) {
	            predicates.add(builder.lessThanOrEqualTo(root.get("start"), form.getEnd()));
	        }
	        return builder.and(predicates.toArray(new Predicate[0]));
	    });
	}

}
