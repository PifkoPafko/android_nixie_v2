package com.example.android_nixie_v2

import java.time.LocalDate
import java.time.LocalTime

data class AlarmData( var mode : AlarmMode,
                      var enable : Boolean,
                      var description : String,
                      var time : LocalTime,
                      var date : LocalDate,
                      var daySet : DaySet,
                      var soundLevel : Int )