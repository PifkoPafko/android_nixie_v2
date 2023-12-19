package com.example.android_nixie_v2

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.util.ArrayList
import java.util.UUID

enum class BleOP {
    INIT, CONNECTED, DISCONNECTED, MTU_CHANGED, SERVICE_DISCOVERED, CHAR_CHANGED, CHAR_WRITE, CHAR_READ
}

object BleManager {
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var appContext: Context? = null
    private lateinit var nixieDevice: BluetoothDevice
    private lateinit var bluetoothGatt: BluetoothGatt
    private var menuHandler : Handler? = null

    var isNixieConnected = false
    var isNixieWifiConnected = false
    var myWifiSSID : String = ""

    private fun enableBluetoothPrompt(context: Context) {
        if (!bluetoothAdapter.isEnabled) {
            val toast = Toast.makeText(context, "Bluetooth jest wyłączony.", Toast.LENGTH_LONG)
            toast.show()
        }
    }

     @SuppressLint("MissingPermission")
     fun bleManagerInit(context: Context) : Boolean {
         bluetoothManager = context.getSystemService(BluetoothManager::class.java)
         bluetoothAdapter = bluetoothManager.adapter
         appContext = context
         enableBluetoothPrompt(context)

         var nixieDeviceFound = false
         val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
         pairedDevices?.forEach { device ->
             if (device.name == BluetoothConst.nixieDeviceName) {
                 nixieDevice = device
                 nixieDeviceFound = true
             }
         }

         if (!nixieDeviceFound) {
             return false
         }

         BluetoothSM.start()
         bluetoothGatt = nixieDevice.connectGatt(appContext, true, bluetoothGattCallback )

         return true
    }

    fun setMenuHandler (handler: Handler?){
        menuHandler = handler
    }

    fun writeAlarmToRemote (alarm : Alarm, handler: Handler) : Boolean {
        if (BluetoothSM.state != BluetoothState.ALL_SET) {
            return false
        }

        return WriteAlarmSM.writeAlarm(alarm, handler)
    }

    fun enableAlarmToRemote (alarm : Alarm, handler: Handler) : Boolean {
        if (BluetoothSM.state != BluetoothState.ALL_SET) {
            return false
        }

        return enableAlarmSM.writeAlarm(alarm, handler)
    }

    fun deleteAlarmToRemote (list : ArrayList<Int>, handler: Handler) : Boolean {
        if (BluetoothSM.state != BluetoothState.ALL_SET) {
            return false
        }

        return deleteAlarmsSM.writeAlarm(list, handler)
    }

    fun startWifiSearch (handler : Handler) : Boolean {
        if (BluetoothSM.state != BluetoothState.ALL_SET) {
            return false
        }

        return ReadWifiNetSM.startSearch(handler)
    }

