package com.example.account.dto;

import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryTransactionResponse {
    private String accountNumber;
    private TransactionType transactionType;
    private TransactionResultType transactionResultType;
    private String transactionId;
    private Long amount;
    private LocalDateTime transactionAt;

    public static QueryTransactionResponse from(TransactionDto transactionDto) {
        return QueryTransactionResponse.builder()
            .accountNumber(transactionDto.getAccountNumber())
            .transactionType(transactionDto.getTransactionType())
            .transactionResultType(
                transactionDto.getTransactionResultType())
            .transactionId(transactionDto.getTransactionId())
            .amount(transactionDto.getAmount())
            .transactionAt(transactionDto.getTransactionAt())
            .build();

    }
}
