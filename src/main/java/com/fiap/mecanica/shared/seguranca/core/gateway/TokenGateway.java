package com.fiap.mecanica.shared.seguranca.core.gateway;

import com.fiap.mecanica.shared.seguranca.core.domain.RoleEnum;

import java.util.List;

/**
 * Este serviço só valida o JWT emitido pelo monólito (login) ou pela Lambda (cliente por CPF).
 * Não há geração de token aqui.
 */
public interface TokenGateway {
    Long getUserId();
    List<RoleEnum> getRoles();
}
