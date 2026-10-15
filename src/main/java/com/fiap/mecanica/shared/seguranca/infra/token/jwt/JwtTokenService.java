package com.fiap.mecanica.shared.seguranca.infra.token.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fiap.mecanica.shared.seguranca.core.domain.RoleEnum;
import com.fiap.mecanica.shared.seguranca.core.gateway.TokenGateway;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtTokenService implements TokenGateway {

    private static final String ROLES = "roles";
    private static final String AUTHORIZATION = "Authorization";

    private final Algorithm algorithm;
    private final String jwtIssuer;
    private final HttpServletRequest httpServletRequest;

    private String getAuthorization() {
        return Optional.ofNullable(httpServletRequest.getHeader(AUTHORIZATION)).map(s -> s.replace("Bearer ", "")).orElse(null);
    }

    @Override
    public Long getUserId() {
        return Long.parseLong(decodeToken(getAuthorization()).getSubject());
    }

    /**
     * Sem banco de usuários neste serviço, as authorities vêm da claim {@code roles} do token
     * (assinado com o mesmo segredo do monólito). Token sem a claim => lista vazia.
     */
    @Override
    public List<RoleEnum> getRoles() {
        List<RoleEnum> roles = decodeToken(getAuthorization()).getClaim(ROLES).asList(RoleEnum.class);
        return roles == null ? List.of() : roles;
    }

    public DecodedJWT decodeToken(String token) {
        try {
            return JWT.require(algorithm)
                    .withIssuer(jwtIssuer)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException ex) {
            log.warn("Falha ao verificar JWT: {}", ex.getMessage());
            throw new JWTVerificationException("Token inválido ou expirado.", ex);
        }
    }
}
