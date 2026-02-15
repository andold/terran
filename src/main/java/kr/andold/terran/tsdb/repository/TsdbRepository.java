package kr.andold.terran.tsdb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import kr.andold.terran.tsdb.entity.TsdbEntity;

@Repository
public interface TsdbRepository extends JpaRepository<TsdbEntity, Integer>, JpaSpecificationExecutor<TsdbEntity> {

}
