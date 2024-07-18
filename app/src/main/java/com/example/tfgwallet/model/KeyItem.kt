package com.example.tfgwallet.model

/**
 * Class that stores a KeyItem made up of private key, public key and chain code as strings for
 * visualization. Even though inheritance is not allowed in data classes, this class will act as a
 * father class.
 * @param privKey
 * @param pubKey
 * @param chainCode
 * @author Ignacio Miguel Rodríguez
 */
data class KeyItem(val privKey: String, val pubKey: String, val chainCode: String)

