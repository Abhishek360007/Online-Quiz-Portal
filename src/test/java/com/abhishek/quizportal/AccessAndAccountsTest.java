package com.abhishek.quizportal;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.repository.AccountRepository;
import com.abhishek.quizportal.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccessAndAccountsTest extends PortalTestBase {

    @Autowired AccountRepository accountRepository;
    @Autowired PasswordEncoder encoder;

    @Test
    void adminIsCreatedOnStartup() {
        assertThat(accountRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).get()
                .extracting(Account::isAdmin).isEqualTo(true);
    }

    @Test
    void passwordsAreHashed() {
        Account student = newStudent();
        assertThat(student.getPasswordHash()).doesNotContain("password1");
        assertThat(encoder.matches("password1", student.getPasswordHash())).isTrue();
    }

    @Test
    void emailsAreUniqueIgnoringCase() {
        accounts.signUp("A", "Same@Test.dev", "password1");
        assertThatThrownBy(() -> accounts.signUp("B", "same@test.DEV", "password1"))
                .isInstanceOf(AccountService.EmailTakenException.class);
    }

    @Test
    void visitorsAreSentToSignIn() throws Exception {
        mvc.perform(get("/home")).andExpect(redirectedUrl("/signin"));
        mvc.perform(get("/catalog")).andExpect(redirectedUrl("/signin"));
    }

    @Test
    void studentsCannotReachAdminPages() throws Exception {
        mvc.perform(get("/admin/quizzes").with(as(newStudent()))).andExpect(status().isForbidden());
    }

    @Test
    void adminsCannotSitQuizzes() throws Exception {
        mvc.perform(get("/catalog").with(asAdmin())).andExpect(status().isForbidden());
    }

    @Test
    void signUpShowsEveryValidationError() throws Exception {
        mvc.perform(post("/signup").with(csrf())
                        .param("displayName", "")
                        .param("email", "nope")
                        .param("password", "short")
                        .param("repeatPassword", "other"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("form", "displayName", "email", "password", "passwordsMatch"));
    }

    @Test
    void signUpThenSignIn() throws Exception {
        mvc.perform(post("/signup").with(csrf())
                        .param("displayName", "Riya")
                        .param("email", "riya@test.dev")
                        .param("password", "password1")
                        .param("repeatPassword", "password1"))
                .andExpect(redirectedUrl("/signin"));

        mvc.perform(post("/signin").with(csrf()).param("email", "riya@test.dev").param("password", "password1"))
                .andExpect(redirectedUrl("/home"));

        mvc.perform(post("/signin").with(csrf()).param("email", "riya@test.dev").param("password", "wrong-pass1"))
                .andExpect(redirectedUrl("/signin?failed"));
    }

    @Test
    void duplicateEmailIsReportedOnTheForm() throws Exception {
        Account existing = newStudent();
        mvc.perform(post("/signup").with(csrf())
                        .param("displayName", "Copy")
                        .param("email", existing.getEmail())
                        .param("password", "password1")
                        .param("repeatPassword", "password1"))
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("form", "email"));
    }
}
