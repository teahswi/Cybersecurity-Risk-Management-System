package com.cybershield.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Session-based Spring Security configuration.
 * URL rules give a first layer of protection; every business operation is ALSO protected with
 * {@code @PreAuthorize} and ownership checks in the service layer (hiding a button is never enough).
 * CSRF protection stays enabled for all forms and AJAX calls.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   LoginSuccessHandler loginSuccessHandler,
                                                   LoginFailureHandler loginFailureHandler,
                                                   AuditLogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/login", "/access-denied", "/error", "/favicon.ico",
                        "/css/**", "/js/**", "/images/**", "/vendor/**").permitAll()
                .requestMatchers("/users/**", "/settings/**", "/system/**").hasRole(Roles.MANAGER)
                .requestMatchers("/alerts/**", "/incidents/**").hasAnyRole(Roles.ANALYST, Roles.MANAGER)
                .requestMatchers("/software/**").hasAnyRole(Roles.ANALYST, Roles.AUDITOR, Roles.MANAGER)
                .requestMatchers("/vulnerabilities/**", "/asset-vulnerabilities/**")
                    .hasAnyRole(Roles.ANALYST, Roles.ENGINEER, Roles.MANAGER)
                .requestMatchers("/risks/**").hasAnyRole(Roles.ANALYST, Roles.AUDITOR, Roles.MANAGER)
                .requestMatchers("/audits/actions/**").authenticated()
                .requestMatchers("/audits/**").hasAnyRole(Roles.AUDITOR, Roles.MANAGER)
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
            .headers(headers -> headers.frameOptions(frame -> frame.deny()));
        return http.build();
    }
}
