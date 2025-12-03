package com;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicStruct;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.StaticStruct;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Int32;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint32;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple1;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.ProxySignTransactionManager;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

@SuppressWarnings("unchecked")
public class Table extends Contract {
    public static final String[] BINARY_ARRAY = {};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"inputs\":[{\"components\":[{\"internalType\":\"enum ConditionOP\",\"name\":\"op\",\"type\":\"uint8\"},{\"internalType\":\"string\",\"name\":\"value\",\"type\":\"string\"}],\"internalType\":\"struct Condition[]\",\"name\":\"conditions\",\"type\":\"tuple[]\"}],\"name\":\"count\",\"outputs\":[{\"internalType\":\"uint32\",\"name\":\"\",\"type\":\"uint32\"}],\"selector\":[3625360167,2327356356],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"components\":[{\"internalType\":\"string\",\"name\":\"key\",\"type\":\"string\"},{\"internalType\":\"string[]\",\"name\":\"fields\",\"type\":\"string[]\"}],\"internalType\":\"struct Entry\",\"name\":\"entry\",\"type\":\"tuple\"}],\"name\":\"insert\",\"outputs\":[{\"internalType\":\"int32\",\"name\":\"\",\"type\":\"int32\"}],\"selector\":[1550717023,1284216112],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"components\":[{\"internalType\":\"enum ConditionOP\",\"name\":\"op\",\"type\":\"uint8\"},{\"internalType\":\"string\",\"name\":\"value\",\"type\":\"string\"}],\"internalType\":\"struct Condition[]\",\"name\":\"conditions\",\"type\":\"tuple[]\"},{\"components\":[{\"internalType\":\"uint32\",\"name\":\"offset\",\"type\":\"uint32\"},{\"internalType\":\"uint32\",\"name\":\"count\",\"type\":\"uint32\"}],\"internalType\":\"struct Limit\",\"name\":\"limit\",\"type\":\"tuple\"}],\"name\":\"remove\",\"outputs\":[{\"internalType\":\"int32\",\"name\":\"\",\"type\":\"int32\"}],\"selector\":[1751202047,277135530],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"key\",\"type\":\"string\"}],\"name\":\"remove\",\"outputs\":[{\"internalType\":\"int32\",\"name\":\"\",\"type\":\"int32\"}],\"selector\":[2153356875,2260153337],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"components\":[{\"internalType\":\"enum ConditionOP\",\"name\":\"op\",\"type\":\"uint8\"},{\"internalType\":\"string\",\"name\":\"value\",\"type\":\"string\"}],\"internalType\":\"struct Condition[]\",\"name\":\"conditions\",\"type\":\"tuple[]\"},{\"components\":[{\"internalType\":\"uint32\",\"name\":\"offset\",\"type\":\"uint32\"},{\"internalType\":\"uint32\",\"name\":\"count\",\"type\":\"uint32\"}],\"internalType\":\"struct Limit\",\"name\":\"limit\",\"type\":\"tuple\"}],\"name\":\"select\",\"outputs\":[{\"components\":[{\"internalType\":\"string\",\"name\":\"key\",\"type\":\"string\"},{\"internalType\":\"string[]\",\"name\":\"fields\",\"type\":\"string[]\"}],\"internalType\":\"struct Entry[]\",\"name\":\"\",\"type\":\"tuple[]\"}],\"selector\":[1020609838,1062557692],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"key\",\"type\":\"string\"}],\"name\":\"select\",\"outputs\":[{\"components\":[{\"internalType\":\"string\",\"name\":\"key\",\"type\":\"string\"},{\"internalType\":\"string[]\",\"name\":\"fields\",\"type\":\"string[]\"}],\"internalType\":\"struct Entry\",\"name\":\"\",\"type\":\"tuple\"}],\"selector\":[4242006977,1530027384],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"key\",\"type\":\"string\"},{\"components\":[{\"internalType\":\"string\",\"name\":\"columnName\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"value\",\"type\":\"string\"}],\"internalType\":\"struct UpdateField[]\",\"name\":\"updateFields\",\"type\":\"tuple[]\"}],\"name\":\"update\",\"outputs\":[{\"internalType\":\"int32\",\"name\":\"\",\"type\":\"int32\"}],\"selector\":[1107285855,33194060],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"components\":[{\"internalType\":\"enum ConditionOP\",\"name\":\"op\",\"type\":\"uint8\"},{\"internalType\":\"string\",\"name\":\"value\",\"type\":\"string\"}],\"internalType\":\"struct Condition[]\",\"name\":\"conditions\",\"type\":\"tuple[]\"},{\"components\":[{\"internalType\":\"uint32\",\"name\":\"offset\",\"type\":\"uint32\"},{\"internalType\":\"uint32\",\"name\":\"count\",\"type\":\"uint32\"}],\"internalType\":\"struct Limit\",\"name\":\"limit\",\"type\":\"tuple\"},{\"components\":[{\"internalType\":\"string\",\"name\":\"columnName\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"value\",\"type\":\"string\"}],\"internalType\":\"struct UpdateField[]\",\"name\":\"updateFields\",\"type\":\"tuple[]\"}],\"name\":\"update\",\"outputs\":[{\"internalType\":\"int32\",\"name\":\"\",\"type\":\"int32\"}],\"selector\":[2572410770,107820592],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_COUNT = "count";

