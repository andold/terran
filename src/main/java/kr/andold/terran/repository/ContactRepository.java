package kr.andold.terran.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import kr.andold.terran.entity.ContactEntity;

@Repository
public interface ContactRepository extends JpaRepository<ContactEntity, Integer> {
	List<ContactEntity> findAllByOrderByFnAsc();
	List<ContactEntity> findByContentContainsOrderByFnAsc(String keyword);
	List<ContactEntity> findAll(Specification<ContactEntity> searchWith, Sort defaultSort);

}
