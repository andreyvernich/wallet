package org.example.walletapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletBalanceResponseDto {
    @JsonProperty("walletId")
    private UUID walletId;

    @JsonProperty("balance")
    private BigDecimal balance;
}
