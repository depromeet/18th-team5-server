package com.team.peektime_api.domain.record.entity;

import com.team.peektime_api.domain.record.dto.ImageUploadConfirmRequest;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_record", indexes = {
        @Index(name = "idx_user_term", columnList = "user_id, solar_term_id"),
        @Index(name = "idx_user_date", columnList = "user_id, record_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "solar_term_id", nullable = false)
    private Long solarTermId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "memo", length = 200)
    private String memo;

    @Builder
    public UserRecord(User user, Long solarTermId, LocalDate recordDate,
                      String imageUrl, String memo) {
        this.user = user;
        this.solarTermId = solarTermId;
        this.recordDate = recordDate;
        this.imageUrl = imageUrl;
        this.memo = memo;
    }

    public static UserRecord of(User user, ImageUploadConfirmRequest request) {
        return UserRecord.builder()
                .user(user)
                .solarTermId(request.solarTermId())
                .recordDate(request.recordDate())
                .imageUrl(request.objectKey())
                .memo(request.memo())
                .build();
    }
}
