package com.br.vr.miniautorizador.controllers;

import com.br.vr.miniautorizador.enums.TransactionError;
import com.br.vr.miniautorizador.exceptions.AuthorizationException;
import com.br.vr.miniautorizador.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TransactionControllerTest {

    private static final String CARD_NUMBER = "6549873025634501";
    private static final String PASSWORD    = "1234";
    private static final String BODY        = "{\"numeroCartao\":\"" + CARD_NUMBER + "\",\"senhaCartao\":\"" + PASSWORD + "\",\"valor\":10.00}";

    @Autowired WebApplicationContext context;
    @MockitoBean TransactionService transactionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser
    void authorize_success_returns201Ok() throws Exception {
        doNothing().when(transactionService).authorize(any());

        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(content().string("OK"));
    }

    @Test
    @WithMockUser
    void authorize_saldoInsuficiente_returns422() throws Exception {
        doThrow(new AuthorizationException(TransactionError.SALDO_INSUFICIENTE))
                .when(transactionService).authorize(any());

        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().is(422))
                .andExpect(content().string(TransactionError.SALDO_INSUFICIENTE.name()));
    }

    @Test
    @WithMockUser
    void authorize_senhaInvalida_returns422() throws Exception {
        doThrow(new AuthorizationException(TransactionError.SENHA_INVALIDA))
                .when(transactionService).authorize(any());

        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().is(422))
                .andExpect(content().string(TransactionError.SENHA_INVALIDA.name()));
    }

    @Test
    @WithMockUser
    void authorize_cartaoInexistente_returns422() throws Exception {
        doThrow(new AuthorizationException(TransactionError.CARTAO_INEXISTENTE))
                .when(transactionService).authorize(any());

        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().is(422))
                .andExpect(content().string(TransactionError.CARTAO_INEXISTENTE.name()));
    }

    @Test
    void authorize_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void authorize_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCartao\":\"\",\"senhaCartao\":\"\",\"valor\":0}"))
                .andExpect(status().isBadRequest());
    }
}
