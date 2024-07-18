package com.example.tfgwallet.model


/**
 * Class that stores a MasterKeyItem made up of private key, public key, chain code and smart contract
 * hash as strings for visualization. Even though inheritance is not allowed in data classes, this class uses
 * composition to extend the parent class KeyItem.
 * @param keyItem
 * @param scHash
 * @author Ignacio Miguel Rodríguez
 */
data class MasterKeyItem(
    val keyItem: KeyItem, // used composition instead of inheritance because data classes do not allow inheritance
    val scHash: String,
)

