package org.example;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.client.protocol.model.JsonTransactionResponse;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.TransactionObject;
import java.lang.reflect.Method;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.request.Transaction;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.Block;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock.TransactionObject;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosTransaction;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import java.util.Optional;
import org.fisco.bcos.sdk.v3.client.protocol.model.JsonTransactionResponse;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosTransactionReceipt;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CheckTransactionReceiptMethods {
    public static void main(String[] args) {
        // 打印 TransactionReceipt 类的所有方法名
        Method[] methods = BcosTransactionReceipt.class.getMethods();//修改.clss.前面的部分
        System.out.println("可用的方法有：");
        for (Method method : methods) {
            System.out.println(method.getName());
        }
    }
}