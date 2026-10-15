package com.fiap.mecanica.shared.seguranca.infra.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigurationUnitTest {

    @Mock
    private UserAuthenticationFilter userAuthenticationFilter;

    @InjectMocks
    private SecurityConfiguration securityConfiguration;

    @Test
    void roleConstants_shouldHaveCorrectValues() {
        assertEquals("ADMINISTRADOR", SecurityConfiguration.ADMINISTRADOR);
    }

    @Test
    void endpointsSemAutenticacao_shouldContainExpectedPaths() {
        String[] endpoints = SecurityConfiguration.ENDPOINTS_SEM_AUTENTICACAO;

        assertArrayEquals(new String[]{
                "/gestao/v3/api-docs/**",
                "/gestao/swagger-ui",
                "/gestao/swagger-ui/**",
                "/favicon.ico",
                "/actuator/health/**"
        }, endpoints);
    }

    @Test
    void userDetailsService_shouldNeverFindUser() {
        UserDetailsService service = securityConfiguration.userDetailsService();

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("52998224725"));
    }
}
