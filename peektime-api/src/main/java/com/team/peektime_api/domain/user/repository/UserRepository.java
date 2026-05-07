package com.team.peektime_api.domain.user.repository;

import com.team.peektime_api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByDeviceUuid(String deviceUuid);
}
