package com.example.tfgwallet.model
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.io.BufferedReader
import java.io.InputStreamReader
class Blockchain {


}
fun main(args: Array<String>) {


    val command = "ganache-cli"

    // Execute the command
    val process = ProcessBuilder()
        .command("cmd", "/c", command)
        .start()

    // Read the output of the command
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        println(line)
    }

    // Wait for the process to complete
    val exitCode = process.waitFor()
    println("Command exited with code $exitCode")
    val web3 = Web3j.build(HttpService("http://localhost:8545"))
    val accounts = web3.ethAccounts().send().accounts
    println( accounts.toString())
}
