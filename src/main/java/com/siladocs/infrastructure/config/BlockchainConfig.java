package com.siladocs.infrastructure.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
@Configuration
public class BlockchainConfig {
    @Value("${blockchain.node.url}") private String nodeUrl;
    @Value("${blockchain.admin.private-key}") private String privateKey;
    @Bean public Web3j web3j() { return Web3j.build(new HttpService(nodeUrl)); }
    @Bean public Credentials credentials() { return Credentials.create(privateKey); }
}