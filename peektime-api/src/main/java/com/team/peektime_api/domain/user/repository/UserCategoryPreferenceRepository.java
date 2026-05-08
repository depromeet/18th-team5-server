package com.team.peektime_api.domain.user.repository;

import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.entity.UserCategoryPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCategoryPreferenceRepository extends JpaRepository<UserCategoryPreference, Long> {

    List<UserCategoryPreference> findByUserOrderByRank(User user);

    void deleteByUser(User user);
}
