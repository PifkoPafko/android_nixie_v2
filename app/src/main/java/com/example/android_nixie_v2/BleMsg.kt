package com.example.android_nixie_v2

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

data class BleMsg(val op : BleOP, val status : Int = 0, val characteristic: BluetoothGattCharacteristic? = null, val descriptor: BluetoothGattDescriptor? = null, val value : ByteArray? = null)
