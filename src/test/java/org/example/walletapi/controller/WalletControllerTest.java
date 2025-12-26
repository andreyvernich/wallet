package org.example.walletapi.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.walletapi.dto.WalletOperationRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WalletControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> 6379);
        registry.add("spring.liquibase.enabled", () -> false);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testWalletId;

    @BeforeEach
    void setUp() {
        testWalletId = UUID.randomUUID();
        log.info("Test wallet ID: {}", testWalletId);
    }

    @Test
    void testDepositCreatesNewWallet() throws Exception {
        WalletOperationRequestDto request = WalletOperationRequestDto.builder()
                .walletId(testWalletId)
                .operationType(WalletOperationRequestDto.OperationType.DEPOSIT)
                .amount(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/wallets/{walletId}", testWalletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void testDepositAndWithdrawOperations() throws Exception {
        WalletOperationRequestDto deposit = WalletOperationRequestDto.builder()
                .walletId(testWalletId)
                .operationType(WalletOperationRequestDto.OperationType.DEPOSIT)
                .amount(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deposit)));

        WalletOperationRequestDto withdraw = WalletOperationRequestDto.builder()
                .walletId(testWalletId)
                .operationType(WalletOperationRequestDto.OperationType.WITHDRAW)
                .amount(new BigDecimal("500.00"))
                .build();

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdraw)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/wallets/{walletId}", testWalletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void testWithdrawFromNonExistentWallet() throws Exception {
        WalletOperationRequestDto request = WalletOperationRequestDto.builder()
                .walletId(testWalletId)
                .operationType(WalletOperationRequestDto.OperationType.WITHDRAW)
                .amount(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Wallet not found"));
    }

    @Test
    void testWithdrawInsufficientFunds() throws Exception {
        WalletOperationRequestDto deposit = WalletOperationRequestDto.builder()
                .walletId(testWalletId)
                .operationType(WalletOperationRequestDto.OperationType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deposit)));

        WalletOperationRequestDto withdraw = WalletOperationRequestDto.builder()
                .walletId(testWalletId)
                .operationType(WalletOperationRequestDto.OperationType.WITHDRAW)
                .amount(new BigDecimal("500.00"))
                .build();

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdraw)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient funds"));
    }

    @Test
    void testGetNonExistentWallet() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/wallets/{walletId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Wallet not found"));
    }

    @Test
    void testValidationErrors() throws Exception {
        WalletOperationRequestDto request = WalletOperationRequestDto.builder()
                .walletId(null)
                .operationType(null)
                .amount(new BigDecimal("-100"))
                .build();

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    void testConcurrentDeposits() throws Exception {

        WalletOperationRequestDto request = WalletOperationRequestDto.builder()
                .walletId(testWalletId)
                .operationType(WalletOperationRequestDto.OperationType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .build();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/wallet")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/wallets/{walletId}", testWalletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));
    }
}
