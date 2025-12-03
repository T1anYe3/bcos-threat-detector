package com;

import java.math.BigInteger;
import java.util.Arrays;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.Address;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int256;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int64;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.ProxySignTransactionManager;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

@SuppressWarnings("unchecked")
public class CastTest extends Contract {
    public static final String[] BINARY_ARRAY = {"608060405234801561001057600080fd5b50610729806100206000396000f3fe608060405234801561001057600080fd5b506004361061009e5760003560e01c8063b7d534a111610066578063b7d534a11461013a578063c7f793141461014d578063cfb5192814610173578063da091c4414610186578063ecb6fef31461019957600080fd5b80630eae73dd146100a35780633b9129fe146100d357806354058170146100f35780639201de5514610114578063a6f86a6114610127575b600080fd5b6100b66100b13660046104c5565b6101ac565b6040516001600160a01b0390911681526020015b60405180910390f35b6100e66100e1366004610545565b61021b565b6040516100ca919061058e565b6101066101013660046104c5565b610285565b6040519081526020016100ca565b6100e6610122366004610545565b6102ee565b6101066101353660046104c5565b610317565b6100e66101483660046105d9565b61033f565b61016061015b3660046104c5565b610370565b60405160079190910b81526020016100ca565b6101066101813660046104c5565b6103d9565b6100e6610194366004610545565b610401565b6100e66101a736600461060c565b61042a565b604051630eae73dd60e01b815260009061100f90630eae73dd906101d490859060040161058e565b602060405180830381865afa1580156101f1573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906102159190610629565b92915050565b604051631dc894ff60e11b81526004810182905260609061100f90633b9129fe906024015b600060405180830381865afa15801561025d573d6000803e3d6000fd5b505050506040513d6000823e601f3d908101601f191682016040526102159190810190610646565b604051630540581760e41b815260009061100f906354058170906102ad90859060040161058e565b602060405180830381865afa1580156102ca573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061021591906106bd565b604051639201de5560e01b81526004810182905260609061100f90639201de5590602401610240565b60405163a6f86a6160e01b815260009061100f9063a6f86a61906102ad90859060040161058e565b60405163b7d534a160e01b81526001600160a01b038216600482015260609061100f9063b7d534a190602401610240565b6040516331fde4c560e21b815260009061100f9063c7f793149061039890859060040161058e565b602060405180830381865afa1580156103b5573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061021591906106d6565b6040516319f6a32560e31b815260009061100f9063cfb51928906102ad90859060040161058e565b604051633682471160e21b81526004810182905260609061100f9063da091c4490602401610240565b60405163ecb6fef360e01b8152600782900b600482015260609061100f9063ecb6fef390602401610240565b634e487b7160e01b600052604160045260246000fd5b604051601f8201601f1916810167ffffffffffffffff8111828210171561049557610495610456565b604052919050565b600067ffffffffffffffff8211156104b7576104b7610456565b50601f01601f191660200190565b6000602082840312156104d757600080fd5b813567ffffffffffffffff8111156104ee57600080fd5b8201601f810184136104ff57600080fd5b803561051261050d8261049d565b61046c565b81815285602083850101111561052757600080fd5b81602084016020830137600091810160200191909152949350505050565b60006020828403121561055757600080fd5b5035919050565b60005b83811015610579578181015183820152602001610561565b83811115610588576000848401525b50505050565b60208152600082518060208401526105ad81604085016020870161055e565b601f01601f19169190910160400192915050565b6001600160a01b03811681146105d657600080fd5b50565b6000602082840312156105eb57600080fd5b81356105f6816105c1565b9392505050565b8060070b81146105d657600080fd5b60006020828403121561061e57600080fd5b81356105f6816105fd565b60006020828403121561063b57600080fd5b81516105f6816105c1565b60006020828403121561065857600080fd5b815167ffffffffffffffff81111561066f57600080fd5b8201601f8101841361068057600080fd5b805161068e61050d8261049d565b8181528560208385010111156106a357600080fd5b6106b482602083016020860161055e565b95945050505050565b6000602082840312156106cf57600080fd5b5051919050565b6000602082840312156106e857600080fd5b81516105f6816105fd56fea2646970667358221220ef185b62dd6e748cb1eb1587b5eb6e7ba758c156fbe348934c649259fa7f174c64736f6c634300080b0033"};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"608060405234801561001057600080fd5b50610729806100206000396000f3fe608060405234801561001057600080fd5b506004361061009e5760003560e01c806366f7a4581161006657806366f7a45814610135578063a013a87d14610148578063bcb0c2e41461015b578063c39b2b3a1461016e578063cb2c4c101461018157600080fd5b8063184f1ff7146100a3578063446a1fe1146100c95780634ef4f309146100e957806351641766146100fc57806362f55d0414610122575b600080fd5b6100b66100b13660046104c5565b6101ac565b6040519081526020015b60405180910390f35b6100dc6100d7366004610545565b61021b565b6040516100c0919061058e565b6100b66100f73660046104c5565b610285565b61010f61010a3660046104c5565b6102ad565b60405160079190910b81526020016100c0565b6100dc610130366004610545565b610316565b6100dc610143366004610545565b61033f565b6100dc6101563660046105d9565b610368565b6100b66101693660046104c5565b610399565b6100dc61017c36600461060c565b6103c1565b61019461018f3660046104c5565b6103ed565b6040516001600160a01b0390911681526020016100c0565b60405163184f1ff760e01b815260009061100f9063184f1ff7906101d490859060040161058e565b602060405180830381865afa1580156101f1573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906102159190610629565b92915050565b60405163446a1fe160e01b81526004810182905260609061100f9063446a1fe1906024015b600060405180830381865afa15801561025d573d6000803e3d6000fd5b505050506040513d6000823e601f3d908101601f191682016040526102159190810190610642565b604051634ef4f30960e01b815260009061100f90634ef4f309906101d490859060040161058e565b6040516328b20bb360e11b815260009061100f906351641766906102d590859060040161058e565b602060405180830381865afa1580156102f2573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061021591906106b9565b6040516318bd574160e21b81526004810182905260609061100f906362f55d0490602401610240565b604051630cdef48b60e31b81526004810182905260609061100f906366f7a45890602401610240565b60405163a013a87d60e01b81526001600160a01b038216600482015260609061100f9063a013a87d90602401610240565b604051632f2c30b960e21b815260009061100f9063bcb0c2e4906101d490859060040161058e565b6040516361cd959d60e11b8152600782900b600482015260609061100f9063c39b2b3a90602401610240565b604051630cb2c4c160e41b815260009061100f9063cb2c4c109061041590859060040161058e565b602060405180830381865afa158015610432573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061021591906106d6565b63b95aa35560e01b600052604160045260246000fd5b604051601f8201601f1916810167ffffffffffffffff8111828210171561049557610495610456565b604052919050565b600067ffffffffffffffff8211156104b7576104b7610456565b50601f01601f191660200190565b6000602082840312156104d757600080fd5b813567ffffffffffffffff8111156104ee57600080fd5b8201601f810184136104ff57600080fd5b803561051261050d8261049d565b61046c565b81815285602083850101111561052757600080fd5b81602084016020830137600091810160200191909152949350505050565b60006020828403121561055757600080fd5b5035919050565b60005b83811015610579578181015183820152602001610561565b83811115610588576000848401525b50505050565b60208152600082518060208401526105ad81604085016020870161055e565b601f01601f19169190910160400192915050565b6001600160a01b03811681146105d657600080fd5b50565b6000602082840312156105eb57600080fd5b81356105f6816105c1565b9392505050565b8060070b81146105d657600080fd5b60006020828403121561061e57600080fd5b81356105f6816105fd565b60006020828403121561063b57600080fd5b5051919050565b60006020828403121561065457600080fd5b815167ffffffffffffffff81111561066b57600080fd5b8201601f8101841361067c57600080fd5b805161068a61050d8261049d565b81815285602083850101111561069f57600080fd5b6106b082602083016020860161055e565b95945050505050565b6000602082840312156106cb57600080fd5b81516105f6816105fd565b6000602082840312156106e857600080fd5b81516105f6816105c156fea2646970667358221220246538c728f6aa126ac3f786e699ac8876033e5acdd78407425e04bacd963bca64736f6c634300080b0033"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"address\",\"name\":\"_a\",\"type\":\"address\"}],\"name\":\"addrToString\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"selector\":[3084203169,2685642877],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"_b\",\"type\":\"bytes32\"}],\"name\":\"bytes32ToString\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"selector\":[2449595989,1147805665],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"int256\",\"name\":\"_i\",\"type\":\"int256\"}],\"name\":\"s256ToString\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"selector\":[999369214,1727505496],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"int64\",\"name\":\"_i\",\"type\":\"int64\"}],\"name\":\"s64ToString\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"selector\":[3971415795,3281726266],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"_s\",\"type\":\"string\"}],\"name\":\"stringToAddr\",\"outputs\":[{\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"}],\"selector\":[246313949,3408677904],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"_s\",\"type\":\"string\"}],\"name\":\"stringToBytes32\",\"outputs\":[{\"internalType\":\"bytes32\",\"name\":\"\",\"type\":\"bytes32\"}],\"selector\":[3484752168,1324675849],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"_s\",\"type\":\"string\"}],\"name\":\"stringToS256\",\"outputs\":[{\"internalType\":\"int256\",\"name\":\"\",\"type\":\"int256\"}],\"selector\":[2801298017,407838711],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"_s\",\"type\":\"string\"}],\"name\":\"stringToS64\",\"outputs\":[{\"internalType\":\"int64\",\"name\":\"\",\"type\":\"int64\"}],\"selector\":[3354891028,1365514086],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"_s\",\"type\":\"string\"}],\"name\":\"stringToU256\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"selector\":[1409646960,3165700836],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"_u\",\"type\":\"uint256\"}],\"name\":\"u256ToString\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"selector\":[3658030148,1660247300],\"stateMutability\":\"view\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_ADDRTOSTRING = "addrToString";

    public static final String FUNC_BYTES32TOSTRING = "bytes32ToString";

    public static final String FUNC_S256TOSTRING = "s256ToString";

    public static final String FUNC_S64TOSTRING = "s64ToString";

    public static final String FUNC_STRINGTOADDR = "stringToAddr";

    public static final String FUNC_STRINGTOBYTES32 = "stringToBytes32";

    public static final String FUNC_STRINGTOS256 = "stringToS256";

    public static final String FUNC_STRINGTOS64 = "stringToS64";

    public static final String FUNC_STRINGTOU256 = "stringToU256";

    public static final String FUNC_U256TOSTRING = "u256ToString";

    protected CastTest(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
        this.transactionManager = new ProxySignTransactionManager(client);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    public String addrToString(String _a) throws ContractException {
        final Function function = new Function(FUNC_ADDRTOSTRING, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(_a)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeCallWithSingleValueReturn(function, String.class);
    }

    public Function getMethodAddrToStringRawFunction(String _a) throws ContractException {
        final Function function = new Function(FUNC_ADDRTOSTRING, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(_a)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return function;
    }

    public String bytes32ToString(byte[] _b) throws ContractException {
        final Function function = new Function(FUNC_BYTES32TOSTRING, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_b)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeCallWithSingleValueReturn(function, String.class);
    }

    public Function getMethodBytes32ToStringRawFunction(byte[] _b) throws ContractException {
        final Function function = new Function(FUNC_BYTES32TOSTRING, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(_b)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return function;
    }

    public String s256ToString(BigInteger _i) throws ContractException {
        final Function function = new Function(FUNC_S256TOSTRING, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int256(_i)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeCallWithSingleValueReturn(function, String.class);
    }

    public Function getMethodS256ToStringRawFunction(BigInteger _i) throws ContractException {
        final Function function = new Function(FUNC_S256TOSTRING, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int256(_i)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return function;
    }

    public String s64ToString(BigInteger _i) throws ContractException {
        final Function function = new Function(FUNC_S64TOSTRING, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int64(_i)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeCallWithSingleValueReturn(function, String.class);
    }

    public Function getMethodS64ToStringRawFunction(BigInteger _i) throws ContractException {
        final Function function = new Function(FUNC_S64TOSTRING, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int64(_i)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return function;
    }

    public String stringToAddr(String _s) throws ContractException {
        final Function function = new Function(FUNC_STRINGTOADDR, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_s)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeCallWithSingleValueReturn(function, String.class);
    }

    public Function getMethodStringToAddrRawFunction(String _s) throws ContractException {
        final Function function = new Function(FUNC_STRINGTOADDR, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_s)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return function;
    }

    public byte[] stringToBytes32(String _s) throws ContractException {
        final Function function = new Function(FUNC_STRINGTOBYTES32, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_s)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeCallWithSingleValueReturn(function, byte[].class);
    }

    public Function getMethodStringToBytes32RawFunction(String _s) throws ContractException {
        final Function function = new Function(FUNC_STRINGTOBYTES32, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_s)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return function;
    }

    public BigInteger stringToS256(String _s) throws ContractException {
        final Function function = new Function(FUNC_STRINGTOS256, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_s)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int256>() {}));
        return executeCallWithSingleValueReturn(function, BigInteger.class);
    }

    public Function getMethodStringToS256RawFunction(String _s) throws ContractException {
        final Function function = new Function(FUNC_STRINGTOS256, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_s)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int256>() {}));
        return function;
    }

    public BigInteger stringToS64(String _s) throws ContractException {
        final Function function = new Function(FUNC_STRINGTOS64, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_s)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int64>() {}));
        return executeCallWithSingleValueReturn(function, BigInteger.class);
    }

    public Function getMethodStringToS64RawFunction(String _s) throws ContractException {
        final Function function = new Function(FUNC_STRINGTOS64, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_s)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int64>() {}));
        return function;
    }

    public BigInteger stringToU256(String _s) throws ContractException {
        final Function function = new Function(FUNC_STRINGTOU256, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_s)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeCallWithSingleValueReturn(function, BigInteger.class);
    }

    public Function getMethodStringToU256RawFunction(String _s) throws ContractException {
        final Function function = new Function(FUNC_STRINGTOU256, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_s)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return function;
    }

    public String u256ToString(BigInteger _u) throws ContractException {
        final Function function = new Function(FUNC_U256TOSTRING, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(_u)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeCallWithSingleValueReturn(function, String.class);
    }

    public Function getMethodU256ToStringRawFunction(BigInteger _u) throws ContractException {
        final Function function = new Function(FUNC_U256TOSTRING, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(_u)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return function;
    }

    public static CastTest load(String contractAddress, Client client, CryptoKeyPair credential) {
        return new CastTest(contractAddress, client, credential);
    }

    public static CastTest deploy(Client client, CryptoKeyPair credential) throws
            ContractException {
        return deploy(CastTest.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
    }
}
