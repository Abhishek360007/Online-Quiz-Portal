package com.abhishek.quizportal.config;

import com.abhishek.quizportal.service.AccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class AppConfig {

    /** System clock; tests swap in a fixed one to control quiz deadlines. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Makes sure an admin can sign in on a brand-new database. */
    @Bean
    ApplicationRunner adminBootstrap(AccountService accounts,
                                     @Value("${portal.admin.name}") String name,
                                     @Value("${portal.admin.email}") String email,
                                     @Value("${portal.admin.password}") String password) {
        return args -> accounts.createAdminIfMissing(name, email, password);
    }
}
