package com.abhishek.quizportal.web.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

public class SignUpForm {

    @NotBlank(message = "Tell us your name")
    @Size(max = 80, message = "Keep your name under 80 characters")
    private String displayName;

    @NotBlank(message = "Email is required")
    @Email(message = "That doesn't look like an email address")
    @Size(max = 120)
    private String email;

    @NotBlank(message = "Choose a password")
    @Size(min = 8, max = 72, message = "Use 8 to 72 characters")
    @Pattern(regexp = ".*\\d.*", message = "Include at least one number")
    private String password;

    private String repeatPassword;

    @AssertTrue(message = "The passwords don't match")
    public boolean isPasswordsMatch() {
        return Objects.equals(password, repeatPassword);
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRepeatPassword() { return repeatPassword; }
    public void setRepeatPassword(String repeatPassword) { this.repeatPassword = repeatPassword; }
}
