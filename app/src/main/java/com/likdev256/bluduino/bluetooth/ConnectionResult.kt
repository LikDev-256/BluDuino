package com.likdev256.bluduino.bluetooth

sealed interface ConnectionResult {
    object ConnectionEstablished: ConnectionResult
    data class TransferSucceeded(val message: String): ConnectionResult
    data class Error(val message: String): ConnectionResult
}