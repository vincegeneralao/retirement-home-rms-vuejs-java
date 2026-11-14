package com.rms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rms.entity.Role;
import com.rms.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    java.util.List<User> findByRoleOrderByIdAsc(Role role);
}
