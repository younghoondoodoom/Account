package com.example.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    public void createAccountSuccess() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
            .willReturn(Optional.of(Account.builder()
                .accountNumber("1000000012")
                .build()));
        given(accountRepository.save(any()))
            .willReturn(Account.builder()
                .accountUser(user)
                .accountNumber("1000000015")
                .build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    public void createFirstAccountSuccess() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(15L)
            .name("Pobi")
            .build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
            .willReturn(Optional.empty());
        given(accountRepository.save(any()))
            .willReturn(Account.builder()
                .accountUser(user)
                .accountNumber("1000000015")
                .build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    public void createAccount_UserNotFound() throws Exception {
        //given
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("유저 당 최대 계좌는 10개")
    public void createAccount_maxAccountIs10() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(15L)
            .name("Pobi")
            .build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.countAccountByAccountUser(user))
            .willReturn(10);

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10,
            exception.getErrorCode());
    }

    @Test
    public void deleteAccountSuccess() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(
                Account.builder()
                    .accountUser(user)
                    .accountNumber("10000000012")
                    .balance(0L)
                    .build()
            ));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(
            (Account.class));

        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("10000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED,
            captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    public void deleteAccountFailed_UserNotFound() throws Exception {
        //given
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "123456789")
        );

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    public void deleteAccountFailed_AccountNotFound() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1234567890")
        );

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 계좌 해지 실패")
    public void deleteAccountFailed_userUnMatch() throws Exception {
        //given
        AccountUser pobi = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        AccountUser harry = AccountUser.builder()
            .id(13L)
            .name("Harry")
            .build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(harry)
                .balance(0L)
                .accountNumber("1000000012")
                .build()
            ));

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1234567890")
        );

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌의 잔액이 있음 - 계좌 해지 실패")
    public void deleteAccountFailed_balanceNotEmpty() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(user)
                .balance(100L)
                .accountNumber("1000000012")
                .build()
            ));

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1234567890")
        );

        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미 해지된 계좌 - 계좌 해지 실패")
    public void deleteAccountFailed_alreadyUnregistered() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(user)
                .balance(100L)
                .accountNumber("1000000012")
                .accountStatus(AccountStatus.UNREGISTERED)
                .build()
            ));

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1234567890")
        );

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED,
            exception.getErrorCode());
    }

    @Test
    public void successGetAccountByUserId() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        List<Account> accounts = Arrays.asList(
            Account.builder()
                .accountUser(user)
                .accountNumber("1111111111")
                .balance(1000L)
                .build(),
            Account.builder()
                .accountUser(user)
                .accountNumber("2222222222")
                .balance(2000L)
                .build(),
            Account.builder()
                .accountUser(user)
                .accountNumber("3333333333")
                .balance(3000L)
                .build()
        );
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findByAccountUser(any()))
            .willReturn(accounts);

        //when
        List<AccountDto> accountDtos = accountService.getAccountByUserId(
            1L);
        //then
        assertEquals(3, accountDtos.size());
        assertEquals("1111111111", accountDtos.get(0).getAccountNumber());
        assertEquals(1000, accountDtos.get(0).getBalance());
        assertEquals("2222222222", accountDtos.get(1).getAccountNumber());
        assertEquals(2000, accountDtos.get(1).getBalance());
        assertEquals("3333333333", accountDtos.get(2).getAccountNumber());
        assertEquals(3000, accountDtos.get(2).getBalance());
    }

    @Test
    public void failedToGetAccounts() throws Exception {
        //given
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1234567890")
        );

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}