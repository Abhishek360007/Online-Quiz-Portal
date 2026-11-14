package com.abhishek.quizportal.web;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.service.AccountService;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
class WebConfig implements WebMvcConfigurer {

    private final AccountService accounts;

    WebConfig(AccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(SignedIn.class)
                        && Account.class.equals(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                          NativeWebRequest request, WebDataBinderFactory binders) {
                return signedInAccount(accounts);
            }
        });
    }

    /** The signed-in account, or null for anonymous visitors. */
    static Account signedInAccount(AccountService accounts) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return accounts.require(auth.getName());
    }
}
