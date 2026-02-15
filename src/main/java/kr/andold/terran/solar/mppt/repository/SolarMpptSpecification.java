package kr.andold.terran.solar.mppt.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import kr.andold.terran.solar.mppt.entity.SolarMpptEntity;
import kr.andold.terran.solar.mppt.param.SolarMpptParam;

public class SolarMpptSpecification {

	public static Specification<SolarMpptEntity> searchWith(SolarMpptParam param) {
		return ((root, query, builder) -> {
	        List<Predicate> predicates = new ArrayList<>();
	        if (param.getStart() != null) {
	            predicates.add(builder.greaterThanOrEqualTo(root.get("base"), param.getStart()));
	        }
	        if (param.getEnd() != null) {
	            predicates.add(builder.lessThan(root.get("base"), param.getEnd()));
	        }

	        return builder.and(predicates.toArray(new Predicate[0]));
		});
	}

}
