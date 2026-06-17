package com.team.peektime_api.domain.calendar.entity;

import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_record_user_date", columnNames = {"user_id", "record_date"})
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

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "memo", length = 200)
    private String memo;

    @Builder(access = AccessLevel.PRIVATE)
    private UserRecord(User user, LocalDate recordDate, String objectKey, String memo) {
        this.user = user;
        this.recordDate = recordDate;
        this.objectKey = objectKey;
        this.memo = memo;
    }

    public void update(String objectKey, String memo) {
        this.objectKey = objectKey;
        this.memo = memo;
    }

    public static UserRecord create(User user, LocalDate recordDate, String objectKey, String memo) {
        return UserRecord.builder()
                .user(user)
                .recordDate(recordDate)
                .objectKey(objectKey)
                .memo(memo)
                .build();
    }
}
