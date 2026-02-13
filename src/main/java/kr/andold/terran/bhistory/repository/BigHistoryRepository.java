package kr.andold.terran.bhistory.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import kr.andold.terran.bhistory.domain.BigHistorySearchRequest;
import kr.andold.terran.bhistory.entity.BigHistoryEntity;

@Repository
public interface BigHistoryRepository extends JpaRepository<BigHistoryEntity, Integer>, JpaSpecificationExecutor<BigHistoryEntity> {

	final String QUERY_SEARCH_FORM	=	""
		+ "	SELECT	x"
		+ "		FROM		BigHistoryEntity	x"
		//	start
		+ "		WHERE	("
		+ "				:#{#form.start}	=	null"
		+ "			OR	:#{#form.start}	<=	x.start"
		//	end
		+ "		)	AND	("
		+ "				:#{#form.end}	=	null"
		+ "			OR	:#{#form.end}	>	x.end"
		//	keyword
		+ "		)	AND	("
		+ "				:#{#form.keyword}	=	null"
		+ "			OR	:#{#form.keyword}	=	''"
		+ "			OR	x.title			LIKE	CONCAT('%', :#{#form.keyword}, '%')"
		+ "			OR	x.description	LIKE	CONCAT('%', :#{#form.keyword}, '%')"
		//	title
		+ "		)	AND	("
		+ "				:#{#form.title}	=		null"
		+ "			OR	:#{#form.title}	=		''"
		+ "			OR	x.title			LIKE	CONCAT('%', :#{#form.title}, '%')"
		+ "		)"
		;

	Page<BigHistoryEntity> findAllByOrderByStartAscEndAsc(Pageable pageable);

	@Query(value = QUERY_SEARCH_FORM + "	ORDER BY	x.start ASC, x.end DESC", nativeQuery = false)
	List<BigHistoryEntity> search(@Param("form") BigHistorySearchRequest form);

	@Query(value = QUERY_SEARCH_FORM + "	ORDER BY	x.start ASC, x.end DESC", nativeQuery = false)
	Page<BigHistoryEntity> search(Pageable pageable, @Param("form") BigHistorySearchRequest form);

	BigHistoryEntity findFirstByOrderByIdDesc();

}
