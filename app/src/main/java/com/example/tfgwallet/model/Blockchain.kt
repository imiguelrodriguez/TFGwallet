package com.example.tfgwallet.model

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.tfgwallet.R
import com.example.tfgwallet.SKM_SC.PPMWallet.src.contracts.SKM_SC
import io.ipfs.kotlin.IPFS
import io.ipfs.kotlin.IPFSConfiguration
import org.json.JSONObject
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.utils.Numeric
import java.io.BufferedReader
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class GasPriceResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: GasData
)

data class GasData(
    @SerializedName("rapid") val rapid: Long,
    @SerializedName("fast") val fast: Long,
    @SerializedName("standard") val standard: Long,
    @SerializedName("slow") val slow: Long,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("price") val price: Int,
    @SerializedName("priceUSD") val priceUSD: Int
)


object Blockchain {
    lateinit var web3: Web3j
    lateinit var gasProvider: StaticGasProvider
    lateinit var contract: SKM_SC
    private const val CHAIN_ID: Long = 11155111
    /*
        Please note that this private constants are just for the purpose of development, they should be
        removed if the app went to a production phase.
     */
    private const val MAIN_ADDRESS: String = "0x2be7268d90B418af669BFf590f2F31Cf73068916"
    private const val MAIN_KEY: String = "0x4a5ff756fe52d4699b68a1ec160f506cc3a9eb5ea5bbce54abb2d7851359d233"
    private fun connect(url: String) {
        try {
            web3 = Web3j.build(HttpService(url))
            Log.i("BC connection", "Successful connection to the blockchain.")

            gasProvider =
                StaticGasProvider(updateGasPrice(), BigInteger.valueOf(1721974))
        } catch (e: Exception) {
            Log.e("BC connection error", "Error connecting to the blockchain.${e.printStackTrace().toString()}")
        }
    }

