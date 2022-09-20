package com.example.account.controller;

import com.example.account.dto.AccountInfo;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.service.AccountService;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/account")
    public CreateAccount.Response createAccount(
        @RequestBody @Valid CreateAccount.Request request
    ) {
        return CreateAccount.Response.from(
            accountService.createAccount(
                request.getUserId(),
                request.getInitialBalance()));
    }

    @DeleteMapping("/account")
    public DeleteAccount.Response deleteAccount(
        @RequestBody @Valid DeleteAccount.Request request
    ) {
        return DeleteAccount.Response.from(
            accountService.deleteAccount(
                request.getUserId(),
                request.getAccountNumber()
            )
        );
    }

    @GetMapping("/account")
    public List<AccountInfo> getAccountsByUserId(
        @RequestParam("user_id") Long userId
    ) {
        return accountService.getAccountByUserId(userId)
            .stream().map(accountDto -> AccountInfo.builder()
                .accountNumber(accountDto.getAccountNumber())
                .balance(accountDto.getBalance())
                .build()
            ).collect(Collectors.toList());
    }
}
