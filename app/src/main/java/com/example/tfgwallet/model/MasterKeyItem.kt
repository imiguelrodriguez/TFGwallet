package com.example.tfgwallet.model

import org.web3j.crypto.Bip32ECKeyPair

data class MasterKeyItem(val privKey: String, val pubKey: String, val chainCode: String, val scHash: String)

