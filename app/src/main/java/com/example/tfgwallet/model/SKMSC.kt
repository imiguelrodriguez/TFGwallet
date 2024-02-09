package com.example.tfgwallet.model


import java.util.LinkedList

class SKMSC (smartphoneID: String, digitalSignature: String){
    private var smartphoneID: String = ""
    private var whiteList: LinkedList<String> = LinkedList()
    private var refsList: HashMap<String, String> = HashMap(0)
    private var temp: String = ""
    init {
        var res = false
        if(verify(smartphoneID, digitalSignature)){
            this.smartphoneID = smartphoneID
            res = true
        }

    }
    /*
    fun connectToGanache() {

        val web3 = Web3j.build(HttpService("http://10.0.2.2:7545")) // Assuming Ganache is running on localhost:7545
// Set up credentials
        val credentials = Credentials.create("your_private_key") // Replace with your private key

        // Deploy the contract
        val contract = MyContract.deploy(
            web3,
            credentials,
            ManagedTransaction.GAS_PRICE,
            Contract.GAS_LIMIT,
            "Initial message" // Constructor parameter
        ).send()

        // Retrieve the contract address
        val contractAddress = contract.contractAddress
        println("Contract deployed at: $contractAddress")
    }*/

    fun addDevice(deviceID: String, digitalSignature: String): Boolean {
        var res = false
        if(verify(smartphoneID, digitalSignature)) {
            whiteList.add(deviceID)
            res = true
        }
        return res
    }

    fun storeRef(deviceID: String, digitalSignature: String, IPFSreference: String): Boolean {
        var res = false
        if(verify(deviceID, digitalSignature)) {
            if (deviceID in whiteList) {
              refsList[deviceID] = IPFSreference
              res = true
            }
        }
        return res
    }

    fun modTemp(newTemp: String, digitalSignature: String): Boolean {
        var res = false
        if(verify(smartphoneID, digitalSignature)) {
            temp = newTemp
            res = true
        }
        return res
    }
    // Just to avoid errors!
    fun verify(x: String, y: String): Boolean {
        return true
    }
 }