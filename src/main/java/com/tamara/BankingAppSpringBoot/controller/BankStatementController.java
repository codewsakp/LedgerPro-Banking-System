
package com.tamara.BankingAppSpringBoot.controller;

import com.itextpdf.text.DocumentException;
import com.tamara.BankingAppSpringBoot.entity.Transaction;
import com.tamara.BankingAppSpringBoot.service.imp.BankStatement;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.util.List;

@RestController
@RequestMapping("/api/statements")
public class BankStatementController {

    private final BankStatement bankStatement;

    public BankStatementController(BankStatement bankStatement) {
        this.bankStatement = bankStatement;
    }

    @GetMapping("/{accountNumber}")
    public List<Transaction> generateStatement(
            @PathVariable String accountNumber,
            @RequestParam String startDate,
            @RequestParam String endDate) throws DocumentException, FileNotFoundException {

        return bankStatement.generateStatement(
                accountNumber,
                startDate,
                endDate
        );
    }
}