    fun updateGasPrice(): BigInteger {
        val url = URL("https://sepolia.beaconcha.in/api/v1/execution/gasnow")

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET
            println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            if(responseCode == 200) {
                val response = inputStream.bufferedReader().use { it.readText() }
                val gson = Gson()
                val gasPriceResponse = gson.fromJson(response, GasPriceResponse::class.java)
                println("Gas price (rapid): ${gasPriceResponse.data.rapid}")
                return BigInteger(gasPriceResponse.data.rapid.toString())
            }
            else { // return "default" gas price value
                return BigInteger.valueOf(40).multiply(BigInteger.valueOf(1000000000))
            }
        }
    }



    fun deploySKM_SC(context: Context, prefs_name: String): String {
        val keyPair: Triple<BigInteger, BigInteger, ByteArray>? = KeyManagement.decryptRsa(
            "${prefs_name.substringAfter("_")}/login${prefs_name.substringAfter("_")}.bin",
            prefs_name.substringAfter("_"),
            context
        )
        if (!::web3.isInitialized) {
            connect(context.getString(R.string.BLOCKCHAIN_IP))
        }
        if (keyPair != null) {
            try {
                Log.i("Private key", keyPair.first.toString(16))
                contract = SKM_SC.deploy(web3, RawTransactionManager(web3, Credentials.create(keyPair.first.toString(16)), CHAIN_ID), gasProvider).send()
            } catch (e: Exception) {
                Log.e("error", e.toString())
            }
        }
        val prefs = context.getSharedPreferences(prefs_name, Context.MODE_PRIVATE)
        prefs.edit().putString(prefs_name + "_contract", contract.contractAddress).apply()
        return contract.contractAddress
    }

    fun modTemp(from: Credentials, value: ByteArray, context: Context, prefs_name: String): String? {
        if (!::web3.isInitialized) {
            connect(context.getString(R.string.BLOCKCHAIN_IP))
        }
        if (!::contract.isInitialized) {
            return try {
                initContract(context, prefs_name, from)
                val transactionHash = contract.modTemp(value).send().transactionHash
                val transactionReceipt = getTxReceipt(transactionHash)
                transactionReceipt?.result?.status
            } catch (e: Exception) {
                Log.e("Error", e.toString())
                e.message
            }
        } else {
            return try {
                val transactionHash = contract.modTemp(value).send().transactionHash
                val transactionReceipt = getTxReceipt(transactionHash)
                transactionReceipt?.result?.status
            } catch (e: Exception) {
                Log.e("Error", e.toString())
                e.message
            }
        }
    }
    private fun initContract(context: Context, prefs_name: String, from: Credentials) {
        val prefs = context.getSharedPreferences(prefs_name, Context.MODE_PRIVATE)
        val address = prefs.getString(prefs_name + "_contract", null)
        contract = SKM_SC.load(
            address,
            web3,
            RawTransactionManager(
                web3,
                from, CHAIN_ID
            ),
            gasProvider
        )
    }
    private fun getTxReceipt(transactionHash: String): EthGetTransactionReceipt? {
        var transactionReceipt = web3.ethGetTransactionReceipt(transactionHash).send()

        while (true) {
            if (transactionReceipt.result != null) {
                break
            }
            println(transactionReceipt.result)
            Thread.sleep(5000)
            transactionReceipt = web3.ethGetTransactionReceipt(transactionHash).send()
        }
        return transactionReceipt
    }
    fun addDevice(
        from: Credentials,
        plugin: Bip32ECKeyPair,
        context: Context,
        prefs_name: String
    ): String? {
        val pub = Credentials.create(plugin).address
        if (!::web3.isInitialized) {
            connect(context.getString(R.string.BLOCKCHAIN_IP))
        }
        if (!::contract.isInitialized) {
            return try {
                initContract(context, prefs_name, from)

                val transactionHash = contract.addDevice(pub).send().transactionHash
                val transactionReceipt = getTxReceipt(transactionHash)
                transactionReceipt?.result?.status
            } catch (e: Exception) {
                Log.e("Error", e.toString())
                e.message
            }
        } else {
            return try {
                val transactionHash = contract.addDevice(pub).send().transactionHash
                val transactionReceipt = getTxReceipt(transactionHash)
                transactionReceipt?.result?.status
            } catch (e: Exception) {
                Log.e("Error", e.toString())
                e.message
            }
        }
    }

    /**
     *
     */
    fun send(context: Context, recipientAddress: String) {
        if (!::web3.isInitialized) {
            connect(context.getString(R.string.BLOCKCHAIN_IP))
        }

        val ethGetTransactionCount = web3.ethGetTransactionCount(
            MAIN_ADDRESS, DefaultBlockParameter.valueOf("latest")
        ).send()

        val nonce = ethGetTransactionCount.transactionCount
        val gasPrice = gasProvider.gasPrice
        val gasLimit = gasProvider.gasLimit
        val value = BigInteger.valueOf(500000000000000000) // Amount in wei (0.5 ETH)
        val data = "" // Optional data field

        val rawTransaction = RawTransaction.createEtherTransaction(
            nonce, gasPrice, gasLimit, recipientAddress, value
        )

        val signedMessage = TransactionEncoder.signMessage(rawTransaction, CHAIN_ID, Credentials.create(
            MAIN_KEY))

        val hexValue = Numeric.toHexString(signedMessage)

    try {
            val ethSendTransaction = web3.ethSendRawTransaction(hexValue).sendAsync().get()
            val transactionHash = ethSendTransaction.transactionHash
            Log.i("Tx hash", transactionHash)
            while (true) {
                val transactionReceipt = web3.ethGetTransactionReceipt(transactionHash).send()
                if (transactionReceipt.result != null) {
                    break
                }
                println(transactionReceipt.result)
                Thread.sleep(5000)
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    fun deploySKM_SC2(senderAddress: String, privateKey: String): String {

        if (!::web3.isInitialized) {
            connect("https://rpc.sepolia.org")
        }
            try {
                val ethGetTransactionCount = web3.ethGetTransactionCount(
                    senderAddress, DefaultBlockParameter.valueOf("latest")
                ).send()

                SKM_SC.deploy(web3, RawTransactionManager(web3, Credentials.create(privateKey), 11155111), StaticGasProvider(BigInteger.valueOf(69).multiply(BigInteger.valueOf(1000000000)), BigInteger.valueOf(6721974))).send()
                val nonce = ethGetTransactionCount.transactionCount.add(BigInteger.ONE)
                val gasPrice = BigInteger.valueOf(69).multiply(BigInteger.valueOf(1000000000)); // 12 Gwei
                val gasLimit = BigInteger.valueOf(6721974)

                val rawTransaction = RawTransaction.createContractTransaction(
                    nonce,
                    gasPrice,
                    gasLimit, BigInteger.ZERO,
                    "608060405234801561000f575f80fd5b50335f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550610cfc8061005c5f395ff3fe608060405234801561000f575f80fd5b5060043610610091575f3560e01c80639525256011610064578063952525601461011b578063ad70dd5b14610139578063b111cb8414610155578063bf28921614610171578063c1c6b6b61461018d57610091565b80631d6d32ba146100955780631f3a2ae2146100b35780631f7b6324146100cf5780635a7db533146100eb575b5f80fd5b61009d6101ab565b6040516100aa91906109d5565b60405180910390f35b6100cd60048036038101906100c89190610a76565b61027d565b005b6100e960048036038101906100e49190610ab4565b6103de565b005b61010560048036038101906101009190610ab4565b61054f565b60405161011291906109d5565b60405180910390f35b6101236106b1565b60405161013091906109d5565b60405180910390f35b610153600480360381019061014e9190610adf565b6106ba565b005b61016f600480360381019061016a9190610ab4565b61078d565b005b61018b60048036038101906101869190610adf565b6108ff565b005b610195610996565b6040516101a29190610b19565b60405180910390f35b5f60015f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f015f9054906101000a900460ff16610237576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161022e90610b8c565b60405180910390fd5b60015f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2060010154905090565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461030a576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161030190610c1a565b60405180910390fd5b60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f015f9054906101000a900460ff16610395576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161038c90610b8c565b60405180910390fd5b8060015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f20600101819055505050565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461046b576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161046290610c1a565b60405180910390fd5b60015f8273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f015f9054906101000a900460ff166104f6576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016104ed90610b8c565b60405180910390fd5b5f60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f015f6101000a81548160ff02191690831515021790555050565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16146105de576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016105d590610c1a565b60405180910390fd5b60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f015f9054906101000a900460ff16610669576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161066090610b8c565b60405180910390fd5b60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f20600101549050919050565b5f600254905090565b60015f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f015f9054906101000a900460ff16610745576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161073c90610b8c565b60405180910390fd5b8060015f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f206001018190555050565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461081a576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161081190610c1a565b60405180910390fd5b60015f8273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f015f9054906101000a900460ff16156108a6576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161089d90610ca8565b60405180910390fd5b6001805f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f015f6101000a81548160ff02191690831515021790555050565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461098c576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161098390610c1a565b60405180910390fd5b8060028190555050565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b5f819050919050565b6109cf816109bd565b82525050565b5f6020820190506109e85f8301846109c6565b92915050565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610a1b826109f2565b9050919050565b610a2b81610a11565b8114610a35575f80fd5b50565b5f81359050610a4681610a22565b92915050565b610a55816109bd565b8114610a5f575f80fd5b50565b5f81359050610a7081610a4c565b92915050565b5f8060408385031215610a8c57610a8b6109ee565b5b5f610a9985828601610a38565b9250506020610aaa85828601610a62565b9150509250929050565b5f60208284031215610ac957610ac86109ee565b5b5f610ad684828501610a38565b91505092915050565b5f60208284031215610af457610af36109ee565b5b5f610b0184828501610a62565b91505092915050565b610b1381610a11565b82525050565b5f602082019050610b2c5f830184610b0a565b92915050565b5f82825260208201905092915050565b7f5468697320706c75672d696e206973206e6f7420636f6e666967757265642e005f82015250565b5f610b76601f83610b32565b9150610b8182610b42565b602082019050919050565b5f6020820190508181035f830152610ba381610b6a565b9050919050565b7f4f6e6c792074686520736d61727470686f6e6520697320616c6c6f77656420745f8201527f6f20646f207468697320616374696f6e2e000000000000000000000000000000602082015250565b5f610c04603183610b32565b9150610c0f82610baa565b604082019050919050565b5f6020820190508181035f830152610c3181610bf8565b9050919050565b7f5468697320706c75672d696e2068617320616c7265616479206265656e20636f5f8201527f6e666967757265642e0000000000000000000000000000000000000000000000602082015250565b5f610c92602983610b32565b9150610c9d82610c38565b604082019050919050565b5f6020820190508181035f830152610cbf81610c86565b905091905056fea2646970667358221220cd4383c90918d599040232fcb2803ad95bd6907a5b1dcdafd9b0f6227f2f559b64736f6c63430008180033")

                val signedMessage = TransactionEncoder.signMessage(rawTransaction, 11155111L, Credentials.create(privateKey))

                val hexValue = Numeric.toHexString(signedMessage)
                val ethSendTransaction = web3.ethSendRawTransaction(hexValue).send()
                val transactionHash = ethSendTransaction.transactionHash

                println(transactionHash)
                while (true) {
                    var transactionReceipt = web3.ethGetTransactionReceipt(transactionHash).send()
                    if (transactionReceipt.result != null) {
                        break
                    }
                    println(transactionReceipt.result)
                    Thread.sleep(5000)
                }
            } catch (e: Exception) {
                println(e)
            }
        return contract.contractAddress
    }

}
fun main() {
    Blockchain.updateGasPrice()
}