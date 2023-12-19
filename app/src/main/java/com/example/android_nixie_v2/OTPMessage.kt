package com.example.android_nixie_v2

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

data class OTPMessage(val op : ObjectTransferProfile.OP, val status : Int, val characteristic: BluetoothGattCharacteristic?, val value : ByteArray?)
