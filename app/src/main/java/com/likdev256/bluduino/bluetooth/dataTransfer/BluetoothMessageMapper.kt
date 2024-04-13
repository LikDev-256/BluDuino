package com.likdev256.bluduino.bluetooth.dataTransfer

fun String.toByteArray(): ByteArray {
    return "#$this".encodeToByteArray()
}