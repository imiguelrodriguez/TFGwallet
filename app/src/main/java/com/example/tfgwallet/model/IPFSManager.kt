package com.example.tfgwallet.model

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.tfgwallet.R
import io.ipfs.kotlin.IPFS
import io.ipfs.kotlin.IPFSConfiguration
import io.ipfs.kotlin.model.NamedHash
import java.io.File


object IPFSManager  {
    private lateinit var manager: IPFS

    fun connect(context: Context) {
        manager = IPFS(IPFSConfiguration(base_url = ContextCompat.getString(context, R.string.IPFS_IP)))
    }
    fun addFile(name: String): NamedHash {
        return manager.add.file(File(name), name)
    }

    fun addString(name: String, text: String): NamedHash {
        return manager.add.string(text, name)
    }
    fun getString(hash: String): String {
        return manager.get.cat(hash)
    }

}

fun main() {
    /*val currentDir = System.getProperty("user.dir")
    println("Current working directory: $currentDir")*/
    val ipfs = IPFS(IPFSConfiguration(base_url = "http://192.168.1.149:5001/api/v0/"))
    // val f = ipfs.add.file(File("app/src/main/assets/english.txt"), "english.txt")
    // println("Hash: " + f.Hash + " for file name " + f.Name)
    println(ipfs.get.cat("QmcuxneW98zQchBU6rGrntBphe5p82wc4M6JuK2Wa2oCpk"))
    println(ipfs.get.cat("QmWr7iqiych7ZMYjiQDuSwDFrRNuNdWxKVzwSxwbaAmeXV"))

}