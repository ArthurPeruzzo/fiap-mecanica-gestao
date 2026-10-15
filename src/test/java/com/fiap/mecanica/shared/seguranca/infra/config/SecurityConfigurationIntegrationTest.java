package com.fiap.mecanica.shared.seguranca.infra.config;

import com.auth0.jwt.algorithms.Algorithm;
import com.fiap.mecanica.resources.JwtTestFactory;
import com.fiap.mecanica.resources.testcontainer.AbstractContainer;
import com.fiap.mecanica.shared.seguranca.core.domain.RoleEnum;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityConfigurationIntegrationTest extends AbstractContainer {

    @LocalServerPort
    private int port;

    @Autowired private Algorithm algorithm;
    @Autowired private String jwtIssuer;

    @BeforeEach
    void configurar() {
        RestAssured.port = port;
    }

    private String token(RoleEnum... roles) {
        return JwtTestFactory.gerar(algorithm, jwtIssuer, 1L, roles);
    }

    @Test
    void shouldAdministradorAcessarClienteEVeiculo() {
        String token = token(RoleEnum.ROLE_ADMINISTRADOR);

        RestAssured.given().header("Authorization", "Bearer " + token)
                .when().get("/cliente")
                .then().statusCode(200);

        RestAssured.given().header("Authorization", "Bearer " + token)
                .when().get("/veiculo")
                .then().statusCode(200);
    }

    @Test
    void shouldReturn403ParaPerfisNaoAdministrador() {
        for (RoleEnum role : new RoleEnum[]{RoleEnum.ROLE_ATENDENTE, RoleEnum.ROLE_MECANICO, RoleEnum.ROLE_CLIENTE}) {
            String token = token(role);

            RestAssured.given().header("Authorization", "Bearer " + token)
                    .when().get("/cliente")
                    .then().statusCode(403);

            RestAssured.given().header("Authorization", "Bearer " + token)
                    .when().get("/veiculo")
                    .then().statusCode(403);
        }
    }

    @Test
    void shouldReturn403ParaTokenSemRoles() {
        RestAssured.given().header("Authorization", "Bearer " + token())
                .when().get("/cliente")
                .then().statusCode(403);
    }

    @Test
    void shouldRejeitarTokenAssinadoComOutroSegredo() {
        Algorithm outro = Algorithm.HMAC256("anotherkey1234567890123456789012".getBytes());
        String token = JwtTestFactory.gerar(outro, jwtIssuer, 1L, RoleEnum.ROLE_ADMINISTRADOR);

        RestAssured.given().header("Authorization", "Bearer " + token)
                .when().get("/cliente")
                .then().statusCode(not(equalTo(200)));
    }

    @Test
    void shouldExporSwaggerSobPrefixoGestaoSemAutenticacao() {
        RestAssured.given()
                .when().get("/gestao/v3/api-docs")
                .then().statusCode(200);
    }

    @Test
    void shouldNegarRotasDeOutrosServicos() {
        RestAssured.given().header("Authorization", "Bearer " + token(RoleEnum.ROLE_ADMINISTRADOR))
                .when().get("/peca")
                .then().statusCode(403);
    }
}
