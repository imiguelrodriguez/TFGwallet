package com.example.tfgwallet
import com.example.tfgwallet.Protocols.Companion.bip39
import com.example.tfgwallet.Protocols.Companion.sha256
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom


class Protocols {

    companion object {
        fun sha256(input: String): String {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            val bytes = messageDigest.digest(input.toByteArray())
            val stringBuilder = StringBuilder()

            for (byte in bytes) {
                stringBuilder.append(String.format("%02x", byte))
            }

            return stringBuilder.toString()

        }


        fun bip39(size: Int, password: String) {

            // generate random sequence (entropy) between 128 to 256 bits
            // first check that size is within the boundaries
            val secureRandom = SecureRandom()
            var entropy = BigInteger(size, secureRandom).toString(16)
            println("Entropy length ${entropy.length}")
            println(entropy)
            var checksum: String = sha256(entropy)
            var modChecksum: String
            println(checksum)
            // only keep first size/32 bits and concatenate at the end of the initial entropy
            println("Char to drop ${checksum.length - (size / 32)}")
            println(checksum.length)
            modChecksum = checksum.dropLast(checksum.length - (size / 32))
            println(modChecksum)
            entropy = entropy + modChecksum
            println(entropy)

            entropy = BigInteger(entropy, 16).toString(2)

            println(entropy)
            println("Entropy length ${entropy.length}")
            // split entropy into 11-bits groups
            var groups = mutableListOf<String>()

            for (i in 0 until entropy.length - 1 step 11) {
                println(i)
                groups.add(entropy.substring(i, i + 11))
            }

            // get decimal value of each group
            val t_groups = groups.map { Integer.parseInt(it, 2).toString() }
            println(t_groups)
            println(System.getProperty("user.dir"))
            // read file and match every code to its word to give the eventual passphrase
            var words = readFile("app/resources/words.txt")
            var passphrase: String = ""
            for (group in t_groups) { // for every number get the i-th word
                passphrase += words[Integer.parseInt(group)] + " "
            }
            println(passphrase)
            // convert to mnemonic words based on https://github.com/bitcoin/bips/blob/master/bip-0039/english.txt file

            // create seed


    }
        private fun readFile(fileName: String) : List<String> {
            val file = File(fileName)
            return file.readLines()
        }

    }

    fun bip32(seed: String) {

    }



}

fun main(args: Array<String>) {
    val inputString = "Hello, this is a test string for hashing."
    val hashedString = sha256(inputString)
    println("SHA256 Hash: $hashedString")
    bip39(128, "whatever")
}