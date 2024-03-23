package com.example.tfgwallet.model

import io.ipfs.kotlin.IPFS
import io.ipfs.kotlin.IPFSConfiguration
import io.ipfs.kotlin.model.NamedHash
import java.io.File

class IPFSManager  {
    private var manager: IPFS
    init {
        manager = IPFS(IPFSConfiguration(base_url = "http://192.168.100.33:5001/api/v0/"))
    }
    fun addFile(name: String): NamedHash {
        return manager.add.file(File(name), name)
    }


}

fun main() {
    val currentDir = System.getProperty("user.dir")
    println("Current working directory: $currentDir")
    val ipfs = IPFS(IPFSConfiguration(base_url = "http://127.0.0.1:5001/api/v0/"))
   // val f = ipfs.add.file(File("app/src/main/assets/english.txt"), "english.txt")
   // println("Hash: " + f.Hash + " for file name " + f.Name)
    println(ipfs.get.cat("QmboH69rqqevyHjtPp4d8ujvAMHZCCc7KEu4AEds9EHspx"))
    println(ipfs.get.cat("QmWr7iqiych7ZMYjiQDuSwDFrRNuNdWxKVzwSxwbaAmeXV"))

}