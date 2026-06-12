package com.team.peektime_api.domain.user.repository;

import com.team.peektime_api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByDeviceUuid(String deviceUuid);

    @Query("SELECT u FROM User u WHERE u.withdrawnAt IS NOT NULL AND u.withdrawnAt < :before")
    List<User> findWithdrawnBefore(@Param("before") LocalDateTime before);
}
