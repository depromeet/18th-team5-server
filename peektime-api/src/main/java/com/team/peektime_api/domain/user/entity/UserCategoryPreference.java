package com.team.peektime_api.domain.user.entity;

import com.team.peektime_api.global.common.BaseEntity;
import com.team.peektime_api.global.common.enums.EnjoyType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_category_preference")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCategoryPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private EnjoyType category;

    @Column(name = "preference_rank", nullable = false)
    private int rank;

    @Builder
    public UserCategoryPreference(User user, EnjoyType category, int rank) {
        this.user = user;
        this.category = category;
        this.rank = rank;
    }
}
