package com.likdev256.bluduino.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi

class BluetoothReceiver(private val onDeviceFound: (BluetoothDevice) -> Unit) : BroadcastReceiver() {
    private val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun register(context: Context) {
        context.registerReceiver(this, intentFilter, Context.RECEIVER_NOT_EXPORTED)
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_FOUND) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            device?.let { onDeviceFound(it) }
        }
    }
}
