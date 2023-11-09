package com.example.tfgwallet
import android.graphics.Point
import com.example.tfgwallet.Protocols.Companion.bip32
import com.example.tfgwallet.Protocols.Companion.bip39
import com.example.tfgwallet.Protocols.Companion.sha256
import java.io.File
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.math.pow


class Protocols {

    companion object {

        /**
         * This function performs the SHA256 hash function.
         *
         * @param input a String with the original text
         * @return the resulting hashed String
         */
        fun sha256(input: String): String {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            val bytes = messageDigest.digest(input.toByteArray())
            val stringBuilder = StringBuilder()

            for (byte in bytes) {
                stringBuilder.append(String.format("%02x", byte))
            }
            return stringBuilder.toString()
        }

        fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray? {
            // https://itneko.com/kotlin-php-hmac-sha256/
            val hmacSHA256 = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(key, "HmacSHA256")
            hmacSHA256.init(secretKey)
            return hmacSHA256.doFinal(data)
        }

        /**
         * This function performs the BIP39 protocol, which allows
         * for creating deterministic wallets. Based on a password
         * and the desired size for the key, it creates a mnemonic
         * phrase in English and the seed for key creation in other
         * protocols such as BIP32.
         *
         * @param size the dimension of the key in bits (value should be between 128 and 256 bits,
         * if not default size will be 128 bits)
         * @param password used for seed generation
         * @return a Pair with the mnemonics phrase and the seed (Pair<String, ByteArray>)
         */
        fun bip39(size: Int, password: String) : Pair<String, ByteArray> {
            // first check that size is within the boundaries
            // does it have to be a multiple of 2?
            if (size < 128 || size > 256) {
                var size = 128
            }

            // generate random sequence (entropy) between 128 to 256 bits
            val secureRandom = SecureRandom()
            var entropy = BigInteger(size, secureRandom).toString(16)

            // if the first char is 0 size will be smaller than needed, thus add first 0 in String
            if (entropy.length < (size / 4)) entropy = "0$entropy"
            println("Entropy length ${entropy.length} (hex), ${entropy.length * 4} (binary)")
            println(entropy)
            var checksum: String = sha256(entropy)
            println("Checksum length ${checksum.length} (hex), ${checksum.length * 4} (binary)")
            println(checksum)
            /* only keep first size/32 bits and concatenate at the end of the initial entropy
               take into account that this is HEXADECIMAL STRING, thus size is not in bits, but
               rather each digit representing 4 bits */
            println("Char to drop ${checksum.length * 4 - (size / 32)}")
            println(checksum.length)
            var modChecksum: String = checksum.dropLast(checksum.length - (entropy.length/ 32))
            println(modChecksum)
            entropy += modChecksum
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
            var mnemonic: String = ""
            for (group in t_groups) { // for every number get the i-th word
                mnemonic += words[Integer.parseInt(group)] + " "
            }
            println(mnemonic)
            // convert to mnemonic words based on https://github.com/bitcoin/bips/blob/master/bip-0039/english.txt file

            // create seed using PBKDF2
            val salt = mnemonic + password
            val iterations = 2048
            val keyLength = 64 * 8 // 64 bytes, 512 bits

            val keySpec: KeySpec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), iterations, keyLength)
            val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            var key = factory.generateSecret(keySpec).encoded
            val hexKey = key.joinToString("") { "%02x".format(it) }
            println("PBKDF2 generated key: $hexKey")

            return Pair(mnemonic, key)
    }

        /**
         * This function reads all the lines in a file
         * and returns them in a list.
         *
         * @param fileName a String with the name of the file (or path)
         * @return a list with the file lines
         */
        private fun readFile(fileName: String) : List<String> {
            val file = File(fileName)
            return file.readLines()
        }


    fun bip32(seed: String) {
        fun point(p: Point) {

        }

        /**
         * This function converts an integer to the
         * corresponding ByteArray.
         *
         * @param int the "size"-byte integer being serialized
         * @param size the dimension of the ByteArray
         * @return the resulting ByteArray
         */
        fun getBytesFromInt(int: Int, size: Int) : ByteArray {
            return ByteBuffer.allocate(size).putInt(int).array()
        }

        /**
         * This function performs the serialization of
         * a 32-bit integer into a 4-byte array.
         *
         * @param i the 32-bit integer being serialized
         * @return the resulting 4-byte size ByteArray
         */
        fun ser32(i: Int) : ByteArray {
            return getBytesFromInt(i, 4)
        }

        /**
         * This function performs the serialization of
         * a 256-bit integer into a 32-byte array.
         *
         * @param p the 256-bit integer being serialized
         * @return the resulting 32-byte size ByteArray
         */
        fun ser256(p: Int): ByteArray {
            return getBytesFromInt(p, 32)
        }

        fun serP(P: Point) {

        }

        fun parse256(p: ByteArray): Int {
            return ByteBuffer.wrap(p).int
        }

        fun CKDpriv(SKpar: Int, cpar: String, i: Int) : Pair<String, String>? {
            var i = i
            val threshold : Int = 2.0.pow(31).toInt()
            while (i < threshold) {
                i += threshold
            }
            // i >= 2^31, that is, a hardened child
            var data = ByteArray(0x00) + ser256(SKpar) + ser32(i) // not sure about this
            var capitalI = hmacSha256(cpar.toByteArray(), data)
            var left = capitalI?.copyOfRange(0, 31)
            var right = capitalI?.copyOfRange(31, 63)
            return null
        }

        val bytes32 = ser32(384)
        val bytes256 = ser256(123109823)
        print("[")
        for (byte in bytes32) {
            print("$byte, ")
        }
        println("]")

        print("[")
        for (byte in bytes256) {
            print("$byte, ")
        }
        println("]")

        println("Original number is ${parse256(bytes256)}")

        // Start derivaton protocol
        /*
        1. Generate a seed byte sequence S of a chosen length (between 128 and 512
        bits; 256 bits is advised) from a pseudo-random generator. On these work,
        we use the seed we obtain from the BIP-39 protocol, explained in Section
        3.3.1.
        2. Calculate I = HMAC − SHA512(Key = ”Bitcoinseed”, Data = S)
        3. Split I into two 32-byte sequences, IL and IR.
        4. Use parse256(IL) as master secret key, and IR as master chain code (in
        case parse256(IL) is 0 or parse256(IL) ≥ n, the master key is invalid).

         */

    }

    }

}

fun main(args: Array<String>) {
    val inputString = "Hello, this is a test string for hashing."
    val hashedString = sha256(inputString)
    println("SHA256 Hash: $hashedString")
    bip39(128, "whatever")
    bip32("123")
}