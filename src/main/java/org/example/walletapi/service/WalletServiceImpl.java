package org.example.walletapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.walletapi.dto.WalletOperationRequestDto;
import org.example.walletapi.entity.Wallet;
import org.example.walletapi.exception.InsufficientFundsException;
import org.example.walletapi.exception.WalletNotFoundException;
import org.example.walletapi.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final DistributedLockService lockService;

    @Override
    @Transactional
    public void processOperation(WalletOperationRequestDto request) {
        UUID walletId = request.getWalletId();

        if (!lockService.tryLock(walletId)) {
            throw new RuntimeException("Could not acquire lock for wallet: " + walletId);
        }

        try {
            Optional<Wallet> walletOpt = walletRepository.findByIdForUpdate(walletId);

            switch (request.getOperationType()) {
                case DEPOSIT:
                    handleDeposit(walletOpt, walletId, request.getAmount());
                    break;
                case WITHDRAW:
                    handleWithdraw(walletOpt, walletId, request.getAmount());
                    break;
            }
        } finally {
            lockService.unlock(walletId);
        }
    }

    private void handleDeposit(Optional<Wallet> walletOpt, UUID walletId, BigDecimal amount) {
        Wallet wallet = walletOpt.orElseGet(() -> createWallet(walletId));
        BigDecimal newBalance = wallet.getBalance().add(amount);
        updateWalletBalance(wallet, newBalance);
        logDeposit(walletId, amount, newBalance);
    }

    private void handleWithdraw(Optional<Wallet> walletOpt, UUID walletId, BigDecimal amount) {
        Wallet wallet = walletOpt.orElseThrow(() ->
                new WalletNotFoundException("Wallet not found: " + walletId));
        validateBalance(wallet, amount);
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        updateWalletBalance(wallet, newBalance);
        logWithdrawal(walletId, amount, newBalance);
    }

    private Wallet createWallet(UUID walletId) {
        Wallet wallet = Wallet.builder()
                .id(walletId)
                .balance(BigDecimal.ZERO)
                .build();
        return walletRepository.save(wallet);
    }

    private void validateBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Current balance: %s, requested: %s",
                            wallet.getBalance(), amount)
            );
        }
    }

    private void updateWalletBalance(Wallet wallet, BigDecimal newBalance) {
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
    }

    private void logDeposit(UUID walletId, BigDecimal amount, BigDecimal newBalance) {
        log.info("Deposited {} to wallet {}. New balance: {}", amount, walletId, newBalance);
    }

    private void logWithdrawal(UUID walletId, BigDecimal amount, BigDecimal newBalance) {
        log.info("Withdrawn {} from wallet {}. New balance: {}", amount, walletId, newBalance);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID walletId) {
        return walletRepository.findById(walletId)
                .map(Wallet::getBalance)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
    }
}
