package com.siladocs.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

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
     * Llama al Smart Contract "addVersion" para registrar un cambio.
     * Esta es una operación de ESCRITURA (cuesta "gas").
     */
    public String registerSyllabusVersion(Long syllabusId, String dataHash, String actorEmail, String action) throws Exception {

        log.info("Registrando en Blockchain: syllabusId={}, hash={}", syllabusId, dataHash);

        // 1. Define la función del Smart Contract que quieres llamar
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

        // 2. Codifica la llamada
        String encodedFunction = FunctionEncoder.encode(function);

        // 3. Obtiene el 'nonce' (contador de transacciones de tu cuenta admin)
        BigInteger nonce = web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                .send().getTransactionCount();

        // 4. Crea un RawTransaction (listo para firmar) usando createTransaction: to=contractAddress, value=0, data=encodedFunction
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                DefaultGasProvider.GAS_PRICE,
                DefaultGasProvider.GAS_LIMIT,
                contractAddress,
                BigInteger.ZERO,
                encodedFunction
        );

        // 5. Firma el RawTransaction y conviértelo a hex antes de enviarlo
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
}
