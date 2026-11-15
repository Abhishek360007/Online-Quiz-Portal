package com.abhishek.quizportal;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChangePasswordTest extends PortalTestBase {

    @Autowired AccountRepository accountRepository;
    @Autowired PasswordEncoder encoder;

    private String hashOf(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getPasswordHash();
    }

    @Test
    void pageRendersForAdminAndStudent() throws Exception {
        mvc.perform(get("/account/password").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Change password")));
        mvc.perform(get("/account/password").with(as(newStudent())))
                .andExpect(status().isOk());
    }

    @Test
    void visitorsMustSignInFirst() throws Exception {
        mvc.perform(get("/account/password")).andExpect(redirectedUrl("/signin"));
    }

    @Test
    void adminChangesPasswordAndCanSignInWithIt() throws Exception {
        Account admin = accounts.require(ADMIN_EMAIL);

        mvc.perform(post("/account/password").with(asAdmin()).with(csrf())
                        .param("currentPassword", "Admin@12345")
                        .param("newPassword", "BrandNew2026")
                        .param("repeatNewPassword", "BrandNew2026"))
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attributeExists("notice"));

        assertThat(encoder.matches("BrandNew2026", hashOf(admin))).isTrue();
        assertThat(encoder.matches("Admin@12345", hashOf(admin))).isFalse();

        mvc.perform(post("/signin").with(csrf()).param("email", ADMIN_EMAIL).param("password", "BrandNew2026"))
                .andExpect(redirectedUrl("/home"));
        mvc.perform(post("/signin").with(csrf()).param("email", ADMIN_EMAIL).param("password", "Admin@12345"))
                .andExpect(redirectedUrl("/signin?failed"));
    }

    @Test
    void wrongCurrentPasswordIsRejected() throws Exception {
        Account student = newStudent();
        String before = hashOf(student);

        mvc.perform(post("/account/password").with(as(student)).with(csrf())
                        .param("currentPassword", "not-my-password1")
                        .param("newPassword", "another123")
                        .param("repeatNewPassword", "another123"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/password"))
                .andExpect(model().attributeHasFieldErrors("form", "currentPassword"));

        assertThat(hashOf(student)).isEqualTo(before);
    }

    @Test
    void newPasswordRulesAreEnforced() throws Exception {
        Account student = newStudent();

        mvc.perform(post("/account/password").with(as(student)).with(csrf())
                        .param("currentPassword", "password1")
                        .param("newPassword", "short")
                        .param("repeatNewPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "newPassword", "newPasswordsMatch"));

        mvc.perform(post("/account/password").with(as(student)).with(csrf())
                        .param("currentPassword", "password1")
                        .param("newPassword", "password1")
                        .param("repeatNewPassword", "password1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "newPasswordDifferent"));
    }

    @Test
    void requestWithoutCsrfTokenIsRefused() throws Exception {
        mvc.perform(post("/account/password").with(asAdmin())
                        .param("currentPassword", "Admin@12345")
                        .param("newPassword", "BrandNew2026")
                        .param("repeatNewPassword", "BrandNew2026"))
                .andExpect(status().isForbidden());
    }
}
