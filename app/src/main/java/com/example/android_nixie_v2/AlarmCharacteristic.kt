package com.example.android_nixie_v2

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.util.Arrays
import kotlin.experimental.and
import kotlin.experimental.or

object AlarmCharacteristic {
    fun booleanToInt(b: Boolean) = if (b) 1 else 0
    fun booleanToUByte(b: Boolean) = booleanToInt(b).toUByte()
    fun byteToBoolean(b: Byte) = b.toInt() != 0

    @OptIn(ExperimentalUnsignedTypes::class)
    fun uIntToUByteArray (data : UInt ) : UByteArray {
        val payload : ArrayList<UByte> = arrayListOf()
        payload.add( (data and 0x000000FFu).toUByte() )
        payload.add( ((data and 0x0000FF00u) shr 8 ).toUByte() )
        payload.add( ((data and 0x00FF0000u) shr 16 ).toUByte() )
        payload.add( ((data and 0xFF000000u) shr 24 ).toUByte() )
        return payload.toUByteArray()
    }

    fun daySetToByte(daySet : DaySet) : Byte {
        var days : UByte = 0u

        days = days or booleanToUByte(daySet.monday)
        days = days or (booleanToInt(daySet.tuesday) shl 1).toUByte()
        days = days or (booleanToInt(daySet.wednesday) shl 2).toUByte()
        days = days or (booleanToInt(daySet.thursday) shl 3).toUByte()
        days = days or (booleanToInt(daySet.friday) shl 4).toUByte()
        days = days or (booleanToInt(daySet.saturday) shl 5).toUByte()
        days = days or (booleanToInt(daySet.sunday) shl 6).toUByte()

        return days.toByte()
    }

