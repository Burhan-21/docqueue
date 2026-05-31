package com.docqueue.user.repository;

import com.docqueue.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE u.email = :email AND r.name = :role")
    Optional<User> findByEmailAndRole(String email, String role);
}
