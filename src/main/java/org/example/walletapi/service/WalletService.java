package org.example.walletapi.service;

import org.example.walletapi.dto.WalletOperationRequestDto;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {
    void processOperation(WalletOperationRequestDto request);
    BigDecimal getBalance(UUID walletId);
}
