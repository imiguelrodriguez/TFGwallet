package com.example.tfgwallet.model

import com.example.tfgwallet.model.Protocols.Companion.sha256
import com.example.tfgwallet.model.Utilities.Companion.UByteArrayToBigInteger
import com.example.tfgwallet.model.Utilities.Companion.readFile
import com.example.tfgwallet.model.Utilities.Companion.toUByteArray
import okhttp3.internal.Util

import org.bitcoinj.core.ECKey
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow
import kotlin.random.Random
import kotlin.random.nextUInt


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

        /**
         * This function performs the HMAC-SHA512 hash function.
         *
         * @param key ByteArray with the secret key
         * @param data ByteArray with the data to be hashed
         * @return the resulting hashed ByteArray
         */
        fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
            val hmacSHA512 = Mac.getInstance("HmacSHA512")
            val secretKey = SecretKeySpec(key, "HmacSHA512")
            hmacSHA512.init(secretKey)
            return hmacSHA512.doFinal(data)
        }


        /**
         * This class represents the instance of the BIP39 protocol,
         * which allows for creating deterministic wallets. Based on a password
         * and the desired size for the key, it creates a mnemonic
         * phrase in English and the seed for key creation in other
         * protocols such as BIP32. After initialization, getSeed() method
         * can be called to get the output of the protocol.
         *
         * @param size the dimension of the key in bits (value should be between 128 and 256 bits,
         * if not default size will be 128 bits)
         * @param password used for seed generation
         */
        class Bip39 {
            private var size: Int = 128
            private var password: String = ""

            constructor(size: Int, password: String) {
                if (size >= 128 || this.size <= 256) { // size is 128 by default
                    this.size=size
                }
                this.password=password
            }

            fun getSeed(): Pair<String, UByteArray>{
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
                   rather each digit represents 4 bits */
                var modChecksum: String = checksum.dropLast(checksum.length - (entropy.length/ 32))
                println(modChecksum)
                entropy += modChecksum
                println(entropy)
                println("Modified entropy length ${entropy.length * 4} (bits)")
                val modEntropyLength = entropy.length * 4
                entropy = BigInteger(entropy, 16).toString(2)
                // same happens here when a 0 is in front, must make sure to add them
                println(entropy)
                println("Entropy length ${entropy.length}")
                if (entropy.length < modEntropyLength) {
                    for (i in 0..modEntropyLength - entropy.length) {
                        entropy = "0$entropy"
                    }
                }
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
                var key = toUByteArray(factory.generateSecret(keySpec).encoded)
                val hexKey = key.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
                println("PBKDF2 generated key: $hexKey")

                return Pair(mnemonic, key)
            }
        }


        class Bip32 {
            val threshold: UInt = Int.MAX_VALUE.toUInt() // (2 ^ 31) - 1
            private var seed = UByteArray(0)
            private var privateKey = BigInteger.ZERO
            private var publicKey = BigInteger.ZERO
            private var chain = BigInteger.ZERO
            constructor(seed: UByteArray) {
                this.seed=seed
                var size = 256
                // MASTER KEY GENERATION
                // possibly include this code into a function since it is repeated
                var valid = false
                var masterKey = BigInteger.ZERO  // initialization
                var masterChain = BigInteger.ZERO
                while(!valid) {
                    var capitalI = toUByteArray(hmacSha512("Bitcoinseed".toByteArray(), seed.toByteArray()))
                    var left = capitalI.copyOfRange(0, 31)
                    var right = capitalI.copyOfRange(31, 63)
                    masterKey = parse256(left)
                    masterChain = parse256(right)
                    if (masterKey != BigInteger.ZERO || masterKey < size.toBigInteger())
                        valid = true
                }
                var output = CKDpriv(size, masterKey, masterChain, Random.nextUInt(0u, (2.0.pow(32).toUInt()) - 1u)) // randomly initialize i
                this.privateKey = output.first
                this.chain = output.second
                this.publicKey = UByteArrayToBigInteger(serP(point(output.first))) // public key
            }
            /**
             * This function performs the multiplication of the integer i with the
             * secp256k1 base point (elliptic curve y^2 = x^3 + 7 (mod p)) and returns
             * the (x,y) resulting coordinate pair.
             *
             * @param p the integer being multiplied
             * @return the resulting (x,y) coordinate (point)
             */
            private fun point(p: BigInteger): org.bouncycastle.math.ec.ECPoint {
                val basePoint : org.bouncycastle.math.ec.ECPoint = ECKey.CURVE.g
                return basePoint.multiply(p)
            }

            /**
             * This function performs the serialization of
             * a 32-bit integer into a 4-unsigned byte array.
             *
             * @param i the 32-bit integer being serialized
             * @return the resulting 4-byte size UByteArray
             */
            private fun ser32(i: UInt) : UByteArray {
                var mask: UInt = 0xFF000000U
                var byteArray = UByteArray(4)
                for (j in 0 until 4) {
                    val byte = ((i and mask) shr ((3 - j) * 8)).toUByte()
                    byteArray[j] = byte
                    mask = mask shr 8
                }
                return byteArray
            }

            /**
             * This function performs the serialization of
             * a 256-bit integer into a 32-byte array.
             *
             * @param p the 256-bit integer being serialized
             * @return the resulting 32-byte size UByteArray
             */
            private fun ser256(p: BigInteger): UByteArray {
                val byteArray: ByteArray = p.toByteArray()
                // if the number does not take the whole 32-byte array, stuff first positions with 0
                if (byteArray.size < 32) {
                    val remainingPos: Int = 32 - byteArray.size
                    val stuffArray = ByteArray(32)
                    System.arraycopy(byteArray, 0, stuffArray, remainingPos, byteArray.size)
                    return toUByteArray(stuffArray)
                }
                return toUByteArray(byteArray)
            }

            /**
             * This function performs the serialization of
             * a (x,y) point into the compressed SEC1 form.
             *
             * @param P the (x,y) ECPoint
             * @return the resulting SEC1 compressed UByteArray
             */
            private fun serP(P: org.bouncycastle.math.ec.ECPoint): UByteArray {
                val xCoordBytes = ser256(P.xCoord.toBigInteger())
                val headerByte: UByte = if (P.yCoord.toBigInteger() % BigInteger("2") == BigInteger.ZERO) 0x02.toUByte() else 0x03.toUByte()
                return ubyteArrayOf(headerByte) + xCoordBytes
            }

            /**
             * This function converts a 32-byte array to a BigInteger
             *
             * @param p the 32-unsigned byte array
             * @return the resulting BigInteger
             */
            private fun parse256(p: UByteArray): BigInteger {
                return UByteArrayToBigInteger(p)
            }

            private fun CKDpriv(size: Int, SKpar: BigInteger, cpar: BigInteger, i: UInt) : Pair<BigInteger, BigInteger> {
                var i: UInt = i
                while (i < threshold) {
                    i += threshold
                }
                var SKi: BigInteger = BigInteger.ZERO
                var chain: BigInteger = BigInteger.ZERO
                var validKey = false
                while(!validKey) {
                    var ser32 = ser32(i)
                    // i >= 2^31, that is, a hardened child
                    var data: UByteArray =
                        ubyteArrayOf(0x00u) + ser256(SKpar) + ser32 // not sure about this
                    var capitalI: UByteArray = toUByteArray(hmacSha512(cpar.toByteArray(), data.toByteArray()))
                    var left = capitalI.copyOfRange(0, 31)
                    var right = capitalI.copyOfRange(31, 63) // chain code
                    chain = UByteArrayToBigInteger(right)
                    SKi =
                        parse256(left) + SKpar % size.toBigInteger() // mod(n), being n key length
                    validKey = parse256(left) < size.toBigInteger()
                    validKey = validKey || SKi != BigInteger.ZERO
                }

                return Pair(SKi, chain)
            }


        }

    }

}

fun main(args: Array<String>) {
    val inputString = "Hello, this is a test string for hashing."
    val hashedString = sha256(inputString)
    println("SHA256 Hash: $hashedString")
    var bip39 = Protocols.Companion.Bip39(256, "password")
    var seed = bip39.getSeed()
    var bip32 = Protocols.Companion.Bip32(seed.second)


}