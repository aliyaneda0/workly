package com.aliya.workly.security;

import com.aliya.workly.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Runs on every request, before Spring Security's own checks. Reads the token, and if it's
// valid, trusts it — the SecurityFilterChain rules (SecurityConfig) decide whether that's
// enough for a given path. See Workly Auth Blueprint, Section 9, for the full request diagram.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);

        if (header != null && header.startsWith(PREFIX)) {
            try {
                Claims claims = jwtService.parseAccessToken(header.substring(PREFIX.length()));

                AuthPrincipal principal = new AuthPrincipal(
                        jwtService.extractUserId(claims),
                        claims.getSubject(),
                        Role.valueOf(jwtService.extractRole(claims)));

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole()));
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                // Bad, expired, or wrong-type (refresh-as-access) token — request stays
                // unauthenticated; SecurityConfig's authorizeHttpRequests rules decide what
                // happens next, not this filter.
            }
        }

        filterChain.doFilter(request, response);
    }
}
