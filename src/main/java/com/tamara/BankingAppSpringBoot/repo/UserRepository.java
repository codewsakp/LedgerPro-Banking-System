package com.tamara.BankingAppSpringBoot.repo;

import com.tamara.BankingAppSpringBoot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {


    Boolean existsByEmail(String email);

    Boolean existsByAccountNumber(String accountNumber);

    User findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT u FROM User u WHERE u.accountNumber = :accountNumber")
        User findByAccountNumberForUpdate(
                @Param("accountNumber") String accountNumber
        );

    Optional<User> findByEmail(String email);
}
