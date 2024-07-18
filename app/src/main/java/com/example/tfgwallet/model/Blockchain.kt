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
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson
import org.web3j.abi.datatypes.Function
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthEstimateGas

object Blockchain {
    private lateinit var web3: Web3j
    private lateinit var contract: SKM_SC
    private const val CHAIN_ID: Long = 11155111
    private val DEFAULT_GAS_LIMIT : BigInteger = BigInteger.valueOf(1721974)
    /*
        Please note that this private constants are just for the purpose of development, they should be
        removed if the app went to a production phase.
     */
    private const val MAIN_ADDRESS: String = "0x2be7268d90B418af669BFf590f2F31Cf73068916"
    private const val MAIN_KEY: String = "0x4a5ff756fe52d4699b68a1ec160f506cc3a9eb5ea5bbce54abb2d7851359d233"

    /**
     * Method that initializes the web3 library using the provided URL as access point.
     * @param url String indicating the URL to be used in the connection (e.g. sepolia testnet, infura, local ganache...)
     */
    private fun connect(url: String) {
        try {
            web3 = Web3j.build(HttpService(url))
            Log.i("BC connection", "Successful connection to the blockchain.")
        } catch (e: Exception) {
            Log.e("BC connection error", "Error connecting to the blockchain.${e.printStackTrace()}")
        }
    }

