package com.example.android_nixie_v2

object AlarmManager {
    var mode : AlarmMode = AlarmMode.WEEKLY
    var alarmOnWorkPosition : Int = -1
    var alarmOnWorkMode : AlarmMode = AlarmMode.SINGLE
    var isAlarmNew : Boolean = false
    var isEditMode : Boolean = false
    var editMode : AlarmMode = AlarmMode.SINGLE

    val alarmsOneTime : ArrayList<Alarm> = arrayListOf()
    val alarmsEveryWeek : ArrayList<Alarm> = arrayListOf()
    val alarmsEveryMonth : ArrayList<Alarm> = arrayListOf()
    val alarmsEveryYear : ArrayList<Alarm> = arrayListOf()

    fun getAlarmSet (mode : AlarmMode) : ArrayList<Alarm> {
        return when (mode) {
            AlarmMode.SINGLE -> alarmsOneTime
            AlarmMode.WEEKLY -> alarmsEveryWeek
            AlarmMode.MONTHLY -> alarmsEveryMonth
            AlarmMode.YEARLY -> alarmsEveryYear
        }
    }

    fun getCurrentAlarmSet () : ArrayList<Alarm> {
        return when (mode) {
            AlarmMode.SINGLE -> alarmsOneTime
            AlarmMode.WEEKLY -> alarmsEveryWeek
            AlarmMode.MONTHLY -> alarmsEveryMonth
            AlarmMode.YEARLY -> alarmsEveryYear
        }
    }

    fun getCurrentAlarm (position : Int) : Alarm {
        return when (mode) {
            AlarmMode.SINGLE -> alarmsOneTime[position]
            AlarmMode.WEEKLY -> alarmsEveryWeek[position]
            AlarmMode.MONTHLY -> alarmsEveryMonth[position]
            AlarmMode.YEARLY -> alarmsEveryYear[position]
        }
    }

    fun addAlarm(alarm : Alarm, position : Int = -1) {
        var alarmPosition = position

        if (alarmPosition == -1) {
            alarmPosition = getAlarmSet(alarm.mode).size
        }

        when (alarm.mode) {
            AlarmMode.SINGLE -> alarmsOneTime.add(alarmPosition, alarm)
            AlarmMode.WEEKLY -> alarmsEveryWeek.add(alarmPosition, alarm)
            AlarmMode.MONTHLY -> alarmsEveryMonth.add(alarmPosition, alarm)
            AlarmMode.YEARLY -> alarmsEveryYear.add(alarmPosition, alarm)
        }
    }

    fun editOnWorkAlarm(alarm : Alarm) {
        getAlarmSet(alarm.mode).removeAt(alarmOnWorkPosition)
        getAlarmSet(alarm.mode).add(alarmOnWorkPosition, alarm)
        mode = alarm.mode
    }

    fun setOnWorkAlarm(mode : AlarmMode, position : Int)
    {
        if (position < 0) {
            alarmOnWorkPosition = -1
        } else {
            alarmOnWorkPosition = position
        }

        alarmOnWorkMode = mode
        isAlarmNew = false
    }

    fun resetOnWorkAlarm()
    {
        alarmOnWorkMode = AlarmMode.SINGLE
        alarmOnWorkPosition = -1
        isAlarmNew = true
    }

    fun getOnWorkAlarm() : Alarm
    {
        return getAlarm(alarmOnWorkMode, alarmOnWorkPosition)
    }

    fun getAlarm(mode : AlarmMode, position : Int) : Alarm
    {
        return getAlarmSet(mode)[position]
    }

    fun getButtonId () : Int
    {
        return when (mode) {
            AlarmMode.SINGLE -> R.id.singleButton
            AlarmMode.WEEKLY -> R.id.weeklyButton
            AlarmMode.MONTHLY -> R.id.monthlyButton
            AlarmMode.YEARLY -> R.id.yearlyButton
        }
    }

    fun setChecks (enable : Boolean) {
        for (alarm in alarmsOneTime) {
            alarm.checked = enable
        }
        for (alarm in alarmsEveryWeek) {
            alarm.checked = enable
        }
        for (alarm in alarmsEveryMonth) {
            alarm.checked = enable
        }
        for (alarm in alarmsEveryYear) {
            alarm.checked = enable
        }
    }

    fun deleteCheckedAlarms () : ArrayList<Int> {
        val list : ArrayList<Int> = arrayListOf()

        for (i in alarmsOneTime.lastIndex downTo 0) {
            if (alarmsOneTime[i].checked) {
                list.add(alarmsOneTime[i].id)
                alarmsOneTime.removeAt(i)
            }
        }

        for (i in alarmsEveryWeek.lastIndex downTo 0) {
            if (alarmsEveryWeek[i].checked) {
                list.add(alarmsEveryWeek[i].id)
                alarmsEveryWeek.removeAt(i)
            }
        }

        for (i in alarmsEveryMonth.lastIndex downTo 0) {
            if (alarmsEveryMonth[i].checked) {
                list.add(alarmsEveryMonth[i].id)
                alarmsEveryMonth.removeAt(i)
            }
        }

        for (i in alarmsEveryYear.lastIndex downTo 0) {
            if (alarmsEveryYear[i].checked) {
                list.add(alarmsEveryYear[i].id)
                alarmsEveryYear.removeAt(i)
            }
        }

        return list
    }

    fun getModeFromOrdinal(value : Int) : AlarmMode {
        return when (value) {
            0 -> AlarmMode.SINGLE
            1 -> AlarmMode.WEEKLY
            2 -> AlarmMode.MONTHLY
            3 -> AlarmMode.YEARLY
            else -> AlarmMode.SINGLE
        }
    }
}