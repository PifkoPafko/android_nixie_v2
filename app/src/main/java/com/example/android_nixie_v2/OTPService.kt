package com.example.android_nixie_v2

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService

object OTPService {
    var otpService : BluetoothGattService? = null

    var otsFeatureChar : BluetoothGattCharacteristic? = null
    var otsNameChar : BluetoothGattCharacteristic? = null
    var otsTypeChar : BluetoothGattCharacteristic? = null
    var otsSizeChar : BluetoothGattCharacteristic? = null
    var otsIdChar : BluetoothGattCharacteristic? = null
    var otsPropertiesChar : BluetoothGattCharacteristic? = null
    var otsOACPChar : BluetoothGattCharacteristic? = null
    var otsOLCPChar : BluetoothGattCharacteristic? = null
    var otsListFilterChar : BluetoothGattCharacteristic? = null
    var otsAlarmAction : BluetoothGattCharacteristic? = null
    var otsWifiAction : BluetoothGattCharacteristic? = null

    var otsOACPdesc : BluetoothGattDescriptor? = null
    var otsOLCPdesc : BluetoothGattDescriptor? = null
    var otsWifidesc : BluetoothGattDescriptor? = null
}