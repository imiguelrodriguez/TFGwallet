package com.example.tfgwallet.model

import com.google.gson.annotations.SerializedName

class Data {
    data class SepoliaAccountResponse(
        @SerializedName("status") val code: Int,
        @SerializedName("message") val message: String,
        @SerializedName("result") val result:  List<TransactionData>
    )

    data class TransactionData(
        @SerializedName("blockNumber") val blockNumber: String,
        @SerializedName("timeStamp") val timeStamp: String,
        @SerializedName("hash") val hash: String,
        @SerializedName("nonce") val nonce: String,
        @SerializedName("blockHash") val blockHash: String,
        @SerializedName("transactionIndex") val transactionIndex: String,
        @SerializedName("from") val from: String,
        @SerializedName("to") val to: String,
        @SerializedName("value") val value: String,
        @SerializedName("gas") val gas: String,
        @SerializedName("gasPrice") val gasPrice: String,
        @SerializedName("isError") val isError: String,
        @SerializedName("txreceipt_status") val txReceiptStatus: String,
        @SerializedName("input") val input: String,
        @SerializedName("contractAddress") val contractAddress: String,
        @SerializedName("cumulativeGasUsed") val cumulativeGasUsed: String,
        @SerializedName("gasUsed") val gasUsed: String,
        @SerializedName("confirmations") val confirmations: String
    )

    data class GasPriceResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("data") val data: GasData
    )

    data class GasData(
        @SerializedName("rapid") val rapid: Long,
        @SerializedName("fast") val fast: Long,
        @SerializedName("standard") val standard: Long,
        @SerializedName("slow") val slow: Long,
        @SerializedName("timestamp") val timestamp: Long,
        @SerializedName("price") val price: Int,
        @SerializedName("priceUSD") val priceUSD: Int
    )
}