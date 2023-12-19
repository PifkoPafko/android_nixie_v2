package com.example.android_nixie_v2

import java.time.LocalDate
import java.time.LocalTime

class Alarm {
    var mode : AlarmMode
    var enable : Boolean
    var description : String
    var time : LocalTime
    var date : LocalDate
    var daySet : DaySet
    var soundLevel : Int
    var checked : Boolean = false
    var id : Int = 0

    constructor() {
        this.description = ""
        this.time = LocalTime.of(6, 0)
        this.date = LocalDate.now().plusDays(1)
        this.soundLevel = 100
        this.enable = true
        this.mode = AlarmMode.SINGLE
        this.daySet = DaySet()
    }

    constructor(mode: AlarmMode, enable: Boolean, description: String, time: LocalTime, date: LocalDate, daySet: DaySet,
                soundLevel: Int) {
        this.description = description
        this.time = time
        this.date = date
        this.soundLevel = soundLevel
        this.enable = enable
        this.mode = mode
        this.daySet = daySet
    }

    constructor(alarm: Alarm) {
        this.description = alarm.description
        this.enable = alarm.enable
        this.time = alarm.time
        this.date = alarm.date
        this.soundLevel = alarm.soundLevel
        this.daySet = alarm.daySet
        this.checked = alarm.checked
        this.mode = alarm.mode
    }

    fun getTimeText () : String {
        return String.format("%02d:%02d", time.hour, time.minute)
    }

    fun getDateText () : String {
        when(mode) {
            AlarmMode.SINGLE -> {
                val monthString = String.format("%c%s",date.month.name.first(), date.month.name.substring(1, 3).lowercase())
                val dayOfWeekString = String.format("%c%s",date.dayOfWeek.name.first(), date.dayOfWeek.name.substring(1, 3).lowercase())

                return String.format("%d. %s %d, %s", date.dayOfMonth, monthString, date.year, dayOfWeekString)
            }

            AlarmMode.WEEKLY -> {
                return ""
            }

            AlarmMode.MONTHLY -> {
                return String.format("%d. day of month", date.dayOfMonth)
            }

            AlarmMode.YEARLY -> {
                val monthString = String.format("%c%s",date.month.name.first(), date.month.name.substring(1, 3).lowercase())

                return String.format("%d. %s", date.dayOfMonth, monthString)
            }
        }
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
}