    fun startWifiConnect (handler : Handler, wifiIndex : Int, password : String) : Boolean {
        if (BluetoothSM.state != BluetoothState.ALL_SET) {
            return false
        }

        return ConnectWifiNetSM.startConnect(handler, wifiIndex, password)
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isNixieConnected = true
                BluetoothSM.insertAction(BleMsg(BleOP.CONNECTED))

                if (menuHandler != null)
                {
                    val btMsg = Message()
                    btMsg.arg1 = ProjectDefs.CONNECTED
                    menuHandler?.sendMessage(btMsg)
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isNixieConnected = false
                BluetoothSM.state = BluetoothState.WAIT_FOR_CONNECTION

                if (menuHandler != null)
                {
                    val btMsg = Message()
                    btMsg.arg1 = ProjectDefs.DISCONNECTED
                    menuHandler?.sendMessage(btMsg)
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)

            if(mtu == 512 && status == 0) {
                BluetoothSM.insertAction(BleMsg(BleOP.MTU_CHANGED))
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            BluetoothSM.insertAction(BleMsg(BleOP.SERVICE_DISCOVERED))
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {

            BluetoothSM.insertAction(BleMsg(BleOP.CHAR_CHANGED, 0, characteristic, null, value))
            WriteAlarmSM.insertAction(OTPMessage(ObjectTransferProfile.OP.CHANGE, 0, characteristic, value))
            enableAlarmSM.insertAction(OTPMessage(ObjectTransferProfile.OP.CHANGE, 0, characteristic, value))
            deleteAlarmsSM.insertAction(OTPMessage(ObjectTransferProfile.OP.CHANGE, 0, characteristic, value))
            ReadWifiNetSM.insertAction(OTPMessage(ObjectTransferProfile.OP.CHANGE, 0, characteristic, value))
            ConnectWifiNetSM.insertAction(OTPMessage(ObjectTransferProfile.OP.CHANGE, 0, characteristic, value))
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {

            BluetoothSM.insertAction(BleMsg(BleOP.CHAR_READ, status, characteristic, null, value))
            WriteAlarmSM.insertAction(OTPMessage(ObjectTransferProfile.OP.READ, status, characteristic, value))
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {

            BluetoothSM.insertAction(BleMsg(BleOP.CHAR_WRITE, status, characteristic))
            WriteAlarmSM.insertAction(OTPMessage(ObjectTransferProfile.OP.WRITE, status, characteristic, null))
            enableAlarmSM.insertAction(OTPMessage(ObjectTransferProfile.OP.WRITE, status, characteristic, null))
            deleteAlarmsSM.insertAction(OTPMessage(ObjectTransferProfile.OP.WRITE, status, characteristic, null))
            ReadWifiNetSM.insertAction(OTPMessage(ObjectTransferProfile.OP.WRITE, status, characteristic, null))
            ConnectWifiNetSM.insertAction(OTPMessage(ObjectTransferProfile.OP.WRITE, status, characteristic, null))
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {

            BluetoothSM.insertAction(BleMsg(BleOP.CHAR_WRITE, status, null, descriptor, null))
        }
    }

    object BluetoothSM {
        var state = BluetoothState.IDLE
        var tempAlarm : Alarm? = null

        fun start () {
            state = BluetoothState.WAIT_FOR_CONNECTION
            insertAction(BleMsg(BleOP.INIT))
        }

        @SuppressLint("MissingPermission")
        fun insertAction(msg : BleMsg) {
            when (state) {
                BluetoothState.IDLE -> {
                    return
                }

                BluetoothState.WAIT_FOR_CONNECTION -> {
                    if (msg.op == BleOP.CONNECTED) {
                        state = BluetoothState.CONNECTED
                        bluetoothGatt.requestMtu(512)
                    }
                }

                BluetoothState.CONNECTED -> {
                    if (msg.op == BleOP.MTU_CHANGED) {
                        state = BluetoothState.WAIT_FOR_MTU_CHANGE
                        bluetoothGatt.discoverServices()
                    }
                }

                BluetoothState.WAIT_FOR_MTU_CHANGE -> {
                    if (msg.op == BleOP.SERVICE_DISCOVERED) {
                        OTPService.otpService = bluetoothGatt.getService(UUID.fromString(ObjectTransferProfile.OTP_SERVICE_UUID))
                        OTPService.otsFeatureChar = OTPService.otpService!!.getCharacteristic(UUID.fromString(ObjectTransferProfile.OTS_CHAR_FEATURE_UUID))
                        OTPService.otsNameChar = OTPService.otpService!!.getCharacteristic(UUID.fromString(ObjectTransferProfile.OTS_CHAR_NAME_UUID))
                        OTPService.otsTypeChar = OTPService.otpService!!.getCharacteristic(UUID.fromString(ObjectTransferProfile.OTS_CHAR_TYPE_UUID))
                        OTPService.otsSizeChar = OTPService.otpService!!.getCharacteristic(UUID.fromString(ObjectTransferProfile.OTS_CHAR_SIZE_UUID))
                        OTPService.otsIdChar = OTPService.otpService!!.getCharacteristic(UUID.fromString(ObjectTransferProfile.OTS_CHAR_ID_UUID))
                        OTPService.otsPropertiesChar = OTPService.otpService!!.getCharacteristic(UUID.fromString(ObjectTransferProfile.OTS_CHAR_PROPERTIES_UUID))
                        OTPService.otsOACPChar = OTPService.otpService!!.getCharacteristic(UUID.fromString(ObjectTransferProfile.OTS_CHAR_OACP_UUID))
                        OTPService.otsOLCPChar = OTPService.otpService!!.getCharacteristic(UUID.fromString(ObjectTransferProfile.OTS_CHAR_OLCP_UUID))
                        OTPService.otsListFilterChar = OTPService.otpService!!.getCharacteristic(UUID.fromString(ObjectTransferProfile.OTS_CHAR_LIST_FILTER_UUID))
                        OTPService.otsAlarmAction = OTPService.otpService!!.getCharacteristic(UUID.fromString(ObjectTransferProfile.OTS_CHAR_ALARM_ACTION_UUID))
                        OTPService.otsWifiAction = OTPService.otpService!!.getCharacteristic(UUID.fromString(ObjectTransferProfile.OTS_CHAR_WIFI_ACTION_UUID))

                        OTPService.otsOACPdesc = OTPService.otsOACPChar!!.getDescriptor(UUID.fromString(ObjectTransferProfile.OTS_DESC_OACP_UUID))
                        OTPService.otsOLCPdesc = OTPService.otsOLCPChar!!.getDescriptor(UUID.fromString(ObjectTransferProfile.OTS_DESC_OLCP_UUID))
                        OTPService.otsWifidesc = OTPService.otsWifiAction!!.getDescriptor(UUID.fromString(ObjectTransferProfile.OTS_DESC_WIFI_UUID))

                        bluetoothGatt.readCharacteristic(OTPService.otsWifiAction!!)

                        state = BluetoothState.WAIT_FOR_WIFI_READ
                    }
                }

                BluetoothState.WAIT_FOR_WIFI_READ -> {
                    if (msg.op == BleOP.CHAR_READ && msg.characteristic == OTPService.otsWifiAction && msg.status == 0) {
                        if (msg.value != null && msg.value.isNotEmpty()) {
                            isNixieWifiConnected = msg.value[0].toInt() == 1
                            if ( msg.value.size >= 2 && msg.value.size == msg.value[1] + 2) {
                                myWifiSSID = String(msg.value.copyOfRange(2, 2 + msg.value[1]), Charsets.UTF_8)
                            }
                        }

                        state = BluetoothState.WAIT_FOR_OACP_DESC_WRITE
                        bluetoothGatt.setCharacteristicNotification(OTPService.otsOACPChar, true)
                        bluetoothGatt.writeDescriptor(OTPService.otsOACPdesc!!, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                    }
                }

                BluetoothState.WAIT_FOR_OACP_DESC_WRITE -> {
                    if (msg.op == BleOP.CHAR_WRITE && msg.descriptor == OTPService.otsOACPdesc && msg.status == 0) {
                        state = BluetoothState.WAIT_FOR_OLCP_DESC_WRITE
                        bluetoothGatt.setCharacteristicNotification(OTPService.otsOLCPChar, true)
                        bluetoothGatt.writeDescriptor(OTPService.otsOLCPdesc!!, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                    }
                }

                BluetoothState.WAIT_FOR_OLCP_DESC_WRITE -> {
                    if (msg.op == BleOP.CHAR_WRITE && msg.descriptor == OTPService.otsOLCPdesc && msg.status == 0) {
                        state = BluetoothState.WAIT_FOR_WIFI_DESC_WRITE
                        bluetoothGatt.setCharacteristicNotification(OTPService.otsWifiAction, true)
                        bluetoothGatt.writeDescriptor(OTPService.otsWifidesc!!, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                    }
                }

                BluetoothState.WAIT_FOR_WIFI_DESC_WRITE -> {
                    if (msg.op == BleOP.CHAR_WRITE && msg.descriptor == OTPService.otsWifidesc && msg.status == 0) {
                        state = BluetoothState.WAIT_FOR_FILTER_SET
                        bluetoothGatt.writeCharacteristic(OTPService.otsListFilterChar!!, AlarmCharacteristic.prepareListFilterByAlarmTypePayload() , BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }

                BluetoothState.WAIT_FOR_FILTER_SET -> {
                    if (msg.op == BleOP.CHAR_WRITE && msg.characteristic == OTPService.otsListFilterChar && msg.status == 0) {
                        state = BluetoothState.WAIT_FOR_FIRST
                        bluetoothGatt.writeCharacteristic(OTPService.otsOLCPChar!!, byteArrayOf(ObjectTransferProfile.OlCP_FIRST) , BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }

                BluetoothState.WAIT_FOR_FIRST -> {
                    if ((msg.op == BleOP.CHAR_CHANGED) && (msg.characteristic == OTPService.otsOLCPChar)) {
                        if ( ObjectTransferProfile.getOLCPIndStatus(msg.value) == ObjectTransferProfile.OLCP_RET_SUCCESS ) {
                            state = BluetoothState.WAIT_FOR_READ_ALARM
                            bluetoothGatt.readCharacteristic(OTPService.otsAlarmAction!!)
                        } else {
                            state = BluetoothState.ALL_SET
                            if (menuHandler != null)
                            {
                                val btMsg = Message()
                                btMsg.arg1 = ProjectDefs.NOTIFY_ALL_SET
                                menuHandler?.sendMessage(btMsg)
                            }
                        }
                    }
                }

                BluetoothState.WAIT_FOR_READ_ALARM -> {
                    if (msg.op == BleOP.CHAR_READ && msg.characteristic == OTPService.otsAlarmAction) {
                        if ( msg.status == 0 ) {
                            tempAlarm = AlarmCharacteristic.addAlarmFromPayload(msg.value!!)
                            if (tempAlarm != null) {
                                state = BluetoothState.WAIT_FOR_READ_ID
                                bluetoothGatt.readCharacteristic(OTPService.otsIdChar!!)
                            } else {
                                bluetoothGatt.readCharacteristic(OTPService.otsAlarmAction!!)
                            }
                        } else {
                            state = BluetoothState.WAIT_FOR_NEXT
                            bluetoothGatt.writeCharacteristic(OTPService.otsOLCPChar!!, byteArrayOf(ObjectTransferProfile.OlCP_NEXT) , BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        }
                    }
                }

                BluetoothState.WAIT_FOR_READ_ID -> {
                    if ((msg.op == BleOP.CHAR_READ) && (msg.characteristic == OTPService.otsIdChar) && (msg.status == ObjectTransferProfile.CHAR_RSP_SUCCESS) && (msg.value?.size == 6)) {
                        tempAlarm!!.id = ObjectTransferProfile.byteArrayIdToInt(msg.value)
                        AlarmManager.addAlarm(tempAlarm!!)
                        state = BluetoothState.WAIT_FOR_NEXT
                        bluetoothGatt.writeCharacteristic(OTPService.otsOLCPChar!!, byteArrayOf(ObjectTransferProfile.OlCP_NEXT) , BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }

                BluetoothState.WAIT_FOR_NEXT -> {
                    if ((msg.op == BleOP.CHAR_CHANGED) && (msg.characteristic == OTPService.otsOLCPChar)) {
                        if (ObjectTransferProfile.getOLCPIndStatus(msg.value) == ObjectTransferProfile.OLCP_RET_SUCCESS) {
                            state = BluetoothState.WAIT_FOR_READ_ALARM
                            bluetoothGatt.readCharacteristic(OTPService.otsAlarmAction!!)
                        } else {
                            state = BluetoothState.ALL_SET
                            if (menuHandler != null)
                            {
                                val btMsg = Message()
                                btMsg.arg1 = ProjectDefs.NOTIFY_ALL_SET
                                menuHandler?.sendMessage(btMsg)
                            }
                        }
                    }
                }

                BluetoothState.ALL_SET -> {
                    if ((msg.op == BleOP.CHAR_CHANGED) && (msg.characteristic == OTPService.otsWifiAction) && msg.status == 0) {
                        if (msg.value != null && msg.value.size == 1 && msg.value[0].toInt() == 0 ) {
                            val wifiMsg = Message()
                            wifiMsg.arg1 = 2
                            menuHandler?.sendMessage(wifiMsg)

                        } else {
                            val wifiMsg = Message()
                            wifiMsg.arg1 = 2
                            menuHandler?.sendMessage(wifiMsg)
                        }
                    }
                }
            }
        }
    }

    object WriteAlarmSM {

        enum class AlarmWriteThread {
            IDLE, CREATE_NEW_OBJECT, WAIT_FOR_CREATE_IND, GOTO_OBJECT, WAIT_FOR_GOTO_IND, WAIT_FOR_SET_ALARM_SUCCESS, WAIT_FOR_READ_ID
        }

        private var state = AlarmWriteThread.IDLE
        private var alarm: Alarm? = null
        private var attempts = 0

        private lateinit var handler : Handler

        fun writeAlarm(newAlarm: Alarm, newHandler: Handler) : Boolean {
            if ( state != AlarmWriteThread.IDLE) {
                return false
            }

            alarm = newAlarm
            attempts = 3

            state = if ( AlarmManager.isAlarmNew ) {
                AlarmWriteThread.CREATE_NEW_OBJECT
            } else {
                AlarmWriteThread.GOTO_OBJECT
            }

            handler = newHandler

            insertAction(null)
            return true
        }

        @SuppressLint("MissingPermission")
        fun insertAction(msg : OTPMessage?) {
            when (state) {
                AlarmWriteThread.IDLE -> {
                    return
                }

                AlarmWriteThread.CREATE_NEW_OBJECT -> {
                    bluetoothGatt.writeCharacteristic(OTPService.otsOACPChar!!, AlarmCharacteristic.prepareCreateAlarmObjectPayload(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    state = AlarmWriteThread.WAIT_FOR_CREATE_IND
                }

                AlarmWriteThread.WAIT_FOR_CREATE_IND -> {
                    if ((msg?.op == ObjectTransferProfile.OP.CHANGE) && (msg.characteristic == OTPService.otsOACPChar) && (ObjectTransferProfile.checkOACPInd(msg.value)) ){
                        state = AlarmWriteThread.WAIT_FOR_SET_ALARM_SUCCESS
                        bluetoothGatt.writeCharacteristic(OTPService.otsAlarmAction!!, AlarmCharacteristic.prepareAlarmPayload(alarm!!), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }

                AlarmWriteThread.GOTO_OBJECT -> {
                    bluetoothGatt.writeCharacteristic(OTPService.otsOLCPChar!!, AlarmCharacteristic.prepareGoToPayload(alarm!!.id), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    state = AlarmWriteThread.WAIT_FOR_GOTO_IND
                }

                AlarmWriteThread.WAIT_FOR_GOTO_IND -> {
                    if ((msg?.op == ObjectTransferProfile.OP.CHANGE) && (msg.characteristic == OTPService.otsOLCPChar) && (ObjectTransferProfile.checkOLCPInd(msg.value)) ){
                        state = AlarmWriteThread.WAIT_FOR_SET_ALARM_SUCCESS
                        bluetoothGatt.writeCharacteristic(OTPService.otsAlarmAction!!, AlarmCharacteristic.prepareAlarmPayload(alarm!!), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }

                AlarmWriteThread.WAIT_FOR_SET_ALARM_SUCCESS -> {
                    if ((msg?.op == ObjectTransferProfile.OP.WRITE) && (msg.characteristic == OTPService.otsAlarmAction) && (msg.status == ObjectTransferProfile.CHAR_RSP_SUCCESS)) {

                        if ( AlarmManager.isAlarmNew ) {
                            state = AlarmWriteThread.WAIT_FOR_READ_ID
                            bluetoothGatt.readCharacteristic(OTPService.otsIdChar!!)
                        } else {
                            state = AlarmWriteThread.IDLE
                            val alarmMsg = Message()
                            alarmMsg.arg1 = ProjectDefs.MODIFY_ALARM_SUCCESS
                            handler.sendMessage(alarmMsg)
                        }
                    } else {
                        bluetoothGatt.writeCharacteristic(OTPService.otsAlarmAction!!, AlarmCharacteristic.prepareAlarmPayload(alarm!!), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }

                AlarmWriteThread.WAIT_FOR_READ_ID -> {
                    if ((msg?.op == ObjectTransferProfile.OP.READ) && (msg.characteristic == OTPService.otsIdChar) && (msg.status == ObjectTransferProfile.CHAR_RSP_SUCCESS) && (msg.value?.size == 6)) {
                        val alarmMsg = Message()
                        alarmMsg.arg1 = ProjectDefs.CREATE_ALARM_SUCCESS
                        alarmMsg.arg2 = ObjectTransferProfile.byteArrayIdToInt(msg.value)
                        handler.sendMessage(alarmMsg)
                        state = AlarmWriteThread.IDLE
                    }
                }
            }
        }
    }

    object enableAlarmSM {

        enum class enableAlarmState {
            IDLE, GOTO_ID_ALARM, WAIT_FOR_GOTO_IND, WAIT_FOR_SET_ALARM_SUCCESS
        }

        private var state = enableAlarmState.IDLE
        private var alarm: Alarm? = null

        private lateinit var handler : Handler

        fun writeAlarm(newAlarm: Alarm, newHandler: Handler) : Boolean {
            if ( state != enableAlarmState.IDLE) {
                return false
            }

            alarm = newAlarm
            state = enableAlarmState.GOTO_ID_ALARM
            handler = newHandler

            insertAction(null)
            return true
        }

        @SuppressLint("MissingPermission")
        fun insertAction(msg : OTPMessage?) {
            when (state) {
                enableAlarmState.IDLE -> {
                    return
                }

                enableAlarmState.GOTO_ID_ALARM -> {
                    bluetoothGatt.writeCharacteristic(OTPService.otsOLCPChar!!, AlarmCharacteristic.prepareGoToPayload(alarm!!.id), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    state = enableAlarmState.WAIT_FOR_GOTO_IND
                }

                enableAlarmState.WAIT_FOR_GOTO_IND -> {
                    if ((msg?.op == ObjectTransferProfile.OP.CHANGE) && (msg.characteristic == OTPService.otsOLCPChar) && (ObjectTransferProfile.checkOLCPInd(msg.value)) ){
                        state = enableAlarmState.WAIT_FOR_SET_ALARM_SUCCESS
                        bluetoothGatt.writeCharacteristic(OTPService.otsAlarmAction!!, AlarmCharacteristic.prepareAlarmPayload(alarm!!), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }

                enableAlarmState.WAIT_FOR_SET_ALARM_SUCCESS -> {
                    if ((msg?.op == ObjectTransferProfile.OP.WRITE) && (msg.characteristic == OTPService.otsAlarmAction) && (msg.status == ObjectTransferProfile.CHAR_RSP_SUCCESS)) {
                        state = enableAlarmState.IDLE
                        val alarmMsg = Message()
                        alarmMsg.arg1 = ProjectDefs.ENABLE_ALARM_ACTION_END
                        handler.sendMessage(alarmMsg)
                    } else {
                        bluetoothGatt.writeCharacteristic(OTPService.otsAlarmAction!!, AlarmCharacteristic.prepareAlarmPayload(alarm!!), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }
            }
        }
    }

    object deleteAlarmsSM {

        enum class deleteAlarmsState {
            IDLE, GOTO_ID_ALARM, WAIT_FOR_GOTO_IND, WAIT_FOR_DEL_ALARM_IND
        }

        private var state = deleteAlarmsState.IDLE
        private lateinit var list: ArrayList<Int>
        private var currentIndex = 0
        private lateinit var handler : Handler

        fun writeAlarm(newList: ArrayList<Int>, newHandler: Handler) : Boolean {
            if ( state != deleteAlarmsState.IDLE) {
                return false
            }

            list = newList
            state = deleteAlarmsState.GOTO_ID_ALARM
            handler = newHandler
            currentIndex = 0

            insertAction(null)
            return true
        }

        @SuppressLint("MissingPermission")
        fun insertAction(msg : OTPMessage?) {
            when (state) {
                deleteAlarmsState.IDLE -> {
                    return
                }

                deleteAlarmsState.GOTO_ID_ALARM -> {
                    bluetoothGatt.writeCharacteristic(OTPService.otsOLCPChar!!, AlarmCharacteristic.prepareGoToPayload(list[currentIndex]), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    state = deleteAlarmsState.WAIT_FOR_GOTO_IND
                }

                deleteAlarmsState.WAIT_FOR_GOTO_IND -> {
                    if ((msg?.characteristic == OTPService.otsOLCPChar) && (((msg?.op == ObjectTransferProfile.OP.CHANGE) && (ObjectTransferProfile.checkOLCPInd(msg.value))) || ((msg?.op == ObjectTransferProfile.OP.WRITE) && (msg.status == 0x00)))) {


                        state = deleteAlarmsState.WAIT_FOR_DEL_ALARM_IND
                        bluetoothGatt.writeCharacteristic(OTPService.otsOACPChar!!, AlarmCharacteristic.prepareDeleteAlarmObjectPayload(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    } else {
                        bluetoothGatt.writeCharacteristic(OTPService.otsOLCPChar!!, AlarmCharacteristic.prepareGoToPayload(list[currentIndex]), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }

                deleteAlarmsState.WAIT_FOR_DEL_ALARM_IND -> {
                    if ((msg?.characteristic == OTPService.otsOACPChar) && (((msg?.op == ObjectTransferProfile.OP.CHANGE) && (ObjectTransferProfile.checkOACPInd(msg.value))) || ((msg?.op == ObjectTransferProfile.OP.WRITE) && (msg.status == 0x00)))) {
                        if (currentIndex == list.lastIndex) {
                            state = deleteAlarmsState.IDLE

                            val alarmMsg = Message()
                            alarmMsg.arg1 = ProjectDefs.DELETE_ALARMS_ACTION_END
                            handler.sendMessage(alarmMsg)
                        } else {
                            currentIndex++
                            state = deleteAlarmsState.WAIT_FOR_GOTO_IND
                            bluetoothGatt.writeCharacteristic(OTPService.otsOLCPChar!!, AlarmCharacteristic.prepareGoToPayload(list[currentIndex]), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        }
                    } else {
                        bluetoothGatt.writeCharacteristic(OTPService.otsOACPChar!!, AlarmCharacteristic.prepareDeleteAlarmObjectPayload(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    }
                }
            }
        }
    }

    object ReadWifiNetSM {

        enum class readWifiState {
            IDLE, WRITE_START_CMD, WAIT_FOR_WRITE, WAIT_FOR_IND
        }

        private var state = readWifiState.IDLE
        private lateinit var handler : Handler

        fun startSearch(newHandler : Handler) : Boolean {
            if ( state != readWifiState.IDLE) {
                return false
            }

            handler = newHandler
            state = readWifiState.WRITE_START_CMD

            insertAction(null)
            return true
        }

        @SuppressLint("MissingPermission")
        fun insertAction(msg : OTPMessage?) {
            when (state) {

                readWifiState.IDLE -> {
                    return
                }

                readWifiState.WRITE_START_CMD -> {
                    bluetoothGatt.writeCharacteristic(OTPService.otsWifiAction!!, byteArrayOf(ObjectTransferProfile.WIFI_ACTION_SEARCH), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    state = readWifiState.WAIT_FOR_WRITE
                }

                readWifiState.WAIT_FOR_WRITE -> {
                    if ((msg?.op == ObjectTransferProfile.OP.WRITE) && (msg.characteristic == OTPService.otsWifiAction) && msg.status == 0) {
                        state = readWifiState.WAIT_FOR_IND
                    }
                }

                readWifiState.WAIT_FOR_IND -> {
                    if ((msg?.op == ObjectTransferProfile.OP.CHANGE) && (msg.characteristic == OTPService.otsWifiAction) && msg.status == 0) {
                        if (msg.value != null && msg.value.size > 1 && msg.value[0].toInt() == 1 ) {
                            var offset = 1
                            val descLen = msg.value[offset]
                            offset++

                            if (msg.value.size < descLen + 4) {
                                return
                            }

                            val description = String(msg.value.copyOfRange(offset, offset + descLen), Charsets.UTF_8)
                            offset += descLen

                            val rssi = msg.value[offset].toInt()
                            offset++

                            val authMode = authModeOrdinaltoEnum(msg.value[offset].toInt())

                            val newWifi = Wifi(description, rssi, authMode)
                            WifiSet.wifiList.add(newWifi)

                            val wifiMsg = Message()
                            wifiMsg.arg1 = 1
                            handler.sendMessage(wifiMsg)

                        } else if (msg.value != null && msg.value.size == 1 && msg.value[0].toInt() == 0 ) {
                            val wifiMsg = Message()
                            wifiMsg.arg1 = 2
                            handler.sendMessage(wifiMsg)

                            state = readWifiState.IDLE

                        } else {
                            val wifiMsg = Message()
                            wifiMsg.arg1 = 2
                            handler.sendMessage(wifiMsg)

                            state = readWifiState.IDLE
                        }
                    }
                }
            }
        }
    }

    object ConnectWifiNetSM {

        enum class ConnectWifiState {
            IDLE, WRITE_START_CMD, WAIT_FOR_WRITE, WAIT_FOR_IND
        }

        private var state = ConnectWifiState.IDLE
        private lateinit var handler : Handler
        private var wifiId : Int = 0
        private lateinit var password : String

        fun startConnect(newHandler : Handler, wifiIndex : Int, newPassword: String) : Boolean {
            if ( state != ConnectWifiState.IDLE) {
                return false
            }

            handler = newHandler
            wifiId = wifiIndex
            password = newPassword
            state = ConnectWifiState.WRITE_START_CMD

            insertAction(null)
            return true
        }

        @SuppressLint("MissingPermission")
        fun insertAction(msg : OTPMessage?) {
            when (state) {

                ConnectWifiState.IDLE -> {
                    return
                }

                ConnectWifiState.WRITE_START_CMD -> {
                    bluetoothGatt.writeCharacteristic(OTPService.otsWifiAction!!, AlarmCharacteristic.prepareConnectWifiPayload(wifiId, password), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    state = ConnectWifiState.WAIT_FOR_WRITE
                }

                ConnectWifiState.WAIT_FOR_WRITE -> {
                    if ((msg?.op == ObjectTransferProfile.OP.WRITE) && (msg.characteristic == OTPService.otsWifiAction) && msg.status == 0) {
                        state = ConnectWifiState.WAIT_FOR_IND
                    }
                }

                ConnectWifiState.WAIT_FOR_IND -> {
                    if ((msg?.op == ObjectTransferProfile.OP.CHANGE) && (msg.characteristic == OTPService.otsWifiAction) && msg.status == 0 && msg.value != null && msg.value.size == 1) {
                        if ( msg.value[0].toInt() == 2 ) {

                            isNixieWifiConnected = true

                            val wifiMsg = Message()
                            wifiMsg.arg1 = 4
                            handler.sendMessage(wifiMsg)

                            state = ConnectWifiState.IDLE

                        } else if (msg.value[0].toInt() == 3 ){

                            isNixieWifiConnected = false

                            val wifiMsg = Message()
                            wifiMsg.arg1 = 5
                            handler.sendMessage(wifiMsg)

                            state = ConnectWifiState.IDLE
                        }
                    }
                }
            }
        }
    }
}


