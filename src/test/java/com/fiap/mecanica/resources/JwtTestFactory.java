package com.fiap.mecanica.resources;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fiap.mecanica.shared.seguranca.core.domain.RoleEnum;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * Emite tokens no mesmo formato do monólito (login) / Lambda, para os testes de integração.
 * Este serviço não tem endpoint de login.
 */
public final class JwtTestFactory {

    private JwtTestFactory() {
    }

    public static String gerar(Algorithm algorithm, String issuer, Long userId, RoleEnum... roles) {
        return JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(4, ChronoUnit.HOURS))
                .withSubject(String.valueOf(userId))
                .withArrayClaim("roles", Arrays.stream(roles).map(Enum::name).toArray(String[]::new))
                .sign(algorithm);
    }
}
