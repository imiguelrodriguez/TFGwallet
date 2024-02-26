package com.example.tfgwallet.model

import java.io.File
import java.math.BigInteger

class Utilities {
    companion object {
    fun UByteArrayToBigInteger(array: UByteArray): BigInteger {
        return array.fold(BigInteger.ZERO) { acc, byte ->
            acc.shl(8).or(BigInteger(byte.toString()))
        }
    }

    fun toUByteArray(signedArray: ByteArray): UByteArray {
        return UByteArray(signedArray.size) { i ->
            (signedArray[i].toInt() and 0xFF).toUByte()
        }
    }

    /**
     * This function reads all the lines in a file
     * and returns them in a list.
     *
     * @param fileName a String with the name of the file (or path)
     * @return a list with the file lines
     */
     fun readFile(fileName: String) : List<String> {
        val file = File(fileName)
        return file.readLines()
    }

}}