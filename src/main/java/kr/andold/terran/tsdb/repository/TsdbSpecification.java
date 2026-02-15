package kr.andold.terran.tsdb.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import kr.andold.terran.tsdb.entity.TsdbEntity;
import kr.andold.terran.tsdb.param.TsdbParam;

public class TsdbSpecification {
	public static Specification<TsdbEntity> searchWith(TsdbParam param) {
		return ((root, query, builder) -> {
	        List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("group"), param.getGroup()));
	        if (param.getBase() != null) {
	            predicates.add(builder.equal(root.get("base"), param.getBase()));
	        }
	        if (param.getStart() != null) {
	            predicates.add(builder.greaterThanOrEqualTo(root.get("base"), param.getStart()));
	        }
	        if (param.getEnd() != null) {
	        	if (param.getIncludeEnd()) {
		            predicates.add(builder.lessThanOrEqualTo(root.get("base"), param.getEnd()));
	        	} else {
		            predicates.add(builder.lessThan(root.get("base"), param.getEnd()));
	        	}
	        }

	        return builder.and(predicates.toArray(new Predicate[0]));
		});
	}

}
