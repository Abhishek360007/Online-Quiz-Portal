package com.abhishek.quizportal.service;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.domain.Role;
import com.abhishek.quizportal.repository.AccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AccountService implements UserDetailsService {

    private final AccountRepository accounts;
    private final PasswordEncoder encoder;

    public AccountService(AccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    /** Registers a student. Throws {@link EmailTakenException} if the address is in use. */
    @Transactional
    public Account signUp(String displayName, String email, String rawPassword) {
        String normalized = normalize(email);
        if (accounts.existsByEmailIgnoreCase(normalized)) {
            throw new EmailTakenException();
        }
        return accounts.save(new Account(displayName.trim(), normalized, encoder.encode(rawPassword), Role.STUDENT));
    }

    @Transactional
    public void createAdminIfMissing(String displayName, String email, String rawPassword) {
        String normalized = normalize(email);
        if (!accounts.existsByEmailIgnoreCase(normalized)) {
            accounts.save(new Account(displayName, normalized, encoder.encode(rawPassword), Role.ADMIN));
        }
    }

    /**
     * Changes an account's password after checking the current one.
     * Throws {@link WrongPasswordException} if the current password doesn't match.
     */
    @Transactional
    public void changePassword(Long accountId, String currentPassword, String newPassword) {
        Account account = accounts.findById(accountId).orElseThrow(() -> new ResourceNotFoundException("Account"));
        if (!encoder.matches(currentPassword, account.getPasswordHash())) {
            throw new WrongPasswordException();
        }
        account.replacePasswordHash(encoder.encode(newPassword));
    }

    public Account require(String email) {
        return accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account"));
    }

    public long studentCount() {
        return accounts.countByRole(Role.STUDENT);
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Account account = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .roles(account.getRole().name())
                .build();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static class WrongPasswordException extends RuntimeException {
        public WrongPasswordException() {
            super("Your current password is incorrect.");
        }
    }

    public static class EmailTakenException extends RuntimeException {
        public EmailTakenException() {
            super("That email is already registered.");
        }
    }
}
