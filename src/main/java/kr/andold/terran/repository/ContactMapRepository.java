package kr.andold.terran.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import kr.andold.terran.entity.ContactMapEntity;
import kr.andold.terran.param.ContactMapParam;

@Repository
public interface ContactMapRepository extends JpaRepository<ContactMapEntity, Integer> {
	final String QUERY_SEARCH_PARAM	=	""
			+ "	SELECT	x"
			+ "		FROM	ContactMapEntity	x"
			//	keyword
			+ "		WHERE	("
			+ "				:#{#param.keyword}	IS NULL"
			+ "			OR	:#{#param.keyword}	=	''"
			+ "			OR	LOWER(x.key)		LIKE	CONCAT('%', LOWER(:#{#param.keyword}), '%')"
			//	vcardId
			+ "		)	AND	("
			+ "				:#{#param.vcardId}	IS NULL"
			+ "			OR	x.vcardId			=	:#{#param.vcardId}"
			//	vcardIds
			+ "		)	AND	("
			+ "				:#{#param.vcardIds}	IS NULL"
			+ "			OR	x.vcardId			IN	:#{#param.vcardIds}"
			// value
//			+ "		)	AND	("
//			+ "				:#{#param.value}	IS NULL"
//			+ "			OR	(x.value			>=		:#{#param.value}"
			//	updated
			+ "		)	AND	("
			+ "				:#{#param.updated}	IS NULL"
			+ "			OR	x.updated			>=	:#{#param.updated}"
			//	flexable
			+ "		)"
			+ "	ORDER BY	x.vcardId ASC, x.key ASC"
			;


	@Query(value = QUERY_SEARCH_PARAM, nativeQuery = false)
	List<ContactMapEntity> search(@Param("param") ContactMapParam param);

}
