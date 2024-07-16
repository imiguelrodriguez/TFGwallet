package com.example.tfgwallet.contracts;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.5.0.
 */
@SuppressWarnings("rawtypes")
public class SKM_SC extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b50336000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506112f0806100606000396000f3fe608060405234801561001057600080fd5b50600436106100935760003560e01c806390cb92f71161006657806390cb92f71461011e578063952525601461013a578063b111cb8414610158578063c1c6b6b614610174578063ff2106361461019257610093565b80631d6d32ba146100985780631f7b6324146100b65780633fbb9407146100d25780635a7db533146100ee575b600080fd5b6100a06101ae565b6040516100ad9190611073565b60405180910390f35b6100d060048036038101906100cb9190610da6565b61030f565b005b6100ec60048036038101906100e79190610e68565b61048a565b005b61010860048036038101906101039190610da6565b610573565b6040516101159190611073565b60405180910390f35b61013860048036038101906101339190610e23565b610764565b005b610142610808565b60405161014f9190611051565b60405180910390f35b610172600480360381019061016d9190610da6565b61089a565b005b61017c610a15565b6040516101899190611036565b60405180910390f35b6101ac60048036038101906101a79190610dcf565b610a3e565b005b6060600160003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160009054906101000a900460ff1661023f576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610236906110d5565b60405180910390fd5b600160003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600101805461028c90611202565b80601f01602080910402602001604051908101604052809291908181526020018280546102b890611202565b80156103055780601f106102da57610100808354040283529160200191610305565b820191906000526020600020905b8154815290600101906020018083116102e857829003601f168201915b5050505050905090565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461039d576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161039490611095565b60405180910390fd5b600160008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160009054906101000a900460ff1661042c576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610423906110d5565b60405180910390fd5b6000600160008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160006101000a81548160ff02191690831515021790555050565b600160003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160009054906101000a900460ff16610519576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610510906110d5565b60405180910390fd5b80600160003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600101908051906020019061056f929190610bb6565b5050565b606060008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610603576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016105fa90611095565b60405180910390fd5b600160008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160009054906101000a900460ff16610692576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610689906110d5565b60405180910390fd5b600160008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060010180546106df90611202565b80601f016020809104026020016040519081016040528092919081815260200182805461070b90611202565b80156107585780601f1061072d57610100808354040283529160200191610758565b820191906000526020600020905b81548152906001019060200180831161073b57829003601f168201915b50505050509050919050565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16146107f2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016107e990611095565b60405180910390fd5b818160029190610803929190610c3c565b505050565b60606002805461081790611202565b80601f016020809104026020016040519081016040528092919081815260200182805461084390611202565b80156108905780601f1061086557610100808354040283529160200191610890565b820191906000526020600020905b81548152906001019060200180831161087357829003601f168201915b5050505050905090565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610928576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161091f90611095565b60405180910390fd5b600160008273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160009054906101000a900460ff16156109b8576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016109af906110b5565b60405180910390fd5b60018060008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160006101000a81548160ff02191690831515021790555050565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610acc576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610ac390611095565b60405180910390fd5b600160008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160009054906101000a900460ff16610b5b576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610b52906110d5565b60405180910390fd5b80600160008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206001019080519060200190610bb1929190610bb6565b505050565b828054610bc290611202565b90600052602060002090601f016020900481019282610be45760008555610c2b565b82601f10610bfd57805160ff1916838001178555610c2b565b82800160010185558215610c2b579182015b82811115610c2a578251825591602001919060010190610c0f565b5b509050610c389190610cc2565b5090565b828054610c4890611202565b90600052602060002090601f016020900481019282610c6a5760008555610cb1565b82601f10610c8357803560ff1916838001178555610cb1565b82800160010185558215610cb1579182015b82811115610cb0578235825591602001919060010190610c95565b5b509050610cbe9190610cc2565b5090565b5b80821115610cdb576000816000905550600101610cc3565b5090565b6000610cf2610ced84611126565b6110f5565b905082815260208101848484011115610d0a57600080fd5b610d158482856111c0565b509392505050565b600081359050610d2c816112a3565b92915050565b60008083601f840112610d4457600080fd5b8235905067ffffffffffffffff811115610d5d57600080fd5b602083019150836001820283011115610d7557600080fd5b9250929050565b600082601f830112610d8d57600080fd5b8135610d9d848260208601610cdf565b91505092915050565b600060208284031215610db857600080fd5b6000610dc684828501610d1d565b91505092915050565b60008060408385031215610de257600080fd5b6000610df085828601610d1d565b925050602083013567ffffffffffffffff811115610e0d57600080fd5b610e1985828601610d7c565b9150509250929050565b60008060208385031215610e3657600080fd5b600083013567ffffffffffffffff811115610e5057600080fd5b610e5c85828601610d32565b92509250509250929050565b600060208284031215610e7a57600080fd5b600082013567ffffffffffffffff811115610e9457600080fd5b610ea084828501610d7c565b91505092915050565b610eb28161118e565b82525050565b6000610ec382611156565b610ecd818561116c565b9350610edd8185602086016111cf565b610ee681611292565b840191505092915050565b6000610efc82611161565b610f06818561117d565b9350610f168185602086016111cf565b610f1f81611292565b840191505092915050565b6000610f3760318361117d565b91507f4f6e6c792074686520736d61727470686f6e6520697320616c6c6f776564207460008301527f6f20646f207468697320616374696f6e2e0000000000000000000000000000006020830152604082019050919050565b6000610f9d60298361117d565b91507f5468697320706c75672d696e2068617320616c7265616479206265656e20636f60008301527f6e666967757265642e00000000000000000000000000000000000000000000006020830152604082019050919050565b6000611003601f8361117d565b91507f5468697320706c75672d696e206973206e6f7420636f6e666967757265642e006000830152602082019050919050565b600060208201905061104b6000830184610ea9565b92915050565b6000602082019050818103600083015261106b8184610eb8565b905092915050565b6000602082019050818103600083015261108d8184610ef1565b905092915050565b600060208201905081810360008301526110ae81610f2a565b9050919050565b600060208201905081810360008301526110ce81610f90565b9050919050565b600060208201905081810360008301526110ee81610ff6565b9050919050565b6000604051905081810181811067ffffffffffffffff8211171561111c5761111b611263565b5b8060405250919050565b600067ffffffffffffffff82111561114157611140611263565b5b601f19601f8301169050602081019050919050565b600081519050919050565b600081519050919050565b600082825260208201905092915050565b600082825260208201905092915050565b6000611199826111a0565b9050919050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b82818337600083830152505050565b60005b838110156111ed5780820151818401526020810190506111d2565b838111156111fc576000848401525b50505050565b6000600282049050600182168061121a57607f821691505b6020821081141561122e5761122d611234565b5b50919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b6000601f19601f8301169050919050565b6112ac8161118e565b81146112b757600080fd5b5056fea2646970667358221220194841c2e2608d8077f64783d0a560a93e79585fd7cc469359d8d9b8179fa82a64736f6c63430008000033";

    public static final String FUNC_ADDDEVICE = "addDevice";

    public static final String FUNC_REMOVEDEVICE = "removeDevice";

    public static final String FUNC_storeRef = "storeRef";

    public static final String FUNC_getRef = "getRef";

    public static final String FUNC_MODTEMP = "modTemp";

    public static final String FUNC_GETTEMP = "getTemp";

    public static final String FUNC_GETSMARTPHONEID = "getSmartphoneID";

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
    }

    @Deprecated
    protected SKM_SC(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected SKM_SC(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected SKM_SC(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected SKM_SC(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> addDevice(String deviceID) {
        final Function function = new Function(
                FUNC_ADDDEVICE, 
                Arrays.<Type>asList(new Address(deviceID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> removeDevice(String deviceID) {
        final Function function = new Function(
                FUNC_REMOVEDEVICE, 
                Arrays.<Type>asList(new Address(deviceID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> storeRef(String IPFSref) {
        final Function function = new Function(
                FUNC_storeRef, 
                Arrays.<Type>asList(new Utf8String(IPFSref)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> storeRef(String deviceID, String IPFSref) {
        final Function function = new Function(
                FUNC_storeRef, 
                Arrays.<Type>asList(new Address(deviceID),
                new Utf8String(IPFSref)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> getRef() {
        final Function function = new Function(FUNC_getRef, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> getRef(String deviceID) {
        final Function function = new Function(FUNC_getRef, 
                Arrays.<Type>asList(new Address(deviceID)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> modTemp(byte[] newTemp) {
        final Function function = new Function(
                FUNC_MODTEMP, 
                Arrays.<Type>asList(new DynamicBytes(newTemp)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> getTemp() {
        final Function function = new Function(FUNC_GETTEMP, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<String> getSmartphoneID() {
        final Function function = new Function(FUNC_GETSMARTPHONEID, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static SKM_SC load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new SKM_SC(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static SKM_SC load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new SKM_SC(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static SKM_SC load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new SKM_SC(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static SKM_SC load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new SKM_SC(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<SKM_SC> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SKM_SC.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<SKM_SC> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SKM_SC.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<SKM_SC> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SKM_SC.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<SKM_SC> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SKM_SC.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }
}
