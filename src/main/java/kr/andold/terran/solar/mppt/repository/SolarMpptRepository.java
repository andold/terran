package kr.andold.terran.solar.mppt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import kr.andold.terran.solar.mppt.entity.SolarMpptEntity;

@Repository
public interface SolarMpptRepository extends JpaRepository<SolarMpptEntity, Integer>, JpaSpecificationExecutor<SolarMpptEntity> {

}
