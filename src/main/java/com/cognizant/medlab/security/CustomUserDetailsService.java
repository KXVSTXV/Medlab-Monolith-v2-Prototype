package com.cognizant.medlab.security;

import com.cognizant.medlab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService implementation.
 * Loads users by username from the DB (soft-delete filtered).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException("User not found: " + username));
    }
}
