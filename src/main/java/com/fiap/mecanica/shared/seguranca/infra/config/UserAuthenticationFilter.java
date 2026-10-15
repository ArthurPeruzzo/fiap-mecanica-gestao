package com.fiap.mecanica.shared.seguranca.infra.config;

import com.fiap.mecanica.shared.seguranca.core.gateway.TokenGateway;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@Profile({"!controller-test"})
@RequiredArgsConstructor
public class UserAuthenticationFilter extends OncePerRequestFilter {

    private final TokenGateway tokenGateway;
    private static final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (checkIfEndpointIsNotPublic(request)) {
            verifyHasTokenInRequest(request);
            getAndSetAuthentication();
        }

        filterChain.doFilter(request, response);
    }

    private boolean checkIfEndpointIsNotPublic(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return Arrays.stream(SecurityConfiguration.ENDPOINTS_SEM_AUTENTICACAO)
                .noneMatch(pattern -> matcher.match(pattern, requestURI));
    }

    private void verifyHasTokenInRequest(HttpServletRequest request) throws AuthenticationException {
        String token = recoveryToken(request);
        if (token == null) {
            log.warn("token ausente");
            throw new AuthenticationException("O token está ausente");
        }
    }

    private String recoveryToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            return authorizationHeader.replace("Bearer ", "");
        }
        return null;
    }

    /**
     * Diferente do monólito, não há tabela {@code users} aqui: principal = {@code sub} do token
     * e authorities = claim {@code roles}. A assinatura HS256 garante que as claims são confiáveis.
     */
    private void getAndSetAuthentication() {
        Long userId = tokenGateway.getUserId();
        List<GrantedAuthority> authorities = tokenGateway.getRoles().stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
