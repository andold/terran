package kr.andold.terran.ics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import kr.andold.terran.ics.entity.VCalendarEntity;

@Repository
public interface VCalendarRepository extends JpaRepository<VCalendarEntity, Integer> {

}
