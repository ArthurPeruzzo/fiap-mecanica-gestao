package com.fiap.mecanica.shared.seguranca.infra.config;

import com.fiap.mecanica.shared.seguranca.core.domain.RoleEnum;
import com.fiap.mecanica.shared.seguranca.core.gateway.TokenGateway;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationFilterUnitTest {

    @InjectMocks
    private UserAuthenticationFilter filter;

    @Mock
    private TokenGateway tokenGateway;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    void doFilterInternal_shouldSkipAuthAndPassThroughForPublicEndpoint() throws Exception {
        Mockito.when(request.getRequestURI()).thenReturn("/gestao/v3/api-docs");

        filter.doFilterInternal(request, response, filterChain);

        Mockito.verify(filterChain).doFilter(request, response);
        Mockito.verifyNoInteractions(tokenGateway);
    }

    @Test
    void doFilterInternal_shouldThrowWhenTokenIsMissingOnProtectedEndpoint() {
        Mockito.when(request.getRequestURI()).thenReturn("/cliente");
        Mockito.when(request.getHeader("Authorization")).thenReturn(null);

        assertThrows(Exception.class, () -> filter.doFilterInternal(request, response, filterChain));

        Mockito.verifyNoInteractions(tokenGateway);
    }

    @Test
    void doFilterInternal_shouldSetAuthenticationFromTokenClaims() throws Exception {
        SecurityContextHolder.clearContext();

        Mockito.when(request.getRequestURI()).thenReturn("/cliente");
        Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        Mockito.when(tokenGateway.getUserId()).thenReturn(1L);
        Mockito.when(tokenGateway.getRoles()).thenReturn(List.of(RoleEnum.ROLE_ADMINISTRADOR));

        filter.doFilterInternal(request, response, filterChain);

        Mockito.verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("1", authentication.getName());
        assertEquals(List.of("ROLE_ADMINISTRADOR"),
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    @Test
    void doFilterInternal_shouldPropagateErrorWhenTokenIsInvalid() {
        Mockito.when(request.getRequestURI()).thenReturn("/cliente");
        Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
        Mockito.when(tokenGateway.getUserId()).thenThrow(new RuntimeException("Token inválido ou expirado."));

        assertThrows(RuntimeException.class, () -> filter.doFilterInternal(request, response, filterChain));
    }
}
