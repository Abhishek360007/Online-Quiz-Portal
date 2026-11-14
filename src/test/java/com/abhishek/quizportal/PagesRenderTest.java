package com.abhishek.quizportal;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.domain.Attempt;
import com.abhishek.quizportal.domain.Quiz;
import com.abhishek.quizportal.service.AttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Renders every template so a broken expression fails the build instead of a user's request. */
class PagesRenderTest extends PortalTestBase {

    @Autowired AttemptService attempts;

    @Test
    void everyPageRenders() throws Exception {
        Account student = newStudent();
        Quiz quiz = liveQuiz("Render quiz", 10);
        Attempt done = attempts.begin(student, quiz.getId());
        attempts.finish(student, done.getId(), Map.of(quiz.getQuestions().get(0).getId(), 1));
        Long questionId = quiz.getQuestions().get(0).getId();

        expectPage("/signin", null, "Sign in");
        expectPage("/signup", null, "Create an account");

        expectPage("/home", asAdminAccount(), "Avg. score");
        expectPage("/admin/quizzes", asAdminAccount(), "Render quiz");
        expectPage("/admin/quizzes/create", asAdminAccount(), "New quiz");
        expectPage("/admin/quizzes/" + quiz.getId(), asAdminAccount(), "2 + 2 = ?");
        expectPage("/admin/quizzes/" + quiz.getId() + "/settings", asAdminAccount(), "Quiz settings");
        expectPage("/admin/quizzes/" + quiz.getId() + "/scores", asAdminAccount(), student.getDisplayName());
        expectPage("/admin/quizzes/" + quiz.getId() + "/questions/create", asAdminAccount(), "Add question");
        expectPage("/admin/questions/" + questionId, asAdminAccount(), "Edit question");
        expectPage("/attempt/" + done.getId() + "/review", asAdminAccount(), "attempt");
        expectPage("/leaderboard", asAdminAccount(), student.getDisplayName());

        expectPage("/home", student, "Your average");
        expectPage("/catalog", student, "Render quiz");
        expectPage("/catalog?topic=General&find=render", student, "Render quiz");
        expectPage("/catalog/" + quiz.getId(), student, "Start the quiz");
        expectPage("/my/attempts", student, "Render quiz");
        expectPage("/attempt/" + done.getId() + "/review", student, "Because it is.");

        Account fresh = newStudent();
        Attempt open = attempts.begin(fresh, quiz.getId());
        expectPage("/attempt/" + open.getId(), fresh, "Submit answers");
    }

    @Test
    void missingThingsShowTheErrorPage() throws Exception {
        mvc.perform(get("/admin/quizzes/999999").with(asAdmin())).andExpect(status().isNotFound());
    }

    private Account asAdminAccount() {
        return accounts.require(ADMIN_EMAIL);
    }

    private void expectPage(String url, Account viewer, String text) throws Exception {
        var request = get(url);
        if (viewer != null) {
            request = request.with(as(viewer));
        }
        mvc.perform(request).andExpect(status().isOk()).andExpect(content().string(containsString(text)));
    }
}
