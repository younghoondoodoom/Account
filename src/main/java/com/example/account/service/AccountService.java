package com.example.account.service;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.AccountStatus.UNREGISTERED;
import static com.example.account.type.ErrorCode.ACCOUNT_ALREADY_UNREGISTERED;
import static com.example.account.type.ErrorCode.ACCOUNT_NOT_FOUND;
import static com.example.account.type.ErrorCode.BALANCE_NOT_EMPTY;
import static com.example.account.type.ErrorCode.MAX_ACCOUNT_PER_USER_10;
import static com.example.account.type.ErrorCode.USER_ACCOUNT_UN_MATCH;
import static com.example.account.type.ErrorCode.USER_NOT_FOUND;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /**
     * 사용자가 있는지 조회 계좌의 번호를 생성하고 계좌를 저장하고, 그 정보를 넘긴다.
     *
     * @param userId
     * @param initialBalance
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = accountUserRepository.findById(userId)
            .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        validateCreateAccount(accountUser);

        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
            .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1
                + "")
            .orElse("1000000000");

        return AccountDto.fromEntity(
            accountRepository.save(
                Account.builder()
                    .accountUser(accountUser)
                    .accountStatus(IN_USE)
                    .accountNumber(newAccountNumber)
                    .balance(initialBalance)
                    .registeredAt(LocalDateTime.now())
                    .build()));
    }

    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countAccountByAccountUser(accountUser) >= 10) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10);
        }
    }

    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = accountUserRepository.findById(userId)
            .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(
                () -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(UNREGISTERED);
        account.setUnregisteredAt(LocalDateTime.now());

        accountRepository.save(account); // 테스트를 위해서 넣음. 좋은 코드는 아님.

        return AccountDto.fromEntity(account);
    }

    private void validateDeleteAccount(AccountUser accountUser,
        Account account) {
        if (!accountUser.getId().equals(account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() == UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }
    }

    @Transactional
    public List<AccountDto> getAccountByUserId(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
            .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        List<Account> accounts = accountRepository.findByAccountUser(
            accountUser);

        return accounts.stream()
            .map(AccountDto::fromEntity)
            .collect(Collectors.toList());
    }

}
