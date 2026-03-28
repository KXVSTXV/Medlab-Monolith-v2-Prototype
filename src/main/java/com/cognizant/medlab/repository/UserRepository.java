package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.identity.Role;
import com.cognizant.medlab.domain.identity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndIsDeletedFalse(String username);
    Optional<User> findByEmailAndIsDeletedFalse(String email);
    boolean existsByUsernameAndIsDeletedFalse(String username);
    boolean existsByEmailAndIsDeletedFalse(String email);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false")
    Page<User> findAllActive(Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :role AND u.isDeleted = false")
    Page<User> findByRole(@Param("role") String role, Pageable pageable);
}
