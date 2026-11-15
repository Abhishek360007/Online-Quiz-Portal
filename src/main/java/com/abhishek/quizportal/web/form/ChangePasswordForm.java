package com.abhishek.quizportal.web.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

public class ChangePasswordForm {

    @NotBlank(message = "Enter your current password")
    private String currentPassword;

    @NotBlank(message = "Choose a new password")
    @Size(min = 8, max = 72, message = "Use 8 to 72 characters")
    @Pattern(regexp = ".*\\d.*", message = "Include at least one number")
    private String newPassword;

    private String repeatNewPassword;

    @AssertTrue(message = "The new passwords don't match")
    public boolean isNewPasswordsMatch() {
        return Objects.equals(newPassword, repeatNewPassword);
    }

    @AssertTrue(message = "The new password must be different from the current one")
    public boolean isNewPasswordDifferent() {
        return newPassword == null || !newPassword.equals(currentPassword);
    }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getRepeatNewPassword() { return repeatNewPassword; }
    public void setRepeatNewPassword(String repeatNewPassword) { this.repeatNewPassword = repeatNewPassword; }
}
