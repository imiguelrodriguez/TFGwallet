package com.example.tfgwallet.model
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

class Blockchain {


}
fun main(args: Array<String>) {
    val web3 = Web3j.build(HttpService("http://localhost:7545"))
    val accounts = web3.ethAccounts().send().accounts

    println( accounts.toString())
}
