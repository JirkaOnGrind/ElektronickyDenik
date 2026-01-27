package com.example.authdemo.repository;

import com.example.authdemo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    Optional<User> findByPhone(String phone);
    List<User> findByRoleAndKeyAndDeletedAtIsNull(String role, String key);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    List<User> findByRoleNot(String role);
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.verificated = false AND u.termsAcceptedAt < :threshold")
    void deleteUnverifiedUsersOlderThan(@Param("threshold") LocalDateTime threshold);

    List<User> findByKeyAndDeletedAtIsNull(String key);
}