    /**
     * Method that deploys a SKM smart contract in the BC.
     * @param context Context of the app.
     * @param prefsName String with the name of the preferences of the user where the hash of the contract is stored.
     * @return String with the contract address or null if error.
     */
    fun deploySKMSC(context: Context, prefsName: String): String? {
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
                val nonce: BigInteger =
                    web3.ethGetTransactionCount(MAIN_ADDRESS, DefaultBlockParameterName.LATEST)
                        .send()
                        .transactionCount

                val contractBinary = SKM_SC.BINARY

                // Create a transaction object to estimate gas
                val tx = Transaction.createContractTransaction(
                    MAIN_ADDRESS,
                    nonce,
                    web3.ethGasPrice().send().gasPrice,
                    BigInteger.ZERO, // This is the gas limit, set it to zero for estimation
                    BigInteger.ZERO, // Value to send with the transaction
                    contractBinary
                )

                val ethEstimateGas: EthEstimateGas = web3.ethEstimateGas(tx).send()

                val gasEstimate: BigInteger? = ethEstimateGas.amountUsed
                if (gasEstimate != null) {
                    Log.i("Deploy","Estimated Gas for contract deployment: $gasEstimate")
                } else {
                    Log.e("Deploy","Error estimating gas: ${ethEstimateGas.error.message}")
                }
                contract = SKM_SC.deploy(web3, RawTransactionManager(web3,
                    Credentials.create(keyPair.first.toString(16)),
                    CHAIN_ID), StaticGasProvider(updateGasPrice(), gasEstimate)).send()
                val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                prefs.edit().putString(prefsName + "_contract", contract.contractAddress).apply()
                return contract.contractAddress
            } catch (e: Exception) {
                Log.e("error", e.toString())
                return null
            }

        }
        return null
    }

    /**
     * Method that calls the modTemp function defined in the SKMSC contract.
     * @param from Credentials of the account calling the method.
     * @param value ByteArray with the value to be stored in the temp field.
     * @param context Context of the app.
     * @param prefsName String with the name of the preferences of the user where the hash of the contract is stored.
     * @return transactionReceipt status or null if error.
     */
    fun modTemp(from: Credentials, value: ByteArray, context: Context, prefsName: String): String? {
        if (!::web3.isInitialized) {
            connect(context.getString(R.string.BLOCKCHAIN_IP))
        }
        val gasLimit = estimateGasContractCall("modTemp", value, from.address)
        if (!::contract.isInitialized) {
            return try {
                initContract(
                    context,
                    prefsName,
                    from,
                    StaticGasProvider(updateGasPrice(), gasLimit)
                )
                val transactionHash = contract.modTemp(value).send().transactionHash
                val transactionReceipt = getTxReceipt(transactionHash)
                transactionReceipt?.result?.status
            } catch (e: Exception) {
                Log.e("Error", e.toString())
                e.message
            }
        } else {
            return try {
                contract.setGasProvider(StaticGasProvider(updateGasPrice(), gasLimit))
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
     * Method that calls the getRef function defined in the SKMSC contract.
     * @param from Credentials of the account calling the method.
     * @param deviceID String with the ID of the plugin.
     * @param context Context of the app.
     * @param prefsName String with the name of the preferences of the user where the hash of the contract is stored.
     * @return transactionReceipt status or null if error.
     */
    fun getRef(from: Credentials, deviceID: String, context: Context, prefsName: String): String? {
        if (!::web3.isInitialized) {
            connect(context.getString(R.string.BLOCKCHAIN_IP))
        }
        if (!::contract.isInitialized) {
            return try {
                initContract(
                    context,
                    prefsName,
                    from,
                    StaticGasProvider(updateGasPrice(), DEFAULT_GAS_LIMIT)
                )
                contract.getRef(deviceID).send().toString()
            } catch (e: Exception) {
                Log.e("Error", e.toString())
                e.message
            }
        } else {
            return try {
                contract.getRef(deviceID).send().toString()
            } catch (e: Exception) {
                Log.e("Error", e.toString())
                e.message
            }
        }
    }


    /**
     * Method that calls the addDevice function defined in the SKMSC contract.
     * @param from Credentials of the account calling the method.
     * @param plugin Bip32ECKeyPair of the newly configured plugin.
     * @param context Context of the app.
     * @param prefsName String with the name of the preferences of the user where the hash of the contract is stored.
     * @return transactionReceipt status or null if error.
     */
    fun addDevice(
        from: Credentials,
        plugin: Bip32ECKeyPair,
        context: Context,
        prefsName: String
    ): String? {
        val pub = Credentials.create(plugin).address
        val gasLimit = estimateGasContractCall("addDevice", pub, from.address)

        if (!::web3.isInitialized) {
            connect(context.getString(R.string.BLOCKCHAIN_IP))
        }
        if (!::contract.isInitialized) {
            return try {
                initContract(context, prefsName, from, StaticGasProvider(updateGasPrice(), gasLimit))
                val transactionHash = contract.addDevice(pub).send().transactionHash
                val transactionReceipt = getTxReceipt(transactionHash)
                transactionReceipt?.result?.status
            } catch (e: Exception) {
                Log.e("Error", e.toString())
                e.message
            }
        } else {
            return try {
                contract.setGasProvider(StaticGasProvider(updateGasPrice(), gasLimit))
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
     * @param context Context of the running app.
     * @param recipientAddress String with the address that will receive the funds.
     * @return transactionReceipt status or null if error.
     */
    fun send(context: Context, recipientAddress: String): String? {
        if (!::web3.isInitialized) {
            connect(context.getString(R.string.BLOCKCHAIN_IP))
        }

        val ethGetTransactionCount = web3.ethGetTransactionCount(
            MAIN_ADDRESS, DefaultBlockParameterName.LATEST
        ).send()

        val nonce = ethGetTransactionCount.transactionCount
        val value = BigInteger.valueOf(1000000000000000000) // Amount in wei (1 ETH)

        return try {
            val gasPrice: BigInteger = updateGasPrice()
            val gasLimit = web3.ethEstimateGas(Transaction(MAIN_ADDRESS, nonce, gasPrice, BigInteger.ZERO, recipientAddress, value, "")).send().amountUsed
            val rawTransaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit, recipientAddress, value
            )
            val signedMessage = TransactionEncoder.signMessage(rawTransaction, CHAIN_ID, Credentials.create(
                MAIN_KEY))

            val hexValue = Numeric.toHexString(signedMessage)
            val ethSendTransaction = web3.ethSendRawTransaction(hexValue).sendAsync().get()
            val transactionHash = ethSendTransaction.transactionHash
            val transactionReceipt = getTxReceipt(transactionHash)
            transactionReceipt?.result?.status

        } catch (e: Exception) {
            Log.e("Error", e.toString())
            e.message
        }
    }


    /**
     * Method that estimates the gas cost used in a smart contract call.
     * @param functionName String indicating the name of the function to be called.
     * @param parameter Parameter to pass as argument to the function called.
     * @param from String indicating the calling address.
     * @return BigInteger indicating the estimated gas cost.
     */
    private fun estimateGasContractCall(functionName: String, parameter: Any, from: String): BigInteger? {
        val function: Function?
        when (parameter) {
            is String -> {
                function = Function(
                    functionName,
                    listOf(org.web3j.abi.datatypes.Utf8String(parameter)), // Input parameters
                    listOf() // Output parameters
                )
            }
            is ByteArray -> {
                function = Function(
                    functionName,
                    listOf(org.web3j.abi.datatypes.DynamicBytes(parameter)), // Input parameters
                    listOf() // Output parameters
                )
            }
            else -> {
                return null
            }
        }

        val encodedFunction = org.web3j.abi.FunctionEncoder.encode(function)
        val ethGetTransactionCount = web3.ethGetTransactionCount(
            from, DefaultBlockParameterName.LATEST
        ).send()

        val nonce = ethGetTransactionCount.transactionCount
        val contractTransaction = Transaction.createFunctionCallTransaction(
            from,
            nonce,
            updateGasPrice(),
            BigInteger.ZERO,
            contract.contractAddress,
            encodedFunction
        )

        return web3.ethEstimateGas(contractTransaction).send().amountUsed
    }

    /**
     * Method that searches for the hash of the SC deployed using the specified
     * address. It uses the Sepolia Etherscan API.
     *
     * @param address String with the address of the account that deployed the SC
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
     * @param context Context of the app.
     * @param prefsName String with the name of the preferences of the user where the hash of the contract is stored.
     * @param from Credentials of the account that deployed the contract.
     */
    private fun initContract(
        context: Context,
        prefsName: String,
        from: Credentials,
        gasProvider: StaticGasProvider
    ) {
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
     * @param transactionHash String with the transaction hash.
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

    /**
     * Method that calls sepolia beacon chain API to get the current gas price.
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

}
