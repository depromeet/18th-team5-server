package com.team.peektime_admin.domain.solarterm.repository;

import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface SolarTermRepository extends JpaRepository<SolarTerm, Long> {

    @Query("SELECT s FROM SolarTerm s WHERE s.startDate <= :date AND s.endDate >= :date")
    Optional<SolarTerm> findByDate(@Param("date") LocalDate date);
}