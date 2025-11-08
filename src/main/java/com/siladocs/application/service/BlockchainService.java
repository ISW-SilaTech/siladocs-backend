package com.siladocs.application.service;

import com.siladocs.application.dto.SyllabusHistoryResponse; // 🔹 Importar DTO
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder; // 🔹 Importar
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*; // 🔹 Importar
import org.web3j.abi.datatypes.generated.Uint256; // 🔹 Importar
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall; // 🔹 Importar
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant; // 🔹 Importar
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors; // 🔹 Importar

@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;

    public BlockchainService(Web3j web3j, Credentials credentials, @Value("${blockchain.contract.address}") String contractAddress) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.contractAddress = contractAddress;
    }

    /**
     * Llama al Smart Contract "addVersion" para registrar un cambio (ESCRITURA).
     */
    public String registerSyllabusVersion(Long syllabusId, String dataHash, String actorEmail, String action) throws Exception {
        // ... (Tu método de escritura existente está perfecto, no se toca)
        log.info("Registrando en Blockchain: syllabusId={}, hash={}", syllabusId, dataHash);
        final Function function = new Function(
                "addVersion",
                Arrays.asList(
                        new org.web3j.abi.datatypes.Uint(BigInteger.valueOf(syllabusId)),
                        new org.web3j.abi.datatypes.Utf8String(dataHash),
                        new org.web3j.abi.datatypes.Utf8String(actorEmail),
                        new org.web3j.abi.datatypes.Utf8String(action)
                ),
                Collections.emptyList()
        );
        String encodedFunction = FunctionEncoder.encode(function);
        BigInteger nonce = web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                .send().getTransactionCount();
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce, DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT,
                contractAddress, BigInteger.ZERO, encodedFunction
        );
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexSignedMessage = Numeric.toHexString(signedMessage);
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexSignedMessage).send();
        if (ethSendTransaction.hasError()) {
            log.error("Error en transacción blockchain: {}", ethSendTransaction.getError().getMessage());
            throw new RuntimeException("Error en transacción blockchain: " + ethSendTransaction.getError().getMessage());
        }
        String txHash = ethSendTransaction.getTransactionHash();
        log.info("Transacción enviada a Ganache. TxHash: {}", txHash);
        return txHash;
    }

    // ⬇️ 🔹 ----- NUEVO MÉTODO DE LECTURA ----- 🔹 ⬇️

    /**
     * Llama al Smart Contract "getHistory" para leer la trazabilidad (LECTURA).
     */
    public List<SyllabusHistoryResponse> getSyllabusHistory(Long syllabusId) throws Exception {

        // 1. Define la función "getHistory(uint256)" de tu contrato
        final Function function = new Function(
                "getHistory",
                Arrays.asList(new Uint256(syllabusId)), // Parámetro de entrada
                Arrays.asList(new TypeReference<DynamicArray<Version>>() {}) // Tipo de dato de salida
        );

        // 2. Codifica la llamada
        String encodedFunction = FunctionEncoder.encode(function);

        // 3. Crea la llamada de solo lectura (eth_call)
        Transaction ethCallTransaction = Transaction.createEthCallTransaction(
                null, // 'from' no es necesario para una lectura
                contractAddress, // A dónde llamas
                encodedFunction
        );

        // 4. Ejecuta la llamada
        EthCall response = web3j.ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).send();

        if (response.hasError()) {
            throw new RuntimeException("Error al leer de la blockchain: " + response.getError().getMessage());
        }

        // 5. Decodifica la respuesta
        List<Type> result = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

        if (result.isEmpty() || result.get(0).getValue() == null) {
            return Collections.emptyList();
        }

        // 6. Convierte la respuesta en tu DTO
        DynamicArray<Version> resultArray = (DynamicArray<Version>) result.get(0);
        return resultArray.getValue().stream()
                .map(this::convertVersionToDto)
                .collect(Collectors.toList());
    }

    // --- Helper para convertir el struct 'Version' a tu DTO ---
    private SyllabusHistoryResponse convertVersionToDto(Version v) {
        return new SyllabusHistoryResponse(
                v.version.longValueExact(),
                v.dataHash,
                Instant.ofEpochSecond(v.timestamp.longValueExact()), // Convierte timestamp
                v.actorEmail,
                v.action
        );
    }

    // --- Clase interna que mapea el 'struct Version' de Solidity ---
    public static class Version extends DynamicStruct {
        public BigInteger version;
        public String dataHash;
        public BigInteger timestamp;
        public String actorEmail;
        public String action;

        public Version(BigInteger version, String dataHash, BigInteger timestamp, String actorEmail, String action) {
            super(
                    new Uint256(version),
                    new Utf8String(dataHash),
                    new Uint256(timestamp),
                    new Utf8String(actorEmail),
                    new Utf8String(action)
            );
            this.version = version;
            this.dataHash = dataHash;
            this.timestamp = timestamp;
            this.actorEmail = actorEmail;
            this.action = action;
        }

        // Constructor requerido por Web3j para decodificar
        public Version(Uint256 version, Utf8String dataHash, Uint256 timestamp, Utf8String actorEmail, Utf8String action) {
            this(version.getValue(), dataHash.getValue(), timestamp.getValue(), actorEmail.getValue(), action.getValue());
        }
    }
}