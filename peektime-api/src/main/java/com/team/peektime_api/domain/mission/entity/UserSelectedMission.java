package com.team.peektime_api.domain.mission.entity;

import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
// 선택 미션은 하루 1개 정책. 동시 요청이 재확인을 동시에 통과해도 DB 레벨에서 중복 저장을 막는다
@Table(name = "user_selected_mission",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_selected_mission_user_date",
                columnNames = {"user_id", "selected_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSelectedMission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solar_term_id", nullable = false)
    private SolarTerm solarTerm;

    @Column(name = "selected_date", nullable = false)
    private LocalDate selectedDate;

    @Builder(access = AccessLevel.PRIVATE)
    private UserSelectedMission(User user, Mission mission, SolarTerm solarTerm, LocalDate selectedDate) {
        this.user = user;
        this.mission = mission;
        this.solarTerm = solarTerm;
        this.selectedDate = selectedDate;
    }

    public static UserSelectedMission create(User user, Mission mission, SolarTerm solarTerm, LocalDate selectedDate) {
        return UserSelectedMission.builder()
                .user(user)
                .mission(mission)
                .solarTerm(solarTerm)
                .selectedDate(selectedDate)
                .build();
    }
}