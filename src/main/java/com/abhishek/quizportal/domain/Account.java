package com.abhishek.quizportal.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    protected Account() {
    }

    public Account(String displayName, String email, String passwordHash, Role role) {
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.registeredAt = LocalDateTime.now();
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public void replacePasswordHash(String newHash) {
        this.passwordHash = newHash;
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
}
