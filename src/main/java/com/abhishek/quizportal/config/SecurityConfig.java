package com.abhishek.quizportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers("/", "/signin", "/signup", "/assets/**", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/attempt/*/review").authenticated()
                        .requestMatchers("/catalog/**", "/attempt/**", "/my/**").hasRole("STUDENT")
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/signin")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/signin?failed")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/signout")
                        .logoutSuccessUrl("/signin?bye"))
                .build();
    }
}