    fun byteToDaySet (value : Byte) : DaySet {
        val result = DaySet()

        result.monday = value and 0x01 > 0
        result.tuesday = value and 0x02 > 0
        result.wednesday = value and 0x04 > 0
        result.thursday = value and 0x08 > 0
        result.friday = value and 0x10 > 0
        result.saturday = value and 0x20 > 0
        result.sunday = value and 0x40 > 0

        return result
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun prepareAlarmPayload (alarm : Alarm) : ByteArray {
        val payload : ArrayList<Byte> = arrayListOf()
        payload.add(alarm.mode.ordinal.toByte())
        payload.add(booleanToInt(alarm.enable).toByte())
        payload.add(alarm.description.length.toByte())

        val descArray = alarm.description.toByteArray(Charsets.UTF_8)
        for (letter in descArray) {
            payload.add(letter.toByte())
        }

        payload.add(alarm.time.hour.toByte())
        payload.add(alarm.time.minute.toByte())

        when (alarm.mode) {
            AlarmMode.SINGLE -> {
                payload.add(alarm.date.dayOfMonth.toByte())
                payload.add(alarm.date.monthValue.toByte())
                payload.add((alarm.date.year - 2000).toByte())
            }

            AlarmMode.WEEKLY -> {
                payload.add(daySetToByte(alarm.daySet))
            }

            AlarmMode.MONTHLY -> {
                payload.add(alarm.date.dayOfMonth.toByte())
            }

            AlarmMode.YEARLY -> {
                payload.add(alarm.date.dayOfMonth.toByte())
                payload.add(alarm.date.monthValue.toByte())
            }
        }

        payload.add(alarm.soundLevel.toByte())

        return payload.toByteArray()
    }

    fun addAlarmFromPayload (payload : ByteArray) : Alarm? {

        if (payload.size < ALARM_MODE_PAYLOAD_SIZE_MIN || payload.size > ALARM_MODE_PAYLOAD_SIZE_MAX)
        {
            return null
        }

        val newAlarm = Alarm()
        var offset = 0

        if (payload[offset].toInt() < 0 || payload[offset].toInt() > 3) {
            return null
        }

        newAlarm.mode = AlarmManager.getModeFromOrdinal(payload[offset].toInt())
        offset += ALARM_FIELD_SIZE

        if (payload[offset].toInt() < 0 || payload[offset].toInt() > 1) {
            return null
        }

        newAlarm.enable = byteToBoolean(payload[offset])
        offset += ALARM_FIELD_SIZE

        if (payload[offset].toInt() < 0 || payload[offset].toInt() > ALARM_DESC_LEN_MAX) {
            return null
        }

        val descriptionLen = payload[offset]
        offset += ALARM_FIELD_SIZE

        when (newAlarm.mode) {
            AlarmMode.SINGLE -> {
                if (payload.size != ALARM_MODE_SINGLE_PAYLOAD_SIZE_MIN + descriptionLen) {
                    return null
                }
            }

            AlarmMode.WEEKLY -> {
                if (payload.size != ALARM_MODE_WEEKLY_PAYLOAD_SIZE_MIN + descriptionLen) {
                    return null
                }
            }

            AlarmMode.MONTHLY -> {
                if (payload.size != ALARM_MODE_MONTHLY_PAYLOAD_SIZE_MIN + descriptionLen) {
                    return null
                }
            }

            AlarmMode.YEARLY -> {
                if (payload.size != ALARM_MODE_YEARLY_PAYLOAD_SIZE_MIN + descriptionLen) {
                    return null
                }
            }
        }

        newAlarm.description = String(payload.copyOfRange(offset, offset + descriptionLen), Charsets.UTF_8)
        offset += descriptionLen

        val hour = payload[offset].toInt()
        offset += ALARM_FIELD_SIZE

        val minute = payload[offset].toInt()
        offset += ALARM_FIELD_SIZE

        try {
            newAlarm.time = LocalTime.of(hour, minute)
        } catch (e : DateTimeException) {
            return null
        }

        when (newAlarm.mode) {
            AlarmMode.SINGLE -> {
                val day = payload[offset].toInt()
                offset += ALARM_FIELD_SIZE

                val month = payload[offset].toInt()
                offset += ALARM_FIELD_SIZE

                val year = payload[offset].toInt() + 2000
                offset += ALARM_FIELD_SIZE

                try {
                    newAlarm.date = LocalDate.of(year, month, day)
                } catch (e : DateTimeException) {
                    return null
                }
            }

            AlarmMode.WEEKLY -> {
                if (payload[offset] and 0x80.toByte() > 0) {
                    return null
                }

                newAlarm.daySet = byteToDaySet(payload[offset])
                offset += ALARM_FIELD_SIZE
            }

            AlarmMode.MONTHLY -> {
                val day = payload[offset].toInt()

                try {
                    newAlarm.date = LocalDate.of(4, 1, day)
                } catch (e : DateTimeException) {
                    return null
                }

                offset += ALARM_FIELD_SIZE
            }

            AlarmMode.YEARLY -> {
                val day = payload[offset].toInt()
                offset += ALARM_FIELD_SIZE

                val month = payload[offset].toInt()
                offset += ALARM_FIELD_SIZE

                try {
                    newAlarm.date = LocalDate.of(4, month, day)
                } catch (e : DateTimeException) {
                    return null
                }
            }
        }

        newAlarm.soundLevel = payload[offset].toInt()

        if (newAlarm.soundLevel < 0 || newAlarm.soundLevel > 100) {
            return null
        }

        newAlarm.checked = false
        newAlarm.id = -1

        return newAlarm
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun prepareCreateAlarmObjectPayload () : ByteArray {
        val payload : ArrayList<Byte> = arrayListOf()

        payload.add(ObjectTransferProfile.OACP_CREATE.toByte())

        val size : UInt = 0u
        for (byte in uIntToUByteArray(size)) {
            payload.add(byte.toByte())
        }

        for (byte in ObjectTransferProfile.ALARM_TYPE) {
            payload.add(byte.toByte())
        }

        return payload.toByteArray()
    }

    fun prepareDeleteAlarmObjectPayload () : ByteArray {
        return byteArrayOf(ObjectTransferProfile.OACP_DELETE.toByte())
    }

    fun prepareGoToPayload (id : Int) : ByteArray {
        val payload : ArrayList<Byte> = arrayListOf()

        payload.add(ObjectTransferProfile.OlCP_GOTO)

        payload.add((id.toUInt() and 0x000000FFu).toByte())
        payload.add(((id.toUInt() and 0x0000FF00u) shr 8).toByte())
        payload.add(((id.toUInt() and 0x00FF0000u) shr 16).toByte())
        payload.add(((id.toUInt() and 0xFF000000u) shr 24).toByte())
        payload.add(0)
        payload.add(0)

        return payload.toByteArray()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun prepareListFilterByAlarmTypePayload() : ByteArray {
        val payload : ArrayList<Byte> = arrayListOf()

        payload.add(ObjectTransferProfile.OBJ_LIST_FILT_BY_TYPE)

        for (item in ObjectTransferProfile.ALARM_TYPE) {
            payload.add(item.toByte())
        }

        return payload.toByteArray()
    }

    fun prepareConnectWifiPayload(wifiId : Int, password : String) : ByteArray {
        val payload : ArrayList<Byte> = arrayListOf()

        val wifi = WifiSet.wifiList[wifiId]
        payload.add(ObjectTransferProfile.WIFI_ACTION_CONNECT)
        payload.add(wifi.ssid.length.toByte())

        val ssidArray = wifi.ssid.toByteArray(Charsets.UTF_8)
        for (letter in ssidArray) {
            payload.add(letter)
        }

        payload.add(password.length.toByte())

        val passwordArray = password.toByteArray(Charsets.UTF_8)
        for (letter in passwordArray) {
            payload.add(letter)
        }

        return payload.toByteArray()
    }

    const val ALARM_MODE_PAYLOAD_SIZE_MIN = 7
    const val ALARM_MODE_PAYLOAD_SIZE_MAX = 49

    const val ALARM_MODE_SINGLE_PAYLOAD_SIZE_MIN = 9
    const val ALARM_MODE_SINGLE_PAYLOAD_SIZE_MAX = 49

    const val ALARM_MODE_WEEKLY_PAYLOAD_SIZE_MIN = 7
    const val ALARM_MODE_WEEKLY_PAYLOAD_SIZE_MAX = 47

    const val ALARM_MODE_MONTHLY_PAYLOAD_SIZE_MIN = 7
    const val ALARM_MODE_MONTHLY_PAYLOAD_SIZE_MAX = 47

    const val ALARM_MODE_YEARLY_PAYLOAD_SIZE_MIN = 8
    const val ALARM_MODE_YEARLY_PAYLOAD_SIZE_MAX = 48

    const val ALARM_FIELD_SIZE = 1
    const val ALARM_DESC_LEN_MAX = 40
}