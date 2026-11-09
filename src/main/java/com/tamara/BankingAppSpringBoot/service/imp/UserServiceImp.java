
package com.tamara.BankingAppSpringBoot.service.imp;

import com.tamara.BankingAppSpringBoot.config.JwtUtil;
import com.tamara.BankingAppSpringBoot.dto.*;
import com.tamara.BankingAppSpringBoot.entity.Role;
import com.tamara.BankingAppSpringBoot.entity.User;
import com.tamara.BankingAppSpringBoot.repo.UserRepository;
import com.tamara.BankingAppSpringBoot.service.EmailService;
import com.tamara.BankingAppSpringBoot.service.TransactionService;
import com.tamara.BankingAppSpringBoot.service.UserService;
import com.tamara.BankingAppSpringBoot.utils.AccountUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class UserServiceImp implements UserService {

    private static final Logger LOGGER =
            Logger.getLogger(UserServiceImp.class.getName());

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TransactionService transactionService;

    @Autowired
    EmailService emailService;


    // CREATE ACCOUNT

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankResponse createAccount(UserRequest userRequest) {

        if (userRepository.existsByEmail(userRequest.getEmail())) {
            return BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_EXISTS_MESSAGE)
                    .accountInfo(null)
                    .build();
        }

        User newUser = User.builder()
                .firstName(userRequest.getFirstName())
                .lastName(userRequest.getLastName())
                .otherName(userRequest.getOtherName())
                .gender(userRequest.getGender())
                .address(userRequest.getAddress())
                .stateOfOrigin(userRequest.getStateOfOrigin())
                .accountNumber(AccountUtils.generateAccountNumber())
                .accountBalance(BigDecimal.ZERO)
                .email(userRequest.getEmail())
                .role(Role.ROLE_USER)
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .phoneNumber(userRequest.getPhoneNumber())
                .alternativeNumber(userRequest.getAlternativeNumber())
                .status("ACTIVE")
                .build();

        User savedUser = userRepository.save(newUser);

        EmailDetails emailDetails = EmailDetails.builder()
                .recipient(savedUser.getEmail())
                .subject("Welcome! Your New Bank Account Details")
                .messageBody(
                        "Dear " + savedUser.getFirstName() + ",\n\n" +
                        "Your bank account has been successfully created.\n\n" +
                        "Account Name: " + savedUser.getFirstName() + " " +
                        savedUser.getLastName() + " " +
                        savedUser.getOtherName() + "\n" +
                        "Account Number: " + savedUser.getAccountNumber() + "\n\n" +
                        "Thank you for choosing our banking services."
                )
                .build();

        // Send welcome email only after successful database commit.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            emailService.sendEmailAlerts(emailDetails);
                        } catch (RuntimeException exception) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Account created, but welcome email could not be sent.",
                                    exception
                            );
                        }
                    }
                }
        );

        return BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_CREATION_SUCCESS)
                .responseMessage(AccountUtils.ACCOUNT_CREATION_MESSAGE)
                .accountInfo(AccountInfo.builder()
                        .accountBalance(savedUser.getAccountBalance())
                        .accountNumber(savedUser.getAccountNumber())
                        .accountName(savedUser.getFirstName() + " " +
                                savedUser.getLastName() + " " +
                                savedUser.getOtherName())
                        .build())
                .build();
    }


    // LOGIN

    @Override
    public BankResponse login(LoginDto loginDto) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginDto.getEmail(),
                                loginDto.getPassword()
                        )
                );

        EmailDetails loginAlert = EmailDetails.builder()
                .subject("You're logged in!")
                .recipient(loginDto.getEmail())
                .messageBody(
                        "You logged into your Bank account. " +
                        "If you did not initiate this request, " +
                        "please contact your bank."
                )
                .build();

        try {
            emailService.sendEmailAlerts(loginAlert);
        } catch (RuntimeException exception) {
            LOGGER.log(
                    Level.WARNING,
                    "Login successful, but login notification email could not be sent.",
                    exception
            );
        }

        return BankResponse.builder()
                .responseCode("Login Success")
                .responseMessage(jwtUtil.generateToken(authentication))
                .build();
    }


    // BALANCE INQUIRY

    @Override
    public BankResponse balanceInquiry(InquiryRequest inquiryRequest) {

        boolean isAccountExist =
                userRepository.existsByAccountNumber(
                        inquiryRequest.getAccountNumber()
                );

        if (!isAccountExist) {
            return BankResponse.builder()
                    .responseCode(AccountUtils.ACCOUNT_NOT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_NOT_EXISTS_MESSAGE)
                    .accountInfo(null)
                    .build();
        }

        User foundUser =
                userRepository.findByAccountNumber(
                        inquiryRequest.getAccountNumber()
                );

        return BankResponse.builder()
                .responseMessage(AccountUtils.ACCOUNT_FOUND_SUCCESS)
                .responseCode(AccountUtils.ACCOUNT_FOUND_CODE)
                .accountInfo(buildAccountInfo(foundUser))
                .build();
    }


    // NAME INQUIRY

    @Override
    public String nameInquiry(InquiryRequest inquiryRequest) {

        boolean isAccountExist =
                userRepository.existsByAccountNumber(
                        inquiryRequest.getAccountNumber()
                );

        if (!isAccountExist) {
            return AccountUtils.ACCOUNT_NOT_EXISTS_MESSAGE;
        }

        User foundUser =
                userRepository.findByAccountNumber(
                        inquiryRequest.getAccountNumber()
                );

        return foundUser.getFirstName() + " " +
                foundUser.getLastName() + " " +
                foundUser.getOtherName();
    }


    // CREDIT ACCOUNT

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankResponse creditAccount(CreditDebitRequest creditDebitRequest) {

        if (creditDebitRequest == null ||
                creditDebitRequest.getAccountNumber() == null ||
                creditDebitRequest.getAccountNumber().isBlank()) {
            return accountNotFoundResponse();
        }

        if (isInvalidAmount(creditDebitRequest.getAmount())) {
            return invalidAmountResponse();
        }

        User userToCredit =
                userRepository.findByAccountNumberForUpdate(
                        creditDebitRequest.getAccountNumber()
                );

        if (userToCredit == null) {
            return accountNotFoundResponse();
        }

        userToCredit.setAccountBalance(
                userToCredit.getAccountBalance()
                        .add(creditDebitRequest.getAmount())
        );

        userRepository.save(userToCredit);

        TransactionDto transactionDto = TransactionDto.builder()
                .transactionType("CREDIT")
                .amount(creditDebitRequest.getAmount())
                .accountNumber(userToCredit.getAccountNumber())
                .build();

        transactionService.saveTransaction(transactionDto);

        return BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_CREDITED_SUCCESS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_CREDITED_SUCCESS_MESSAGE)
                .accountInfo(buildAccountInfo(userToCredit))
                .build();
    }


    // DEBIT ACCOUNT

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankResponse debitAccount(CreditDebitRequest creditDebitRequest) {

        if (creditDebitRequest == null ||
                creditDebitRequest.getAccountNumber() == null ||
                creditDebitRequest.getAccountNumber().isBlank()) {
            return accountNotFoundResponse();
        }

        if (isInvalidAmount(creditDebitRequest.getAmount())) {
            return invalidAmountResponse();
        }

        User userToDebit =
                userRepository.findByAccountNumberForUpdate(
                        creditDebitRequest.getAccountNumber()
                );

        if (userToDebit == null) {
            return accountNotFoundResponse();
        }

        if (userToDebit.getAccountBalance()
                .compareTo(creditDebitRequest.getAmount()) < 0) {

            return BankResponse.builder()
                    .responseCode(AccountUtils.INSUFFICIENT_BALANCE_CODE)
                    .responseMessage(AccountUtils.INSUFFICIENT_BALANCE_MESSAGE)
                    .accountInfo(buildAccountInfo(userToDebit))
                    .build();
        }

        userToDebit.setAccountBalance(
                userToDebit.getAccountBalance()
                        .subtract(creditDebitRequest.getAmount())
        );

        userRepository.save(userToDebit);

        TransactionDto transactionDto = TransactionDto.builder()
                .transactionType("DEBIT")
                .amount(creditDebitRequest.getAmount())
                .accountNumber(userToDebit.getAccountNumber())
                .build();

        transactionService.saveTransaction(transactionDto);

        return BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_DEBITED_SUCCESS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_DEBITED_SUCCESS_MESSAGE)
                .accountInfo(buildAccountInfo(userToDebit))
                .build();
    }


    // TRANSFER

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankResponse transfer(TransferRequest transferRequest) {

        if (transferRequest == null) {
            return accountNotFoundResponse();
        }

        BigDecimal amount = transferRequest.getAmount();

        if (isInvalidAmount(amount)) {
            return invalidAmountResponse();
        }

        String sourceNumber = transferRequest.getSourceAccountNumber();
        String destinationNumber =
                transferRequest.getDestinationAccountNumber();

        if (sourceNumber == null || sourceNumber.isBlank()
                || destinationNumber == null || destinationNumber.isBlank()) {
            return accountNotFoundResponse();
        }

        if (Objects.equals(sourceNumber, destinationNumber)) {
            return BankResponse.builder()
                    .responseCode("010")
                    .responseMessage(
                            "Source and destination accounts cannot be the same."
                    )
                    .accountInfo(null)
                    .build();
        }

        User sourceAccountUser;
        User destinationAccountUser;

        // Lock accounts in a consistent order to reduce deadlock risk.
        if (sourceNumber.compareTo(destinationNumber) < 0) {

            sourceAccountUser =
                    userRepository.findByAccountNumberForUpdate(sourceNumber);

            destinationAccountUser =
                    userRepository.findByAccountNumberForUpdate(destinationNumber);

        } else {

            destinationAccountUser =
                    userRepository.findByAccountNumberForUpdate(destinationNumber);

            sourceAccountUser =
                    userRepository.findByAccountNumberForUpdate(sourceNumber);
        }

        if (sourceAccountUser == null) {
            return accountNotFoundResponse();
        }

        if (destinationAccountUser == null) {
            return accountNotFoundResponse();
        }

        if (sourceAccountUser.getAccountBalance().compareTo(amount) < 0) {
            return BankResponse.builder()
                    .responseCode(AccountUtils.INSUFFICIENT_BALANCE_CODE)
                    .responseMessage(AccountUtils.INSUFFICIENT_BALANCE_MESSAGE)
                    .accountInfo(buildAccountInfo(sourceAccountUser))
                    .build();
        }

        // Update both account balances.
        sourceAccountUser.setAccountBalance(
                sourceAccountUser.getAccountBalance().subtract(amount)
        );

        destinationAccountUser.setAccountBalance(
                destinationAccountUser.getAccountBalance().add(amount)
        );

        userRepository.save(sourceAccountUser);
        userRepository.save(destinationAccountUser);

        // Record debit transaction for source account.
        TransactionDto debitTransaction = TransactionDto.builder()
                .transactionType("TRANSFER_DEBIT")
                .amount(amount)
                .accountNumber(sourceAccountUser.getAccountNumber())
                .destinationAccountNumber(destinationAccountUser.getAccountNumber())
                .build();

        // Record credit transaction for destination account.
        TransactionDto creditTransaction = TransactionDto.builder()
                .transactionType("TRANSFER_CREDIT")
                .amount(amount)
                .accountNumber(destinationAccountUser.getAccountNumber())
                .destinationAccountNumber(sourceAccountUser.getAccountNumber())
                .build();

        transactionService.saveTransaction(debitTransaction);
        transactionService.saveTransaction(creditTransaction);

        // Send transfer notifications only after successful commit.
        registerTransferEmailNotifications(
                sourceAccountUser,
                destinationAccountUser,
                amount
        );

        return BankResponse.builder()
                .responseCode(AccountUtils.TRANSFER_SUCCESS_CODE)
                .responseMessage(AccountUtils.TRANSFER_SUCCESS_MESSAGE)
                .accountInfo(buildAccountInfo(sourceAccountUser))
                .build();
    }


    // HELPER METHODS

    private boolean isInvalidAmount(BigDecimal amount) {
        return amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0;
    }

    private BankResponse invalidAmountResponse() {
        return BankResponse.builder()
                .responseCode("009")
                .responseMessage("Amount must be greater than zero.")
                .accountInfo(null)
                .build();
    }

    private BankResponse accountNotFoundResponse() {
        return BankResponse.builder()
                .responseCode(AccountUtils.ACCOUNT_NOT_EXISTS_CODE)
                .responseMessage(AccountUtils.ACCOUNT_NOT_EXISTS_MESSAGE)
                .accountInfo(null)
                .build();
    }

    private AccountInfo buildAccountInfo(User user) {
        return AccountInfo.builder()
                .accountName(user.getFirstName() + " " +
                        user.getLastName() + " " +
                        user.getOtherName())
                .accountNumber(user.getAccountNumber())
                .accountBalance(user.getAccountBalance())
                .build();
    }


    // TRANSFER EMAIL NOTIFICATIONS

    private void registerTransferEmailNotifications(
            User source,
            User destination,
            BigDecimal amount) {

        EmailDetails debitAlert = EmailDetails.builder()
                .recipient(source.getEmail())
                .subject("Transfer Debit Alert")
                .messageBody(
                        "Dear " + source.getFirstName() + ",\n\n" +
                        "You have transferred " + amount +
                        " to account " + destination.getAccountNumber() + ".\n" +
                        "Your new balance is " + source.getAccountBalance() + "."
                )
                .build();

        EmailDetails creditAlert = EmailDetails.builder()
                .recipient(destination.getEmail())
                .subject("Transfer Credit Alert")
                .messageBody(
                        "Dear " + destination.getFirstName() + ",\n\n" +
                        "Your account has been credited with " + amount +
                        " from account " + source.getAccountNumber() + ".\n" +
                        "Your new balance is " + destination.getAccountBalance() + "."
                )
                .build();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        try {
                            emailService.sendEmailAlerts(debitAlert);
                        } catch (RuntimeException exception) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Failed to send transfer debit alert.",
                                    exception
                            );
                        }

                        try {
                            emailService.sendEmailAlerts(creditAlert);
                        } catch (RuntimeException exception) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Failed to send transfer credit alert.",
                                    exception
                            );
                        }
                    }
                }
        );
    }
}
