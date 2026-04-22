package com.team.peektime_admin.domain.solarterm.repository;

import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolarTermRepository extends JpaRepository<SolarTerm, Long> {
}