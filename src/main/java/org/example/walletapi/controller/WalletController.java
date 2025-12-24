package org.example.walletapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.walletapi.dto.WalletBalanceResponseDto;
import org.example.walletapi.dto.WalletOperationRequestDto;
import org.example.walletapi.service.WalletServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WalletController {

    private final WalletServiceImpl walletService;

    @PostMapping("/wallet")
    public ResponseEntity<Void> processWalletOperation(
            @Valid @RequestBody WalletOperationRequestDto request) {

        log.debug("Processing wallet operation: {}", request);
        walletService.processOperation(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<WalletBalanceResponseDto> getWalletBalance(
            @PathVariable UUID walletId) {

        log.debug("Getting balance for wallet: {}", walletId);
        var balance = walletService.getBalance(walletId);
        return ResponseEntity.ok(
                WalletBalanceResponseDto.builder()
                        .walletId(walletId)
                        .balance(balance)
                        .build()
        );
    }
}
