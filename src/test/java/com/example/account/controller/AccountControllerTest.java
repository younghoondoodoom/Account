package com.example.account.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.account.dto.AccountDto;
import com.example.account.dto.CreateAccount.Request;
import com.example.account.dto.DeleteAccount;
import com.example.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @MockBean
    private AccountService accountService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void successCreateAccount() throws Exception {
        //given
        given(accountService.createAccount(anyLong(), anyLong()))
            .willReturn(AccountDto.builder()
                .userId(1L)
                .accountNumber("1234567890")
                .registeredAt(LocalDateTime.now())
                .unregisteredAt(LocalDateTime.now())
                .build());

        //when
        //then
        mockMvc.perform(post("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new Request(1L, 100L)
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.accountNumber").value("1234567890"))
            .andDo(print());
    }

    @Test
    public void successDeleteAccount() throws Exception {
        //given
        given(accountService.deleteAccount(anyLong(), anyString()))
            .willReturn(AccountDto.builder()
                .userId(1L)
                .accountNumber("1234567890")
                .registeredAt(LocalDateTime.now())
                .unregisteredAt(LocalDateTime.now())
                .build());

        //when
        //then
        mockMvc.perform(delete("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new DeleteAccount.Request(333L, "0987654321")
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.accountNumber").value("1234567890"))
            .andDo(print());
    }

    @Test
    public void successGetAccountByUserId() throws Exception {
        //given
        List<AccountDto> accountDtos =
            Arrays.asList(
                AccountDto.builder()
                    .accountNumber("1234567890")
                    .balance(1000L)
                    .build(),
                AccountDto.builder()
                    .accountNumber("1111111111")
                    .balance(2000L)
                    .build(),
                AccountDto.builder()
                    .accountNumber("2222222222")
                    .balance(3000L)
                    .build()
            );
        given(accountService.getAccountByUserId(anyLong()))
            .willReturn(accountDtos);

        //when
        //then
        mockMvc.perform(get("/account?user_id=1"))
            .andExpect(jsonPath("$[0].accountNumber").value("1234567890"))
            .andExpect(jsonPath("$[0].balance").value(1000))
            .andExpect(jsonPath("$[1].accountNumber").value("1111111111"))
            .andExpect(jsonPath("$[1].balance").value(2000))
            .andExpect(jsonPath("$[2].accountNumber").value("2222222222"))
            .andExpect(jsonPath("$[2].balance").value(3000))
            .andDo(print());
    }
}