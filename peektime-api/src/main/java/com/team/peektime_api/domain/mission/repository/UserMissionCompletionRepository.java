package com.team.peektime_api.domain.mission.repository;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import com.team.peektime_api.global.common.enums.MissionType;

public interface UserMissionCompletionRepository extends JpaRepository<UserMissionCompletion, Long> {

    boolean existsByUser_IdAndMission_Id(Long userId, Long missionId);

    long countByUser_IdAndMissionType(Long userId, MissionType missionType);

    @Query("SELECT COUNT(c) FROM UserMissionCompletion c " +
            "WHERE c.user.id = :userId AND c.missionType = :missionType " +
            "AND c.createdAt >= :startOfDay AND c.createdAt < :startOfNextDay")
    long countTodayByUserIdAndMissionType(
            @Param("userId") Long userId,
            @Param("missionType") MissionType missionType,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay);

    @Query("SELECT c FROM UserMissionCompletion c " +
            "WHERE c.user.id = :userId AND c.mission.id = :missionId " +
            "ORDER BY c.createdAt DESC")
    List<UserMissionCompletion> findCompletions(
            @Param("userId") Long userId,
            @Param("missionId") Long missionId);

    long countByMission_Id(Long missionId);

    @Query("SELECT c FROM UserMissionCompletion c " +
            "WHERE c.user.id = :userId AND c.objectKey IS NOT NULL " +
            "ORDER BY c.createdAt DESC LIMIT 3")
    List<UserMissionCompletion> findRecentRecordsWithImage(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM UserMissionCompletion c " +
            "WHERE c.user.id = :userId AND c.createdAt BETWEEN :startDate AND :endDate")
    long countByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c FROM UserMissionCompletion c " +
            "WHERE c.user.id = :userId " +
            "AND c.createdAt BETWEEN :startDate AND :endDate " +
            "AND c.objectKey IS NOT NULL " +
            "ORDER BY c.createdAt DESC LIMIT 3")
    List<UserMissionCompletion> findRecentRecordsWithImageByPeriod(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c FROM UserMissionCompletion c " +
            "WHERE c.user.id = :userId AND c.createdAt BETWEEN :start AND :end " +
            "ORDER BY c.createdAt ASC")
    List<UserMissionCompletion> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // 전역 공유 피드: 사진 있는 미션 완료를 작성자 무관하게 최신순으로 (캐시 MISS 시 재적재용)
    @Query("SELECT c FROM UserMissionCompletion c " +
            "WHERE c.objectKey IS NOT NULL " +
            "ORDER BY c.createdAt DESC")
    List<UserMissionCompletion> findRecentGlobalWithImage(Pageable pageable);

    // 전역 피드 좋아요 카운터 — 원자 ±1 UPDATE. 엔티티 로드 없이 핫 완료행만 직접 갱신해
    // read-modify-write 유실을 막는다. 멤버십(feed_like) 변경이 실제 일어났을 때만 호출된다.
    @Modifying
    @Query("UPDATE UserMissionCompletion c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    // like_count > 0 가드 — 카운터가 음수로 내려가지 않게 한다(드리프트·중복 취소 방어)
    @Modifying
    @Query("UPDATE UserMissionCompletion c SET c.likeCount = c.likeCount - 1 " +
            "WHERE c.id = :id AND c.likeCount > 0")
    int decrementLikeCount(@Param("id") Long id);

    // 전역 피드 조건부 재캐싱용: 피드에 노출되는 완료(사진 있음)의 최신 createdAt.
    // 캐시 최신값과 비교해 새 완료가 없으면 상위 20건 재조회·교체를 스킵한다. 완료가 없으면 null.
    @Query("SELECT MAX(c.createdAt) FROM UserMissionCompletion c WHERE c.objectKey IS NOT NULL")
    LocalDateTime findLatestCreatedAtWithImage();

    void deleteByUser_Id(Long userId);
}
