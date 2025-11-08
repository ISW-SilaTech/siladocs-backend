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
import java.util.ArrayList;
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

    // ⬇️ 🔹 CORRECCIÓN: MÉTODO DE LECTURA DE LISTA (getSyllabusHistory) 🔹 ⬇️
    /**
     * Resuelve el error de compilación del controlador.
     * Itera usando el método getSyllabusVersionByIndex, que es más estable.
     */
    public List<SyllabusHistoryResponse> getSyllabusHistory(Long syllabusId) throws Exception {

        List<SyllabusHistoryResponse> history = new ArrayList<>();
        int index = 0;

        while (true) {
            try {
                // Llama al método de lectura de un solo elemento (más robusto)
                SyllabusHistoryResponse version = getSyllabusVersionByIndex(syllabusId, index);

                // Si la versión es 0, significa que el registro no existe (condición de parada)
                if (version.version() == 0) {
                    break;
                }

                history.add(version);
                index++;
            } catch (Exception e) {
                // Si ocurre un error de decodificación o índice fuera de límites, asumimos el fin del array
                // No logueamos el error aquí, lo hacemos en el controlador si es necesario.
                break;
            }
        }
        // Devolvemos el historial del más reciente al más antiguo
        Collections.reverse(history);
        return history;
    }

    // ⬇️ 🔹 ----- NUEVO MÉTODO DE LECTURA ----- 🔹 ⬇️

    /**
     * Llama al Smart Contract "getHistory" para leer la trazabilidad (LECTURA).
     */
    public SyllabusHistoryResponse getSyllabusVersionByIndex(Long syllabusId, int index) throws Exception {

        final Function function = new Function(
                "syllabusHistory", // Este es el nombre del getter automático del mapping
                Arrays.asList(
                        new org.web3j.abi.datatypes.Uint(BigInteger.valueOf(syllabusId)),
                        new org.web3j.abi.datatypes.Uint(BigInteger.valueOf(index))
                ),
                Arrays.asList(
                        new TypeReference<Uint256>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {}
                )
        );

        String encodedFunction = FunctionEncoder.encode(function);

        // Ejecutar eth_call
        EthCall response = web3j.ethCall(Transaction.createEthCallTransaction(
                null, contractAddress, encodedFunction
        ), DefaultBlockParameterName.LATEST).send();

        if (response.hasError()) {
            throw new RuntimeException("Error al leer versión de blockchain: " + response.getError().getMessage());
        }

        // Decodificar la respuesta
        List<Type> decodedResult = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

        // Convertir el resultado a DTO
        if (decodedResult.size() < 5) {
            throw new RuntimeException("Datos incompletos de la blockchain.");
        }

        return new SyllabusHistoryResponse(
                ((Uint256) decodedResult.get(0)).getValue().longValueExact(), // Version
                ((Utf8String) decodedResult.get(1)).getValue(), // dataHash
                Instant.ofEpochSecond(((Uint256) decodedResult.get(2)).getValue().longValueExact()), // timestamp
                ((Utf8String) decodedResult.get(3)).getValue(), // actorEmail
                ((Utf8String) decodedResult.get(4)).getValue()  // action
        );
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