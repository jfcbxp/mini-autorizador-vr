package com.br.vr.miniautorizador.controllers;

import com.br.vr.miniautorizador.exceptions.CardAlreadyExistsException;
import com.br.vr.miniautorizador.exceptions.CardNotFoundException;
import com.br.vr.miniautorizador.records.responses.CreateCardResponse;
import com.br.vr.miniautorizador.services.CardService;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class CardControllerTest {

    private static final String CARD_NUMBER = "6549873025634501";
    private static final String PASSWORD    = "1234";
    private static final String BODY_CARD   = "{\"numeroCartao\":\"" + CARD_NUMBER + "\",\"senha\":\"" + PASSWORD + "\"}";

    @Autowired WebApplicationContext context;
    @MockitoBean CardService cardService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser
    void createCard_success_returns201WithBody() throws Exception {
        when(cardService.createCard(any())).thenReturn(new CreateCardResponse(CARD_NUMBER, PASSWORD));

        mockMvc.perform(post("/cartoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_CARD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCartao").value(CARD_NUMBER))
                .andExpect(jsonPath("$.senha").value(PASSWORD))
                .andExpect(header().exists("Location"));
    }

    @Test
    @WithMockUser
    void createCard_duplicate_returns422WithBody() throws Exception {
        when(cardService.createCard(any()))
                .thenThrow(new CardAlreadyExistsException(new CreateCardResponse(CARD_NUMBER, PASSWORD)));

        mockMvc.perform(post("/cartoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_CARD))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.numeroCartao").value(CARD_NUMBER))
                .andExpect(jsonPath("$.senha").value(PASSWORD));
    }

    @Test
    @WithMockUser
    void createCard_blankFields_returns400() throws Exception {
        mockMvc.perform(post("/cartoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCartao\":\"\",\"senha\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/cartoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_CARD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getBalance_success_returns200() throws Exception {
        when(cardService.getBalance(CARD_NUMBER)).thenReturn(new BigDecimal("500.00"));

        mockMvc.perform(get("/cartoes/{numeroCartao}", CARD_NUMBER))
                .andExpect(status().isOk())
                .andExpect(content().string("500.00"));
    }

    @Test
    @WithMockUser
    void getBalance_notFound_returns404() throws Exception {
        when(cardService.getBalance(CARD_NUMBER)).thenThrow(new CardNotFoundException(CARD_NUMBER));

        mockMvc.perform(get("/cartoes/{numeroCartao}", CARD_NUMBER))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBalance_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/cartoes/{numeroCartao}", CARD_NUMBER))
                .andExpect(status().isUnauthorized());
    }
}
