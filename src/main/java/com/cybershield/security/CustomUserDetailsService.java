package com.cybershield.security;

import com.cybershield.entity.User;
import com.cybershield.enums.UserStatus;
import com.cybershield.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Database-backed authentication: users and their single role are loaded from MySQL. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithRole(username == null ? "" : username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
        return new SecurityUser(user.getId(), user.getUsername(), user.getPasswordHash(), user.getFullName(),
                user.getRole().getCode(), user.getStatus() == UserStatus.ACTIVE);
    }
}
