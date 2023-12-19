package com.example.android_nixie_v2

object ObjectTransferProfile {

    const val OTP_SERVICE_UUID = "00001825-0000-1000-8000-00805f9b34fb"
    const val OTS_CHAR_FEATURE_UUID = "00002ABD-0000-1000-8000-00805f9b34fb"
    const val OTS_CHAR_NAME_UUID = "00002ABE-0000-1000-8000-00805f9b34fb"
    const val OTS_CHAR_TYPE_UUID = "00002ABF-0000-1000-8000-00805f9b34fb"
    const val OTS_CHAR_SIZE_UUID = "00002AC0-0000-1000-8000-00805f9b34fb"
    const val OTS_CHAR_ID_UUID = "00002AC3-0000-1000-8000-00805f9b34fb"
    const val OTS_CHAR_PROPERTIES_UUID = "00002AC4-0000-1000-8000-00805f9b34fb"
    const val OTS_CHAR_OACP_UUID = "00002AC5-0000-1000-8000-00805f9b34fb"
    const val OTS_CHAR_OLCP_UUID = "00002AC6-0000-1000-8000-00805f9b34fb"
    const val OTS_CHAR_LIST_FILTER_UUID = "00002AC7-0000-1000-8000-00805f9b34fb"
    const val OTS_DESC_OACP_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    const val OTS_DESC_OLCP_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    const val OTS_DESC_WIFI_UUID = "00002902-0000-1000-8000-00805f9b34fb"

    const val OTS_CHAR_ALARM_ACTION_UUID = "9E913F27-E506-F2AF-9845-AB57E057AB26"
    const val OTS_CHAR_WIFI_ACTION_UUID = "80142A27-E506-F2AF-9845-AB57E057AB26"

    const val CHAR_RSP_SUCCESS : Int = 0x00

    const val OACP_CREATE: UByte = 0x01u
    const val OACP_DELETE: UByte = 0x02u

    const val OACP_RET_SUCCESS: Byte = 0x01
    const val OACP_RET_OP_CODE_NOT_SUPPORTED: Byte = 0x02
    const val OACP_RET_INVALID_PARAMETER: Byte = 0x03
    const val OACP_RET_INSUFF_RES: Byte = 0x04
    const val OACP_RET_INVALID_OBJECT: Byte = 0x05
    const val OACP_RET_UNSUPPORTED_TYPE: Byte = 0x07
    const val OACP_RET_PROC_NOT_PERMITTES: Byte = 0x08
    const val OACP_OP_CODE_RESPONSE : Byte = 0x60

    const val OlCP_FIRST: Byte = 0x01
    const val OlCP_LAST: Byte = 0x02
    const val OlCP_PREVIOUS: Byte = 0x03
    const val OlCP_NEXT: Byte = 0x04
    const val OlCP_GOTO: Byte = 0x05
    const val OlCP_ORDER: Byte = 0x06
    const val OlCP_REQ_NUM: Byte = 0x07
    const val OlCP_CLEAR_MARKING: Byte = 0x08
    const val OlCP_RESPONSE: Byte = 0x70

    const val OLCP_RET_SUCCESS: Byte = 0x01
    const val OLCP_RET_OP_CODE_NOT_SUPPORTED: Byte = 0x02
    const val OLCP_RET_INVALID_PARAMETER: Byte = 0x03
    const val OLCP_RET_OPERATION_FAILED: Byte = 0x04
    const val OLCP_RET_OUT_OF_THE_BONDS: Byte = 0x05
    const val OLCP_RET_TOO_MANY_OBJECTS: Byte = 0x06
    const val OLCP_RET_NO_OBJECT: Byte = 0x07
    const val OLCP_RET_OBJECT_ID_NOT_FOUND: Byte = 0x08

    const val OBJ_LIST_FILT_BY_TYPE : Byte = 0x05

    const val WIFI_ACTION_SEARCH : Byte = 0x01
    const val WIFI_ACTION_CONNECT : Byte = 0x02

    @OptIn(ExperimentalUnsignedTypes::class)
    val ALARM_TYPE : UByteArray = ubyteArrayOf(0x02u, 0x00u, 0x12u, 0xACu, 0x42u, 0x02u, 0x61u, 0xA2u, 0xEDu, 0x11u, 0xBAu, 0x29u, 0xB8u, 0x13u, 0x08u, 0xCCu)

    enum class OP{
        READ, WRITE, CHANGE
    }

    fun checkOACPInd(value : ByteArray?) : Boolean {
        if (value != null) {
            if ((value.size == 2) && (value[0] == OACP_OP_CODE_RESPONSE) && (value[1] == OACP_RET_SUCCESS)) {
                return true
            }
        }

        return false
    }

    fun getOACPIndStatus (value : ByteArray?) : Byte {
        if (value != null) {
            if ((value.size == 2) && (value[0] == OACP_OP_CODE_RESPONSE)) {
                return value[1]
            }
        }

        return -1
    }

    fun checkOLCPInd(value : ByteArray?) : Boolean {
        if (value != null) {
            if ((value.size == 2) && (value[0] == OlCP_RESPONSE) && (value[1] == OLCP_RET_SUCCESS)) {
                return true
            }
        }

        return false
    }

    fun getOLCPIndStatus (value : ByteArray?) : Byte {
        if (value != null) {
            if ((value.size == 2) && (value[0] == OlCP_RESPONSE)) {
                return value[1]
            }
        }

        return -1
    }

    fun byteArrayIdToInt(value: ByteArray) : Int {
        var result : Int = 0
        result += value[0]
        result += value[1].toInt() shl 8
        result += value[2].toInt() shl 16
        result += value[3].toInt() shl 24
        result += value[4].toInt() shl 32
        result += value[5].toInt() shl 40

        return result
    }

}