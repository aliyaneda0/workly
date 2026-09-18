package com.aliya.workly.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

class WithAuthPrincipalSecurityContextFactory implements WithSecurityContextFactory<WithAuthPrincipal> {

    @Override
    public SecurityContext createSecurityContext(WithAuthPrincipal annotation) {
        AuthPrincipal principal = new AuthPrincipal(annotation.id(), annotation.email(), annotation.role());
        // "ROLE_" + role mirrors what JwtAuthenticationFilter grants, so hasRole(...) rules behave the same
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
