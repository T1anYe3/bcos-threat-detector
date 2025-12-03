package com;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes;
import org.fisco.bcos.sdk.v3.codec.datatypes.Event;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int256;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.eventsub.EventSubCallback;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.ProxySignTransactionManager;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

@SuppressWarnings("unchecked")
public class EventSubDemo extends Contract {
    public static final String[] BINARY_ARRAY = {"608060405234801561001057600080fd5b506105e2806100206000396000f3fe608060405234801561001057600080fd5b50600436106100415760003560e01c8063274b9f101461004657806375a71ca0146100705780639b80b05014610092575b600080fd5b6100596100543660046103ac565b6100a7565b604051610067929190610463565b60405180910390f35b61008361007e3660046104ab565b61015e565b604051610067939291906104fb565b6100a56100a0366004610523565b610247565b005b60006060837f347ac5ea9ec0d19b3b6ad2651257cc684ecca0e41558fc1c578516e90bcf45f260405160405180910390a2826040516100e69190610590565b604051908190038120907f5f886b86d4364df6c5d7d9a65705aac01b180e2a136442a9921860ca0fdf49db90600090a2826040516101249190610590565b6040519081900381209085907f3752a357a949d58944fee07631779eb98f8c1ba788096770cc9cbc014e2375b890600090a3509192909150565b6000806060857f34d6d9becd7a327109612b0e636ca3bea6263a273c0256df42fbdf3d92e467f960405160405180910390a260405185907f335e9c894374ff443b4a42deadc7dce6dba3b921062aef988a13ed1fba42034390600090a2836040516101c99190610590565b604051908190038120907fdb84d7c006c4de68f9c0bd50b8b81ed31f29ebeec325c872d36445c6565d757c90600090a2836040516102079190610590565b60405190819003812090869088907f6a69e35d4db25f48425c10a32d6e2553fdc3af0e5096cb309b0049c2235e197990600090a450939492935090919050565b80826040516102569190610590565b60405180910390208460405161026c9190610590565b604051908190038120907f5358be4df107be4d9b023fc323f41d7109610225c6ef211b9d375b9fbd7ccc4f90600090a4816040516102aa9190610590565b6040518091039020836040516102c09190610590565b604051908190038120907f8c21d0fee2cb98bb839d8a17a9fe8e11839e85be0675622169aca05cda58260890600090a360405181907f8f922f97953a95596e8c51012daa271d3dd61455382767329dec1c997ed1fbf190600090a2505050565b634e487b7160e01b600052604160045260246000fd5b600067ffffffffffffffff8084111561035157610351610320565b604051601f8501601f19908116603f0116810190828211818310171561037957610379610320565b8160405280935085815286868601111561039257600080fd5b858560208301376000602087830101525050509392505050565b600080604083850312156103bf57600080fd5b82359150602083013567ffffffffffffffff8111156103dd57600080fd5b8301601f810185136103ee57600080fd5b6103fd85823560208401610336565b9150509250929050565b60005b8381101561042257818101518382015260200161040a565b83811115610431576000848401525b50505050565b6000815180845261044f816020860160208601610407565b601f01601f19169290920160200192915050565b82815260406020820152600061047c6040830184610437565b949350505050565b600082601f83011261049557600080fd5b6104a483833560208501610336565b9392505050565b6000806000606084860312156104c057600080fd5b8335925060208401359150604084013567ffffffffffffffff8111156104e557600080fd5b6104f186828701610484565b9150509250925092565b83815282602082015260606040820152600061051a6060830184610437565b95945050505050565b60008060006060848603121561053857600080fd5b833567ffffffffffffffff8082111561055057600080fd5b61055c87838801610484565b9450602086013591508082111561057257600080fd5b5061057f86828701610484565b925050604084013590509250925092565b600082516105a2818460208701610407565b919091019291505056fea2646970667358221220be39faf07c33e8ba6fd21d9013349e745c257c9bccd1a529e4174e04abceeefe64736f6c634300080b0033"};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"608060405234801561001057600080fd5b506105e2806100206000396000f3fe608060405234801561001057600080fd5b50600436106100415760003560e01c8063612d2bff14610046578063d2ae49dd1461005b578063e1ddfaf714610085575b600080fd5b6100596100543660046103d3565b6100a7565b005b61006e610069366004610440565b610180565b60405161007c9291906104f7565b60405180910390f35b610098610093366004610518565b610237565b60405161007c93929190610568565b80826040516100b69190610590565b6040518091039020846040516100cc9190610590565b604051908190038120907fb8ab06736b3dfec12a172981931c9e25724e33b9d0902dcc3d776d1957fed49590600090a48160405161010a9190610590565b6040518091039020836040516101209190610590565b604051908190038120907f7d5d4b35565e8ef6f3e083591478c95dcb35dedf5d0ac6355dde5d9f9e0e5bd390600090a360405181907fe083dc30f45290272bf5496ad6e0c7a5051ed7c6aa2042d26d38a69aa579042190600090a2505050565b60006060837fa088533085dfc0ff7c4d3bbdb59aaff77bbd3c33e7d9ab03a5925f992f9b82a660405160405180910390a2826040516101bf9190610590565b604051908190038120907fd93dc2cb92c6566fa1747478cf11424fea23528fdb5cbfb18b3d9a617e88531a90600090a2826040516101fd9190610590565b6040519081900381209085907f2f05c1c5ee846538bc6a5c8740a4385209bd1569ec55feb14b9a9af7d018730d90600090a3509192909150565b6000806060857f5cbb1b4a7272b6d2f504f0a84614866e3b1c10358e1aff1650b1261492fc134760405160405180910390a260405185907fd8b50b6b74c748d297159108a50fc320d5c4c12ba2ec65a752f8bc35e0422cd190600090a2836040516102a29190610590565b604051908190038120907fcb4a0749276a06a00950a4a6d16baf41ed5148d70c2ee5a255bb6c02ee6ad95f90600090a2836040516102e09190610590565b60405190819003812090869088907ff16a378efaa9313a2841e3199e34474dcc5449d20b6cc8d5e9f97ef97429b1c390600090a450939492935090919050565b63b95aa35560e01b600052604160045260246000fd5b600067ffffffffffffffff8084111561035157610351610320565b604051601f8501601f19908116603f0116810190828211818310171561037957610379610320565b8160405280935085815286868601111561039257600080fd5b858560208301376000602087830101525050509392505050565b600082601f8301126103bd57600080fd5b6103cc83833560208501610336565b9392505050565b6000806000606084860312156103e857600080fd5b833567ffffffffffffffff8082111561040057600080fd5b61040c878388016103ac565b9450602086013591508082111561042257600080fd5b5061042f868287016103ac565b925050604084013590509250925092565b6000806040838503121561045357600080fd5b82359150602083013567ffffffffffffffff81111561047157600080fd5b8301601f8101851361048257600080fd5b61049185823560208401610336565b9150509250929050565b60005b838110156104b657818101518382015260200161049e565b838111156104c5576000848401525b50505050565b600081518084526104e381602086016020860161049b565b601f01601f19169290920160200192915050565b82815260406020820152600061051060408301846104cb565b949350505050565b60008060006060848603121561052d57600080fd5b8335925060208401359150604084013567ffffffffffffffff81111561055257600080fd5b61055e868287016103ac565b9150509250925092565b83815282602082015260606040820152600061058760608301846104cb565b95945050505050565b600082516105a281846020870161049b565b919091019291505056fea2646970667358221220992e8d242270481b0d86e8825270390c3773c771f1ead3ad1951872c5af1ed0664736f6c634300080b0033"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"uint256\",\"name\":\"u\",\"type\":\"uint256\"}],\"name\":\"Echo\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"int256\",\"name\":\"i\",\"type\":\"int256\"}],\"name\":\"Echo\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"string\",\"name\":\"s\",\"type\":\"string\"}],\"name\":\"Echo\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"uint256\",\"name\":\"u\",\"type\":\"uint256\"},{\"indexed\":true,\"internalType\":\"int256\",\"name\":\"i\",\"type\":\"int256\"},{\"indexed\":true,\"internalType\":\"string\",\"name\":\"s\",\"type\":\"string\"}],\"name\":\"Echo\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"bytes32\",\"name\":\"bsn\",\"type\":\"bytes32\"}],\"name\":\"Echo\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"bytes\",\"name\":\"bs\",\"type\":\"bytes\"}],\"name\":\"Echo\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"bytes32\",\"name\":\"bsn\",\"type\":\"bytes32\"},{\"indexed\":true,\"internalType\":\"bytes\",\"name\":\"bs\",\"type\":\"bytes\"}],\"name\":\"Echo\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"string\",\"name\":\"from_account\",\"type\":\"string\"},{\"indexed\":true,\"internalType\":\"string\",\"name\":\"to_account\",\"type\":\"string\"},{\"indexed\":true,\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"Transfer\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"string\",\"name\":\"from_account\",\"type\":\"string\"},{\"indexed\":true,\"internalType\":\"string\",\"name\":\"to_account\",\"type\":\"string\"}],\"name\":\"TransferAccount\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"TransferAmount\",\"type\":\"event\"},{\"conflictFields\":[{\"kind\":5}],\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"bsn\",\"type\":\"bytes32\"},{\"internalType\":\"bytes\",\"name\":\"bs\",\"type\":\"bytes\"}],\"name\":\"echo\",\"outputs\":[{\"internalType\":\"bytes32\",\"name\":\"\",\"type\":\"bytes32\"},{\"internalType\":\"bytes\",\"name\":\"\",\"type\":\"bytes\"}],\"selector\":[659267344,3534637533],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":5}],\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"u\",\"type\":\"uint256\"},{\"internalType\":\"int256\",\"name\":\"i\",\"type\":\"int256\"},{\"internalType\":\"string\",\"name\":\"s\",\"type\":\"string\"}],\"name\":\"echo\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"},{\"internalType\":\"int256\",\"name\":\"\",\"type\":\"int256\"},{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"selector\":[1973886112,3789421303],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":5}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"from_account\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"to_account\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"transfer\",\"outputs\":[],\"selector\":[2608902224,1630350335],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_ECHO = "echo";

    public static final String FUNC_TRANSFER = "transfer";

    public static final Event ECHOUINT256_EVENT = new Event("Echo", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    public static final Event ECHOINT256_EVENT = new Event("Echo", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Int256>(true) {}));
    ;

    public static final Event ECHOSTRING_EVENT = new Event("Echo", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}));
    ;

    public static final Event ECHOUINT256INT256STRING_EVENT = new Event("Echo", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Int256>(true) {}, new TypeReference<Utf8String>(true) {}));
    ;

    public static final Event ECHOBYTES32_EVENT = new Event("Echo", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event ECHOBYTES_EVENT = new Event("Echo", 
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>(true) {}));
    ;

    public static final Event ECHOBYTES32BYTES_EVENT = new Event("Echo", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<DynamicBytes>(true) {}));
    ;

    public static final Event TRANSFER_EVENT = new Event("Transfer", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Utf8String>(true) {}, new TypeReference<Uint256>(true) {}));
    ;

    public static final Event TRANSFERACCOUNT_EVENT = new Event("TransferAccount", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Utf8String>(true) {}));
    ;

    public static final Event TRANSFERAMOUNT_EVENT = new Event("TransferAmount", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    protected EventSubDemo(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
        this.transactionManager = new ProxySignTransactionManager(client);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    public List<EchoUint256EventResponse> getEchoUint256Events(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ECHOUINT256_EVENT, transactionReceipt);
        ArrayList<EchoUint256EventResponse> responses = new ArrayList<EchoUint256EventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            EchoUint256EventResponse typedResponse = new EchoUint256EventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.u = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeEchoUint256Event(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOUINT256_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeEchoUint256Event(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOUINT256_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<EchoInt256EventResponse> getEchoInt256Events(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ECHOINT256_EVENT, transactionReceipt);
        ArrayList<EchoInt256EventResponse> responses = new ArrayList<EchoInt256EventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            EchoInt256EventResponse typedResponse = new EchoInt256EventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.i = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeEchoInt256Event(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOINT256_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeEchoInt256Event(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOINT256_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<EchoStringEventResponse> getEchoStringEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ECHOSTRING_EVENT, transactionReceipt);
        ArrayList<EchoStringEventResponse> responses = new ArrayList<EchoStringEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            EchoStringEventResponse typedResponse = new EchoStringEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.s = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeEchoStringEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOSTRING_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeEchoStringEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOSTRING_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<EchoUint256Int256StringEventResponse> getEchoUint256Int256StringEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ECHOUINT256INT256STRING_EVENT, transactionReceipt);
        ArrayList<EchoUint256Int256StringEventResponse> responses = new ArrayList<EchoUint256Int256StringEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            EchoUint256Int256StringEventResponse typedResponse = new EchoUint256Int256StringEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.u = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.i = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.s = (byte[]) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeEchoUint256Int256StringEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOUINT256INT256STRING_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeEchoUint256Int256StringEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOUINT256INT256STRING_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<EchoBytes32EventResponse> getEchoBytes32Events(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ECHOBYTES32_EVENT, transactionReceipt);
        ArrayList<EchoBytes32EventResponse> responses = new ArrayList<EchoBytes32EventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            EchoBytes32EventResponse typedResponse = new EchoBytes32EventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.bsn = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeEchoBytes32Event(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOBYTES32_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeEchoBytes32Event(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOBYTES32_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<EchoBytesEventResponse> getEchoBytesEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ECHOBYTES_EVENT, transactionReceipt);
        ArrayList<EchoBytesEventResponse> responses = new ArrayList<EchoBytesEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            EchoBytesEventResponse typedResponse = new EchoBytesEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.bs = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeEchoBytesEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOBYTES_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeEchoBytesEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOBYTES_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<EchoBytes32BytesEventResponse> getEchoBytes32BytesEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ECHOBYTES32BYTES_EVENT, transactionReceipt);
        ArrayList<EchoBytes32BytesEventResponse> responses = new ArrayList<EchoBytes32BytesEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            EchoBytes32BytesEventResponse typedResponse = new EchoBytes32BytesEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.bsn = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.bs = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeEchoBytes32BytesEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOBYTES32BYTES_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeEchoBytes32BytesEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(ECHOBYTES32BYTES_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<TransferEventResponse> getTransferEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSFER_EVENT, transactionReceipt);
        ArrayList<TransferEventResponse> responses = new ArrayList<TransferEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransferEventResponse typedResponse = new TransferEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.from_account = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.to_account = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.amount = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeTransferEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(TRANSFER_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeTransferEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(TRANSFER_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<TransferAccountEventResponse> getTransferAccountEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSFERACCOUNT_EVENT, transactionReceipt);
        ArrayList<TransferAccountEventResponse> responses = new ArrayList<TransferAccountEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransferAccountEventResponse typedResponse = new TransferAccountEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.from_account = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.to_account = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeTransferAccountEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(TRANSFERACCOUNT_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeTransferAccountEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(TRANSFERACCOUNT_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<TransferAmountEventResponse> getTransferAmountEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSFERAMOUNT_EVENT, transactionReceipt);
        ArrayList<TransferAmountEventResponse> responses = new ArrayList<TransferAmountEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransferAmountEventResponse typedResponse = new TransferAmountEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.amount = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeTransferAmountEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(TRANSFERAMOUNT_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeTransferAmountEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(TRANSFERAMOUNT_EVENT);
        subscribeEvent(topic0,callback);
    }

    public TransactionReceipt echo(byte[] bsn, byte[] bs) {
        final Function function = new Function(
                FUNC_ECHO, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(bsn), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(bs)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return executeTransaction(function);
    }

    public Function getMethodEchoRawFunction(byte[] bsn, byte[] bs) throws ContractException {
        final Function function = new Function(FUNC_ECHO, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(bsn), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(bs)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}));
        return function;
    }

    public String getSignedTransactionForEcho(byte[] bsn, byte[] bs) {
        final Function function = new Function(
                FUNC_ECHO, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(bsn), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(bs)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return createSignedTransaction(function);
    }

    public String echo(byte[] bsn, byte[] bs, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_ECHO, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(bsn), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(bs)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple2<byte[], byte[]> getEchoBytes32BytesInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_ECHO, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple2<byte[], byte[]>(

                (byte[]) results.get(0).getValue(), 
                (byte[]) results.get(1).getValue()
                );
    }

    public Tuple2<byte[], byte[]> getEchoBytes32BytesOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        final Function function = new Function(FUNC_ECHO, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple2<byte[], byte[]>(

                (byte[]) results.get(0).getValue(), 
                (byte[]) results.get(1).getValue()
                );
    }

    public TransactionReceipt echo(BigInteger u, BigInteger i, String s) {
        final Function function = new Function(
                FUNC_ECHO, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(u), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int256(i), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(s)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return executeTransaction(function);
    }

    public Function getMethodEchoRawFunction(BigInteger u, BigInteger i, String s) throws
            ContractException {
        final Function function = new Function(FUNC_ECHO, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(u), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int256(i), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(s)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Int256>() {}, new TypeReference<Utf8String>() {}));
        return function;
    }

    public String getSignedTransactionForEcho(BigInteger u, BigInteger i, String s) {
        final Function function = new Function(
                FUNC_ECHO, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(u), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int256(i), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(s)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return createSignedTransaction(function);
    }

    public String echo(BigInteger u, BigInteger i, String s, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_ECHO, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(u), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int256(i), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(s)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<BigInteger, BigInteger, String> getEchoUint256Int256StringInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_ECHO, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Int256>() {}, new TypeReference<Utf8String>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<BigInteger, BigInteger, String>(

                (BigInteger) results.get(0).getValue(), 
                (BigInteger) results.get(1).getValue(), 
                (String) results.get(2).getValue()
                );
    }

    public Tuple3<BigInteger, BigInteger, String> getEchoUint256Int256StringOutput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        final Function function = new Function(FUNC_ECHO, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Int256>() {}, new TypeReference<Utf8String>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<BigInteger, BigInteger, String>(

                (BigInteger) results.get(0).getValue(), 
                (BigInteger) results.get(1).getValue(), 
                (String) results.get(2).getValue()
                );
    }

    public TransactionReceipt transfer(String from_account, String to_account, BigInteger amount) {
        final Function function = new Function(
                FUNC_TRANSFER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(from_account), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(to_account), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(amount)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return executeTransaction(function);
    }

    public Function getMethodTransferRawFunction(String from_account, String to_account,
            BigInteger amount) throws ContractException {
        final Function function = new Function(FUNC_TRANSFER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(from_account), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(to_account), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(amount)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public String getSignedTransactionForTransfer(String from_account, String to_account,
            BigInteger amount) {
        final Function function = new Function(
                FUNC_TRANSFER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(from_account), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(to_account), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(amount)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return createSignedTransaction(function);
    }

    public String transfer(String from_account, String to_account, BigInteger amount,
            TransactionCallback callback) {
        final Function function = new Function(
                FUNC_TRANSFER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(from_account), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(to_account), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(amount)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<String, String, BigInteger> getTransferInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_TRANSFER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<String, String, BigInteger>(

                (String) results.get(0).getValue(), 
                (String) results.get(1).getValue(), 
                (BigInteger) results.get(2).getValue()
                );
    }

    public static EventSubDemo load(String contractAddress, Client client,
            CryptoKeyPair credential) {
        return new EventSubDemo(contractAddress, client, credential);
    }

    public static EventSubDemo deploy(Client client, CryptoKeyPair credential) throws
            ContractException {
        return deploy(EventSubDemo.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
    }

    public static class EchoUint256EventResponse {
        public TransactionReceipt.Logs log;

        public BigInteger u;
    }

    public static class EchoInt256EventResponse {
        public TransactionReceipt.Logs log;

        public BigInteger i;
    }

    public static class EchoStringEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] s;
    }

    public static class EchoUint256Int256StringEventResponse {
        public TransactionReceipt.Logs log;

        public BigInteger u;

        public BigInteger i;

        public byte[] s;
    }

    public static class EchoBytes32EventResponse {
        public TransactionReceipt.Logs log;

        public byte[] bsn;
    }

    public static class EchoBytesEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] bs;
    }

    public static class EchoBytes32BytesEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] bsn;

        public byte[] bs;
    }

    public static class TransferEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] from_account;

        public byte[] to_account;

        public BigInteger amount;
    }

    public static class TransferAccountEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] from_account;

        public byte[] to_account;
    }

    public static class TransferAmountEventResponse {
        public TransactionReceipt.Logs log;

        public BigInteger amount;
    }
}
