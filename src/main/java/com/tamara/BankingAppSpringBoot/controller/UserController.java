
package com.tamara.BankingAppSpringBoot.controller;

import com.tamara.BankingAppSpringBoot.dto.*;
import com.tamara.BankingAppSpringBoot.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(
        name = "User Operations",
        description = "Endpoints for managing users and bank accounts"
)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Create a new account",
            description = "Registers a new user and generates an account number."
    )
    @ApiResponse(responseCode = "201", description = "Account successfully created")
    @PostMapping("/create")
    public ResponseEntity<BankResponse> createAccount(
            @RequestBody UserRequest userRequest) {

        BankResponse response = userService.createAccount(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<BankResponse> login(@RequestBody LoginDto loginDto) {
        return ResponseEntity.ok(userService.login(loginDto));
    }

    @Operation(summary = "Balance Inquiry")
    @GetMapping("/user/balanceInquiry/{accountNumber}")
    public ResponseEntity<BankResponse> balanceInquiry(
            @PathVariable String accountNumber) {

        InquiryRequest request = new InquiryRequest(accountNumber);
        return ResponseEntity.ok(userService.balanceInquiry(request));
    }

    @Operation(summary = "Name Inquiry")
    @GetMapping("/user/nameInquiry/{accountNumber}")
    public ResponseEntity<String> nameInquiry(
            @PathVariable String accountNumber) {

        InquiryRequest request = new InquiryRequest(accountNumber);
        return ResponseEntity.ok(userService.nameInquiry(request));
    }

    @Operation(summary = "Credit an account")
    @PostMapping("/user/credit")
    public ResponseEntity<BankResponse> creditAccount(
            @RequestBody CreditDebitRequest request) {

        return ResponseEntity.ok(userService.creditAccount(request));
    }

    @Operation(summary = "Debit an account")
    @PostMapping("/user/debit")
    public ResponseEntity<BankResponse> debitAccount(
            @RequestBody CreditDebitRequest request) {

        return ResponseEntity.ok(userService.debitAccount(request));
    }

    @Operation(summary = "Transfer funds")
    @PostMapping("/user/transfer")
    public ResponseEntity<BankResponse> transfer(
            @RequestBody TransferRequest request) {

        return ResponseEntity.ok(userService.transfer(request));
    }
}
