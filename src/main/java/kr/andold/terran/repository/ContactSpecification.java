package kr.andold.terran.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import kr.andold.terran.entity.ContactEntity;
import kr.andold.terran.param.ContactParam;

public class ContactSpecification {
	public static Specification<ContactEntity> searchWith(ContactParam form) {
		return ((root, query, builder) -> {
	        List<Predicate> predicates = new ArrayList<>();
	        if (StringUtils.hasText(form.getKeyword())) {
	            predicates.add(builder.or(
	            		builder.like(root.get("fn"), "%" + form.getKeyword() + "%")
	            		, builder.like(root.get("content"), "%" + form.getKeyword() + "%")
        		));
	        }
	        if (form.getValue() != null) {
	            predicates.add(builder.lessThanOrEqualTo(root.get("value"), form.getValue()));
	        }

	        return builder.and(predicates.toArray(new Predicate[0]));
		});
	}

}