    public static final String FUNC_INSERT = "insert";

    public static final String FUNC_REMOVE = "remove";

    public static final String FUNC_SELECT = "select";

    public static final String FUNC_UPDATE = "update";

    protected Table(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
        this.transactionManager = new ProxySignTransactionManager(client);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    public BigInteger count(List<Condition> conditions) throws ContractException {
        final Function function = new Function(FUNC_COUNT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}));
        return executeCallWithSingleValueReturn(function, BigInteger.class);
    }

    public Function getMethodCountRawFunction(List<Condition> conditions) throws ContractException {
        final Function function = new Function(FUNC_COUNT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}));
        return function;
    }

    public TransactionReceipt insert(Entry entry) {
        final Function function = new Function(
                FUNC_INSERT, 
                Arrays.<Type>asList(entry), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodInsertRawFunction(Entry entry) throws ContractException {
        final Function function = new Function(FUNC_INSERT, 
                Arrays.<Type>asList(entry), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int32>() {}));
        return function;
    }

    public String getSignedTransactionForInsert(Entry entry) {
        final Function function = new Function(
                FUNC_INSERT, 
                Arrays.<Type>asList(entry), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String insert(Entry entry, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_INSERT, 
                Arrays.<Type>asList(entry), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple1<Entry> getInsertInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_INSERT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Entry>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Entry>(

                (Entry) results.get(0)
                );
    }

    public Tuple1<BigInteger> getInsertOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        final Function function = new Function(FUNC_INSERT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int32>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<BigInteger>(

                (BigInteger) results.get(0).getValue()
                );
    }

    public TransactionReceipt remove(List<Condition> conditions, Limit limit) {
        final Function function = new Function(
                FUNC_REMOVE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions), 
                limit), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodRemoveRawFunction(List<Condition> conditions, Limit limit) throws
            ContractException {
        final Function function = new Function(FUNC_REMOVE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions), 
                limit), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int32>() {}));
        return function;
    }

    public String getSignedTransactionForRemove(List<Condition> conditions, Limit limit) {
        final Function function = new Function(
                FUNC_REMOVE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions), 
                limit), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String remove(List<Condition> conditions, Limit limit, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_REMOVE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions), 
                limit), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple2<DynamicArray<Condition>, Limit> getRemoveTupletupleTupleInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_REMOVE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Condition>>() {}, new TypeReference<Limit>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple2<DynamicArray<Condition>, Limit>(

                new DynamicArray<>(Condition.class,(List<Condition>) results.get(0).getValue()), 
                (Limit) results.get(1)
                );
    }

    public Tuple1<BigInteger> getRemoveTupletupleTupleOutput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        final Function function = new Function(FUNC_REMOVE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int32>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<BigInteger>(

                (BigInteger) results.get(0).getValue()
                );
    }

    public TransactionReceipt remove(String key) {
        final Function function = new Function(
                FUNC_REMOVE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(key)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodRemoveRawFunction(String key) throws ContractException {
        final Function function = new Function(FUNC_REMOVE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(key)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int32>() {}));
        return function;
    }

    public String getSignedTransactionForRemove(String key) {
        final Function function = new Function(
                FUNC_REMOVE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(key)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String remove(String key, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_REMOVE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(key)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple1<String> getRemoveStringInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_REMOVE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<String>(

                (String) results.get(0).getValue()
                );
    }

    public Tuple1<BigInteger> getRemoveStringOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        final Function function = new Function(FUNC_REMOVE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int32>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<BigInteger>(

                (BigInteger) results.get(0).getValue()
                );
    }

    public List<Entry> select(List<Condition> conditions, Limit limit) throws ContractException {
        final Function function = new Function(FUNC_SELECT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions), 
                limit), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Entry>>() {}));
        return executeCallWithSingleValueReturn(function, List.class);
    }

    public Function getMethodSelectRawFunction(List<Condition> conditions, Limit limit) throws
            ContractException {
        final Function function = new Function(FUNC_SELECT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions), 
                limit), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Entry>>() {}));
        return function;
    }

    public Entry select(String key) throws ContractException {
        final Function function = new Function(FUNC_SELECT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(key)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Entry>() {}));
        return executeCallWithSingleValueReturn(function, Entry.class);
    }

    public Function getMethodSelectRawFunction(String key) throws ContractException {
        final Function function = new Function(FUNC_SELECT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(key)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Entry>() {}));
        return function;
    }

    public TransactionReceipt update(String key, List<UpdateField> updateFields) {
        final Function function = new Function(
                FUNC_UPDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(key), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<UpdateField>(UpdateField.class, updateFields)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodUpdateRawFunction(String key, List<UpdateField> updateFields) throws
            ContractException {
        final Function function = new Function(FUNC_UPDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(key), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<UpdateField>(UpdateField.class, updateFields)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int32>() {}));
        return function;
    }

    public String getSignedTransactionForUpdate(String key, List<UpdateField> updateFields) {
        final Function function = new Function(
                FUNC_UPDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(key), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<UpdateField>(UpdateField.class, updateFields)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String update(String key, List<UpdateField> updateFields, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_UPDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(key), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<UpdateField>(UpdateField.class, updateFields)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple2<String, DynamicArray<UpdateField>> getUpdateStringTupletupleInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_UPDATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<DynamicArray<UpdateField>>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple2<String, DynamicArray<UpdateField>>(

                (String) results.get(0).getValue(), 
                new DynamicArray<>(UpdateField.class,(List<UpdateField>) results.get(1).getValue())
                );
    }

    public Tuple1<BigInteger> getUpdateStringTupletupleOutput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        final Function function = new Function(FUNC_UPDATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int32>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<BigInteger>(

                (BigInteger) results.get(0).getValue()
                );
    }

    public TransactionReceipt update(List<Condition> conditions, Limit limit,
            List<UpdateField> updateFields) {
        final Function function = new Function(
                FUNC_UPDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions), 
                limit, 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<UpdateField>(UpdateField.class, updateFields)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodUpdateRawFunction(List<Condition> conditions, Limit limit,
            List<UpdateField> updateFields) throws ContractException {
        final Function function = new Function(FUNC_UPDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions), 
                limit, 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<UpdateField>(UpdateField.class, updateFields)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int32>() {}));
        return function;
    }

    public String getSignedTransactionForUpdate(List<Condition> conditions, Limit limit,
            List<UpdateField> updateFields) {
        final Function function = new Function(
                FUNC_UPDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions), 
                limit, 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<UpdateField>(UpdateField.class, updateFields)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String update(List<Condition> conditions, Limit limit, List<UpdateField> updateFields,
            TransactionCallback callback) {
        final Function function = new Function(
                FUNC_UPDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<Condition>(Condition.class, conditions), 
                limit, 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<UpdateField>(UpdateField.class, updateFields)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<DynamicArray<Condition>, Limit, DynamicArray<UpdateField>> getUpdateTupletupleTupleTupletupleInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_UPDATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Condition>>() {}, new TypeReference<Limit>() {}, new TypeReference<DynamicArray<UpdateField>>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<DynamicArray<Condition>, Limit, DynamicArray<UpdateField>>(

                new DynamicArray<>(Condition.class,(List<Condition>) results.get(0).getValue()), 
                (Limit) results.get(1), 
                new DynamicArray<>(UpdateField.class,(List<UpdateField>) results.get(2).getValue())
                );
    }

    public Tuple1<BigInteger> getUpdateTupletupleTupleTupletupleOutput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        final Function function = new Function(FUNC_UPDATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int32>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<BigInteger>(

                (BigInteger) results.get(0).getValue()
                );
    }

    public static Table load(String contractAddress, Client client, CryptoKeyPair credential) {
        return new Table(contractAddress, client, credential);
    }

    public static Table deploy(Client client, CryptoKeyPair credential) throws ContractException {
        return deploy(Table.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
    }

    public static class Condition extends DynamicStruct {
        public BigInteger op;

        public String value;

        public Condition(Uint8 op, Utf8String value) {
            super(op,value);
            this.op = op.getValue();
            this.value = value.getValue();
        }

        public Condition(BigInteger op, String value) {
            super(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(op),new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(value));
            this.op = op;
            this.value = value;
        }
    }

    public static class Entry extends DynamicStruct {
        public String key;

        public List<String> fields;

        public Entry(Utf8String key, DynamicArray<Utf8String> fields) {
            super(key,fields);
            this.key = key.getValue();
            this.fields = fields.getValue().stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String::getValue).collect(java.util.stream.Collectors.toList());
        }

        public Entry(String key, List<String> fields) {
            super(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(key),new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String>(org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class, fields.stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String::new).collect(java.util.stream.Collectors.toList())));
            this.key = key;
            this.fields = fields;
        }
    }

    public static class Limit extends StaticStruct {
        public BigInteger offset;

        public BigInteger count;

        public Limit(Uint32 offset, Uint32 count) {
            super(offset,count);
            this.offset = offset.getValue();
            this.count = count.getValue();
        }

        public Limit(BigInteger offset, BigInteger count) {
            super(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint32(offset),new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint32(count));
            this.offset = offset;
            this.count = count;
        }
    }

    public static class UpdateField extends DynamicStruct {
        public String columnName;

        public String value;

        public UpdateField(Utf8String columnName, Utf8String value) {
            super(columnName,value);
            this.columnName = columnName.getValue();
            this.value = value.getValue();
        }

        public UpdateField(String columnName, String value) {
            super(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(columnName),new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(value));
            this.columnName = columnName;
            this.value = value;
        }
    }
}
