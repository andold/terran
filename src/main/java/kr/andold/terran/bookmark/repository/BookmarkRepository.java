package kr.andold.terran.bookmark.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import kr.andold.terran.bookmark.domain.BookmarkParameter;
import kr.andold.terran.bookmark.entity.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Integer> {

	List<Bookmark> findByOrderByPidAscIdAsc();
	Bookmark findOneByPid(long pid);
	List<Bookmark> findByIdOrUrl(Integer id, String trim);

	@Query(value = "SELECT x"
		+ "	FROM	Bookmark	x"
		+ "	WHERE"
		+ "		("
		+ "				:#{#param.id}	IS NULL"
		+ "			OR	x.id			=	:#{#param.id}"
		+ "		) AND ("
		+ "				:#{#param.pid}	IS NULL"
		+ "			OR	x.pid			=	:#{#param.pid}"
		+ "		) AND ("
		+ "				:#{#param.keyword}	IS NULL"
		+ "			OR	:#{#param.keyword}	= ''"
		+ "			OR	x.title				LIKE	CONCAT('%', :#{#param.keyword}, '%')"
		+ "			OR	x.url				LIKE	CONCAT('%', :#{#param.keyword}, '%')"
		+ "			OR	x.description		LIKE	CONCAT('%', :#{#param.keyword}, '%')"
		+ "		)",
		nativeQuery = false)
	List<Bookmark> search(@Param("param") BookmarkParameter param);

}
