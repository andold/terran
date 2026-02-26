package kr.andold.terran.ics.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import kr.andold.terran.ics.domain.IcsParam;
import kr.andold.terran.ics.entity.VCalendarComponentEntity;

@Repository
public interface VCalendarComponentRepository extends JpaRepository<VCalendarComponentEntity, Integer> {

	List<VCalendarComponentEntity> findByContentContains(String keyword);
	
	@Deprecated
	@Query(value = ""
			+ "	SELECT	x"
			+ "		FROM	VCalendarComponentEntity		x"
			+ "		WHERE	("
			+ "				:#{#param.keyword}	IS NULL"
			+ "			OR	:#{#param.keyword}	=	''"
			+ "			OR	x.content			LIKE	%:#{#param.keyword}%"
//			+ "			OR	POSITION(:#{#param.keyword} IN x.content)	>	0"
//			+ "			OR	x.content	~	:#{#param.keyword}"
			//	vcalendarId
			+ "		)	AND	("
			+ "				:#{#param.vcalendarId}	IS NULL"
			+ "			OR	x.vcalendarId	=	:#{#param.vcalendarId}"
			//	start
			+ "		)	AND	("
			+ "				:#{#param.start}	IS NULL"
			+ "			OR	x.end	>=	:#{#param.start}"
			//	end
			+ "		)	AND	("
			+ "				x.start	<	:#{#param.end}"
			+ "		)"
			+ "	ORDER BY	x.start DESC, x.end DESC", nativeQuery = false)
	List<VCalendarComponentEntity> search(@Param("param") IcsParam param);

	List<VCalendarComponentEntity> findAllByVcalendarId(Integer vcalendarId);

	List<VCalendarComponentEntity> findAllByContentContainsAndVcalendarIdAndEndGreaterThanEqualAndStartLessThan(
			String keyword, Integer vcalendarId, Date start, Date end);

}
