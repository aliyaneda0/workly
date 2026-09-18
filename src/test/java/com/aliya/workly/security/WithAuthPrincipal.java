package com.aliya.workly.security;

import com.aliya.workly.user.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Stand-in for @WithMockUser. That one builds a stock UserDetails principal, which
// SecurityUtils treats as anonymous — it only recognises AuthPrincipal, the type
// JwtAuthenticationFilter puts in the SecurityContext. This builds the same shape.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithAuthPrincipalSecurityContextFactory.class)
public @interface WithAuthPrincipal {

    long id() default 1L;

    String email() default "test-user@workly.test";

    Role role() default Role.APPLICANT;
}
