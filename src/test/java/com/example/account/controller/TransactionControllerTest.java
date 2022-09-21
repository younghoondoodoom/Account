package com.example.account.controller;

import static com.example.account.type.TransactionType.USE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.account.dto.CancelBalance;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.service.TransactionService;
import com.example.account.type.TransactionResultType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void successUseBalance() throws Exception {
        //given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
            .willReturn(TransactionDto.builder()
                .accountNumber("1000000000")
                .transactionAt(LocalDateTime.now())
                .amount(12345L)
                .transactionId("transactionId")
                .transactionResultType(TransactionResultType.S)
                .build());

        //when
        //then
        mockMvc.perform(post("/transaction/use")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UseBalance.Request(1L, "2000000000", 3000L)
                ))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value("1000000000"))
            .andExpect(jsonPath("$.transactionResultType").value("S"))
            .andExpect(jsonPath("$.transactionId").value("transactionId"))
            .andExpect(jsonPath("$.amount").value(12345));
    }

    @Test
    public void successCancelBalance() throws Exception {
        //given
        given(transactionService.cancelBalance(anyString(), anyString(),
            anyLong()))
            .willReturn(TransactionDto.builder()
                .accountNumber("0987654321")
                .transactionAt(LocalDateTime.now())
                .amount(12345L)
                .transactionId("transactionId")
                .transactionResultType(TransactionResultType.S)
                .build());

        //when
        //then
        mockMvc.perform(post("/transaction/cancel")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CancelBalance.Request("transactionId", "2000000000", 3000L)
                ))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value("0987654321"))
            .andExpect(jsonPath("$.transactionResultType").value("S"))
            .andExpect(jsonPath("$.transactionId").value("transactionId"))
            .andExpect(jsonPath("$.amount").value(12345));
    }

    @Test
    public void successQueryTransaction() throws Exception {
        //given
        given(transactionService.queryTransaction(anyString()))
            .willReturn(TransactionDto.builder()
                .accountNumber("0987654321")
                .transactionType(USE)
                .transactionAt(LocalDateTime.now())
                .amount(12345L)
                .transactionId("transactionId")
                .transactionResultType(TransactionResultType.S)
                .build());

        //when
        //then
        mockMvc.perform(get("/transaction/12345"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value("0987654321"))
            .andExpect(jsonPath("$.transactionResultType").value("S"))
            .andExpect(jsonPath("$.transactionId").value("transactionId"))
            .andExpect(jsonPath("$.amount").value(12345))
            .andExpect(jsonPath("$.transactionType").value("USE"));
    }
}