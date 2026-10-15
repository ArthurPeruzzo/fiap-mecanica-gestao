package com.fiap.mecanica.gestao.infra.controller;

import com.fiap.mecanica.gestao.infra.gateway.entity.ClienteEntity;
import com.fiap.mecanica.gestao.infra.gateway.repository.ClienteRepository;
import com.auth0.jwt.algorithms.Algorithm;
import com.fiap.mecanica.resources.JwtTestFactory;
import com.fiap.mecanica.resources.testcontainer.AbstractContainer;
import com.fiap.mecanica.shared.seguranca.core.domain.RoleEnum;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClienteIntegrationTest extends AbstractContainer {

	@Autowired
	private Algorithm algorithm;

	@Autowired
	private String jwtIssuer;

	@Autowired
	private ClienteRepository clienteRepository;

	@LocalServerPort
	private int port;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
	}

	private String obterToken() {
		return JwtTestFactory.gerar(algorithm, jwtIssuer, 1L, RoleEnum.ROLE_ADMINISTRADOR);
	}

	@Test
	void shouldCreateClienteSuccessFully() {
		String token = obterToken();

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.body("""
						{
						  "nome": "Pedro",
						  "cpf": "188.254.690-39"
						}
						""")
				.when()
				.post("/cliente")
				.then()
				.statusCode(201);

		List<ClienteEntity> allClientes = clienteRepository.findAll();

		Assertions.assertFalse(allClientes.isEmpty());
		Assertions.assertEquals(1, allClientes.size());
	}

	@Test
	void shouldReturn409WhenClienteWithSameCpfAlreadyExists() {
		String token = obterToken();

		String body = """
				{
				  "nome": "Pedro",
				  "cpf": "975.730.520-06"
				}
				""";

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.body(body)
				.when()
				.post("/cliente")
				.then()
				.statusCode(201);

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.body(body)
				.when()
				.post("/cliente")
				.then()
				.statusCode(409)
				.body("message", Matchers.equalTo("Já existe um cliente cadastrado com o documento informado"));
	}

	@Test
	void shouldReturn409WhenClienteWithSameCnpjAlreadyExists() {
		String token = obterToken();

		String body = """
				{
				  "nome": "Empresa",
				  "cnpj": "D4.779.442/0001-21"
				}
				""";

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.body(body)
				.when()
				.post("/cliente")
				.then()
				.statusCode(201);

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.body(body)
				.when()
				.post("/cliente")
				.then()
				.statusCode(409)
				.body("message", Matchers.equalTo("Já existe um cliente cadastrado com o documento informado"));
	}

	@Test
	void shouldReturnEmptyPageWhenNoClientesCadastrados() {
		String token = obterToken();

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.queryParam("page", 0)
				.queryParam("size", 10)
				.when()
				.get("/cliente")
				.then()
				.statusCode(200)
				.body("content", Matchers.hasSize(0))
				.body("totalElements", Matchers.equalTo(0))
				.body("totalPages", Matchers.equalTo(0))
				.body("page", Matchers.equalTo(0))
				.body("size", Matchers.equalTo(10));
	}

	@Test
	void shouldReturnClientesPagedAfterCreation() {
		String token = obterToken();

		String cpf1 = """
				{"nome":"Pedro","cpf":"909.815.060-89"}
				""";
		String cpf2 = """
				{"nome":"Ana","cpf":"859.886.590-71"}
				""";

		RestAssured.given().contentType("application/json").header("Authorization", "Bearer " + token)
				.body(cpf1).when().post("/cliente").then().statusCode(201);
		RestAssured.given().contentType("application/json").header("Authorization", "Bearer " + token)
				.body(cpf2).when().post("/cliente").then().statusCode(201);

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.queryParam("page", 0)
				.queryParam("size", 10)
				.when()
				.get("/cliente")
				.then()
				.statusCode(200)
				.body("content", Matchers.hasSize(2))
				.body("totalElements", Matchers.equalTo(2))
				.body("totalPages", Matchers.equalTo(1))
				.body("content[0].nome", Matchers.notNullValue())
				.body("content[0].cpf", Matchers.notNullValue());
	}

	@Test
	void shouldRespectPageSizeInPagination() {
		String token = obterToken();

		String cpf1 = """
				{"nome":"Pedro","cpf":"909.815.060-89"}
				""";
		String cpf2 = """
				{"nome":"Ana","cpf":"859.886.590-71"}
				""";

		RestAssured.given().contentType("application/json").header("Authorization", "Bearer " + token)
				.body(cpf1).when().post("/cliente").then().statusCode(201);
		RestAssured.given().contentType("application/json").header("Authorization", "Bearer " + token)
				.body(cpf2).when().post("/cliente").then().statusCode(201);

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.queryParam("page", 0)
				.queryParam("size", 1)
				.when()
				.get("/cliente")
				.then()
				.statusCode(200)
				.body("content", Matchers.hasSize(1))
				.body("totalElements", Matchers.equalTo(2))
				.body("totalPages", Matchers.equalTo(2))
				.body("size", Matchers.equalTo(1));
	}

	@Test
	void shouldUpdateClienteSuccessfully() {
		String token = obterToken();

		String createBody = """
				{"nome":"Pedro","cpf":"188.254.690-39"}
				""";

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.body(createBody)
				.when()
				.post("/cliente")
				.then()
				.statusCode(201);

		Long id = clienteRepository.findAll().getFirst().getId();

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.body("""
						{"nome":"Carlos","cpf":"951.147.520-73"}
						""")
				.when()
				.put("/cliente/" + id)
				.then()
				.statusCode(204);

		var updated = clienteRepository.findById(id).orElseThrow();
		Assertions.assertEquals("Carlos", updated.getNome());
		Assertions.assertEquals("95114752073", updated.getCpf());
	}

	@Test
	void shouldReturn404WhenClienteNotFoundOnUpdate() {
		String token = obterToken();

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.body("""
						{"nome":"Pedro","cpf":"951.147.520-73"}
						""")
				.when()
				.put("/cliente/9999")
				.then()
				.statusCode(404)
				.body("message", Matchers.equalTo("Cliente não encontrado"));
	}

	@Test
	void shouldReturn409WhenUpdatingToExistingCpf() {
		String token = obterToken();

		RestAssured.given().contentType("application/json").header("Authorization", "Bearer " + token)
				.body("""
						{"nome":"Pedro","cpf":"188.254.690-39"}
						""")
				.when().post("/cliente").then().statusCode(201);

		RestAssured.given().contentType("application/json").header("Authorization", "Bearer " + token)
				.body("""
						{"nome":"Ana","cpf":"951.147.520-73"}
						""")
				.when().post("/cliente").then().statusCode(201);

		Long secondId = clienteRepository.findAll().stream()
				.filter(c -> "95114752073".equals(c.getCpf()))
				.findFirst().orElseThrow().getId();

		RestAssured
				.given()
				.contentType("application/json")
				.header("Authorization", "Bearer " + token)
				.body("""
						{"nome":"Ana","cpf":"188.254.690-39"}
						""")
				.when()
				.put("/cliente/" + secondId)
				.then()
				.statusCode(409)
				.body("message", Matchers.equalTo("Já existe um cliente cadastrado com o documento informado"));
	}

	@Test
	void shouldDeleteClienteSuccessfully() {
		String token = obterToken();

		RestAssured.given().contentType("application/json").header("Authorization", "Bearer " + token)
				.body("""
						{"nome":"Pedro","cpf":"188.254.690-39"}
						""")
				.when().post("/cliente").then().statusCode(201);

		Long id = clienteRepository.findAll().getFirst().getId();

		RestAssured
				.given()
				.header("Authorization", "Bearer " + token)
				.when()
				.delete("/cliente/" + id)
				.then()
				.statusCode(204);

		Assertions.assertTrue(clienteRepository.findById(id).isEmpty());
	}

	@Test
	void shouldReturn404WhenClienteNotFoundOnDelete() {
		String token = obterToken();

		RestAssured
				.given()
				.header("Authorization", "Bearer " + token)
				.when()
				.delete("/cliente/9999")
				.then()
				.statusCode(404)
				.body("message", Matchers.equalTo("Cliente não encontrado"));
	}

	@Test
	void shouldReturn401WhenNoTokenProvided() {
		RestAssured
				.given()
				.contentType("application/json")
				.body("""
						{
						  "nome": "Pedro",
						  "cpf": "188.254.690-39"
						}
						""")
				.when()
				.post("/cliente")
				.then()
				.statusCode(401);
	}
}
