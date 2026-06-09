package com.team.peektime_api.domain.announcement.repository;

import com.team.peektime_api.domain.announcement.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
}