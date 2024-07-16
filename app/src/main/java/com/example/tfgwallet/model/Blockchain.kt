package com.example.tfgwallet.model

import android.content.Context
import android.util.Log
import com.example.tfgwallet.R
import com.example.tfgwallet.contracts.SKM_SC
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
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson

object Blockchain {
    private lateinit var web3: Web3j
    private lateinit var gasProvider: StaticGasProvider
    private lateinit var contract: SKM_SC
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
            val gasPrice  = updateGasPrice()
            gasProvider =
                StaticGasProvider(gasPrice, BigInteger.valueOf(1721974))
        } catch (e: Exception) {
            Log.e("BC connection error", "Error connecting to the blockchain.${e.printStackTrace()}")
        }
    }

    /**
     * Method that calls sepolia beaconcha API to get the current gas price.
     * @return BigInteger indicating the current gas price.
     */
    private fun updateGasPrice(): BigInteger {
        val url = URL("https://sepolia.beaconcha.in/api/v1/execution/gasnow")

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET
            println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            if(responseCode == 200) {
                val response = inputStream.bufferedReader().use { it.readText() }
                val gson = Gson()
                val gasPriceResponse = gson.fromJson(response, Data.GasPriceResponse::class.java)
                println("Gas price (rapid): ${gasPriceResponse.data.rapid}")
                return BigInteger(gasPriceResponse.data.rapid.toString())
            }
            else { // return "default" gas price value
                return BigInteger.valueOf(40).multiply(BigInteger.valueOf(1000000000))
            }
        }
    }

    /**
     * Method that deploys a SKM smart contract in the BC.
     * @param context - Context of the app.
     * @param prefsName - String with the name of the preferences of the user where the hash of the contract is stored.
     * @return String with the contract address.
     */
    fun deploySKM_SC(context: Context, prefsName: String): String {
        val keyPair: Triple<BigInteger, BigInteger, ByteArray>? = KeyManagement.decryptRsa(
            "${prefsName.substringAfter("_")}/login${prefsName.substringAfter("_")}.bin",
            prefsName.substringAfter("_"),
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
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putString(prefsName + "_contract", contract.contractAddress).apply()
        return contract.contractAddress
    }

    /**
     * Method that calls the modTemp function defined in the SKMSC contract.
     * @param from - Credentials of the account calling the method.
     * @param value - ByteArray with the value to be stored in the temp field.
     * @param context - Context of the app.
     * @param prefsName - String with the name of the preferences of the user where the hash of the contract is stored.
     * @return String with the status of the transaction
     */
    fun modTemp(from: Credentials, value: ByteArray, context: Context, prefsName: String): String? {
        if (!::web3.isInitialized) {
            connect(context.getString(R.string.BLOCKCHAIN_IP))
        }
        if (!::contract.isInitialized) {
            return try {
                initContract(context, prefsName, from)
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


    /**
     * Method that calls the addDevice function defined in the SKMSC contract.
     * @param from - Credentials of the account calling the method.
     * @param plugin - Bip32ECKeyPair of the newly configured plugin.
     * @param context - Context of the app.
     * @param prefsName - String with the name of the preferences of the user where the hash of the contract is stored.
     * @return String with the status of the transaction
     */
    fun addDevice(
        from: Credentials,
        plugin: Bip32ECKeyPair,
        context: Context,
        prefsName: String
    ): String? {
        val pub = Credentials.create(plugin).address
        if (!::web3.isInitialized) {
            connect(context.getString(R.string.BLOCKCHAIN_IP))
        }
        if (!::contract.isInitialized) {
            return try {
                initContract(context, prefsName, from)

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
     * Method that send funds to the address specified.
     * @param context - Context of the running app.
     * @param recipientAddress - String with the address that will receive the funds.
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

    /**
     * Method that searches for the hash of the SC deployed using the specified
     * address. It uses the Sepolia Etherscan API.
     *
     * @param address - String with the address of the account that deployed the SC
     * @return String with the hash of the SC, empty String if not found
     */
    fun lookForSChashInBC(address: String): String {

        val url = URL("https://api-sepolia.etherscan.io/api" +
                "?module=account" +
                "&action=txlist" +
                "&address=${address}" +
                "&startblock=0" +
                "&endblock=99999999" +
                "&page=1" +
                "&offset=10" +
                "&sort=asc" +
                "&apikey=YourApiKeyToken")
        var contractAddress = ""
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET
            println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            if(responseCode == 200) {
                val response = inputStream.bufferedReader().use { it.readText() }
                val gson = Gson()
                val sepoliaAccountResponse = gson.fromJson(response, Data.SepoliaAccountResponse::class.java)
                for(result in sepoliaAccountResponse.result) {
                    if (result.to == "") {  // The contract deployment has "to" address empty
                        contractAddress = result.contractAddress
                        break // found
                    }
                }
            }
        }
        return contractAddress
    }

    /**
     * Private method that initializes the contract class variable so that other functions
     * can interact with it without errors.
     * @param context - Context of the app.
     * @param prefsName - String with the name of the preferences of the user where the hash of the contract is stored.
     * @param from - Credentials of the account that deployed the contract.
     */
    private fun initContract(context: Context, prefsName: String, from: Credentials) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val address = prefs.getString(prefsName + "_contract", null)
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

    /**
     * Private method that based on a transaction hash, interacts with the BC to get its receipt.
     * @param transactionHash - String with the transaction hash.
     * @return EthTransactionReceipt
     */
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
}
