package com.example.tfgwallet.model

import android.content.Context
import android.content.res.AssetManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.URL
import java.util.LinkedList

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
        fun readFile(context: Context, fileName: String): List<String> {
            val wordsList = mutableListOf<String>()

            try {
                val assetManager = context.assets
                val inputStream = assetManager.open(fileName)
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))

                var line: String
                while (bufferedReader.readLine().also { line = it } != null) {
                    wordsList.add(line)
                }

                bufferedReader.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return wordsList

        }

    }
}