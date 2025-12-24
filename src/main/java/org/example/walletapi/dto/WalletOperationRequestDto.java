package org.example.walletapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletOperationRequestDto {
    @NotNull(message = "walletId must not be null")
    private UUID walletId;

    @NotNull(message = "operationType must not be null")
    private OperationType operationType;

    @NotNull(message = "amount must not be null")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    public enum OperationType {
        DEPOSIT,
        WITHDRAW
    }
}
