package com.fiap.mecanica.shared.seguranca.infra.token.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fiap.mecanica.resources.JwtTestFactory;
import com.fiap.mecanica.shared.seguranca.core.domain.RoleEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceUnitTest {

    private static final String SECRET = Base64.getUrlEncoder().encodeToString(
            "supersecretkey1234567890123456ab".getBytes()
    );
    private static final String ISSUER = "test-issuer";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(
            Base64.getUrlDecoder().decode(SECRET)
    );

    @Mock
    private HttpServletRequest httpServletRequest;

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(ALGORITHM, ISSUER, httpServletRequest);
    }

    @Test
    void getUserId_shouldReturnUserIdFromValidToken() {
        String token = JwtTestFactory.gerar(ALGORITHM, ISSUER, 42L, RoleEnum.ROLE_MECANICO);
        Mockito.when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertEquals(42L, jwtTokenService.getUserId());
    }

    @Test
    void getUserId_shouldThrowJWTVerificationExceptionForInvalidToken() {
        Mockito.when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer invalid.token.here");

        assertThrows(JWTVerificationException.class, () -> jwtTokenService.getUserId());
    }

    @Test
    void getRoles_shouldReturnRolesFromValidToken() {
        String token = JwtTestFactory.gerar(ALGORITHM, ISSUER, 1L, RoleEnum.ROLE_ATENDENTE, RoleEnum.ROLE_MECANICO);
        Mockito.when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        List<RoleEnum> roles = jwtTokenService.getRoles();

        assertEquals(List.of(RoleEnum.ROLE_ATENDENTE, RoleEnum.ROLE_MECANICO), roles);
    }

    @Test
    void getRoles_shouldReturnEmptyListWhenTokenHasNoRolesClaim() {
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("7")
                .withExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .sign(ALGORITHM);
        Mockito.when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertTrue(jwtTokenService.getRoles().isEmpty());
    }

    @Test
    void getRoles_shouldThrowJWTVerificationExceptionForInvalidToken() {
        Mockito.when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer invalid.token.here");

        assertThrows(JWTVerificationException.class, () -> jwtTokenService.getRoles());
    }

    @Test
    void decodeToken_shouldReturnDecodedJWTForValidToken() {
        String token = JwtTestFactory.gerar(ALGORITHM, ISSUER, 1L, RoleEnum.ROLE_ADMINISTRADOR);

        DecodedJWT decoded = jwtTokenService.decodeToken(token);

        assertEquals("1", decoded.getSubject());
        assertEquals(ISSUER, decoded.getIssuer());
    }

    @Test
    void decodeToken_shouldThrowForTokenWithDifferentIssuer() {
        String token = JwtTestFactory.gerar(ALGORITHM, "outro-issuer", 1L, RoleEnum.ROLE_ADMINISTRADOR);

        assertThrows(JWTVerificationException.class, () -> jwtTokenService.decodeToken(token));
    }

    @Test
    void decodeToken_shouldThrowForTokenSignedWithDifferentKey() {
        Algorithm otherAlgorithm = Algorithm.HMAC256("anotherkey1234567890123456789012".getBytes());
        String token = JwtTestFactory.gerar(otherAlgorithm, ISSUER, 1L, RoleEnum.ROLE_ADMINISTRADOR);

        assertThrows(JWTVerificationException.class, () -> jwtTokenService.decodeToken(token));
    }
}
