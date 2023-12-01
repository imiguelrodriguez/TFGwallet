package com.example.tfgwallet.model

import java.util.LinkedList

class SKMSC (smartphoneID: String, digitalSignature: String){
    var smartphoneID: String = ""
    var whiteList: LinkedList<String> = LinkedList()
    var refsList: HashMap<String, String> = HashMap(0)
    var temp: String = ""
    init {
        var res = false
        if(verify(smartphoneID.toByteArray(), digitalSignature.toByteArray())){
            this.smartphoneID = smartphoneID
            res = true
        }

    }

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
 }