package com.example.account.service;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.ErrorCode.CANCEL_MUST_FULLY;
import static com.example.account.type.ErrorCode.TOO_OLD_ORDER_TO_CANCEL;
import static com.example.account.type.ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
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
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    public static final long USE_AMOUNT = 200L;
    public static final long CANCEL_AMOUNT = 200L;
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    public void successUseBalance() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012")
            .build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
            .willReturn(Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactionAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(
            Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.useBalance(1L,
            "1000000000", USE_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 사용 실패")
    public void useBalanceFailed_UserNotFound() throws Exception {
        //given
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 사용 실패")
    public void useBalanceFailed_AccountNotFound() throws Exception {
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
            () -> transactionService.useBalance(1L, "1000000000", 1000L)
        );

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 계좌 사용 실패")
    public void useBalanceFailed_userUnMatch() throws Exception {
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
            () -> transactionService.useBalance(1L, "1234567890", 1000L)
        );

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미 해지된 계좌 - 계좌 사용 실패")
    public void useBalanceFailed_alreadyUnregistered() throws Exception {
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
            () -> transactionService.useBalance(1L, "1234567890", 1000L)
        );

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED,
            exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우")
    public void exceedAmount_UseBalance() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(100L)
            .accountNumber("1000000012")
            .build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.useBalance(1L, "1234567890", USE_AMOUNT)
        );

        //then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    public void saveFailedUseTransaction() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012")
            .build();
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
            .willReturn(Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactionAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(
            Transaction.class);

        //when
        transactionService.saveFailedUseTransaction("1000000000", USE_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(F, captor.getValue().getTransactionResultType());
    }

    @Test
    public void successCancelBalance() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        Account account = Account.builder()
            .id(1L)
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012")
            .build();

        Transaction transaction = Transaction.builder()
            .account(account)
            .transactionType(USE)
            .transactionResultType(S)
            .transactionId("transactionId")
            .transactionAt(LocalDateTime.now())
            .amount(CANCEL_AMOUNT)
            .balanceSnapshot(9000L)
            .build();

        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
            .willReturn(Transaction.builder()
                .account(account)
                .transactionType(CANCEL)
                .transactionResultType(S)
                .transactionId("transactionIdForCancel")
                .transactionAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(10000L)
                .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(
            Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.cancelBalance(
            "transactionId",
            "1000000000", CANCEL_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L + CANCEL_AMOUNT,
            captor.getValue().getBalanceSnapshot());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
    public void cancelTransactionFailed_AccountNotFound() throws Exception {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(Transaction.builder().build()));

        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.cancelBalance("transactionId",
                "1000000000", 1000L)
        );

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 거래 없음 - 잔액 사용 취소 실패")
    public void cancelTransactionFailed_TransactionNotFound() throws Exception {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.cancelBalance("transactionId",
                "1000000000", 1000L)
        );

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래와 계좌가 매칭실패 - 잔액 사용 취소 실패")
    public void cancelTransactionFailed_TransactionAccountUnMatch()
        throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        Account account = Account.builder()
            .id(1L)
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012")
            .build();
        Account accountNotUse = Account.builder()
            .id(2L)
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000013")
            .build();
        Transaction transaction = Transaction.builder()
            .account(account)
            .transactionType(USE)
            .transactionResultType(S)
            .transactionId("transactionId")
            .transactionAt(LocalDateTime.now())
            .amount(CANCEL_AMOUNT)
            .balanceSnapshot(9000L)
            .build();
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(accountNotUse));
        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.cancelBalance("transactionId",
                "1000000000", CANCEL_AMOUNT)
        );

        //then
        assertEquals(TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래금액과 취소 금액이 다 - 잔액 사용 취소 실패")
    public void cancelTransactionFailed_CancelMustFully()
        throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        Account account = Account.builder()
            .id(1L)
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012")
            .build();
        Transaction transaction = Transaction.builder()
            .account(account)
            .transactionType(USE)
            .transactionResultType(S)
            .transactionId("transactionId")
            .transactionAt(LocalDateTime.now())
            .amount(CANCEL_AMOUNT + 1000L)
            .balanceSnapshot(9000L)
            .build();
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));
        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.cancelBalance("transactionId",
                "1000000000", CANCEL_AMOUNT)
        );

        //then
        assertEquals(CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("취소는 1년까지만 가능합니다 - 잔액 사용 취소 실패")
    public void cancelTransactionFailed_TooOldeOrderToCancel()
        throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        Account account = Account.builder()
            .id(1L)
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012")
            .build();
        Transaction transaction = Transaction.builder()
            .account(account)
            .transactionType(USE)
            .transactionResultType(S)
            .transactionId("transactionId")
            .transactionAt(LocalDateTime.now().minusYears(1).minusDays(1))
            .amount(CANCEL_AMOUNT)
            .balanceSnapshot(9000L)
            .build();
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));
        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.cancelBalance("transactionId",
                "1000000000", CANCEL_AMOUNT)
        );

        //then
        assertEquals(TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }

    @Test
    public void successQueryTransaction() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi")
            .build();
        Account account = Account.builder()
            .id(1L)
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012")
            .build();
        Transaction transaction = Transaction.builder()
            .account(account)
            .transactionType(USE)
            .transactionResultType(S)
            .transactionId("transactionId")
            .transactionAt(LocalDateTime.now().minusYears(1).minusDays(1))
            .amount(CANCEL_AMOUNT)
            .balanceSnapshot(9000L)
            .build();
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));

        //when
        transactionService.queryTransaction(("trxId"));

        //then
        assertEquals(USE, transaction.getTransactionType());
        assertEquals(S, transaction.getTransactionResultType());
        assertEquals(CANCEL_AMOUNT, transaction.getAmount());
        assertEquals("transactionId", transaction.getTransactionId());
    }

    @Test
    @DisplayName("해당 거래 없음 - 거래 조 실패")
    public void queryTransactionFailed_TransactionNotFound() throws Exception {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.queryTransaction("transactionId")
        );

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
}