package com.team.peektime_api.domain.solarterm.repository;

import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SolarTermRepository extends JpaRepository<SolarTerm, Long> {

    @Query("SELECT s FROM SolarTerm s WHERE s.startDate <= :date AND s.endDate >= :date")
    Optional<SolarTerm> findByDate(@Param("date") LocalDate date);

    @Query("SELECT s FROM SolarTerm s WHERE s.startDate <= :endDate AND s.endDate >= :startDate ORDER BY s.startDate ASC")
    List<SolarTerm> findOverlappingByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM SolarTerm s WHERE s.id >= :solarTermId ORDER BY s.id ASC LIMIT 2")
    List<SolarTerm> findTwoStartingFrom(@Param("solarTermId") Long solarTermId);

    @Query("SELECT s FROM SolarTerm s WHERE s.id > :lastSolarTermId ORDER BY s.id ASC LIMIT 1")
    Optional<SolarTerm> findNextAfter(@Param("lastSolarTermId") Long lastSolarTermId);
}