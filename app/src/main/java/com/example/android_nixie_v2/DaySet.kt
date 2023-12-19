package com.example.android_nixie_v2

class DaySet {
    var monday : Boolean
    var tuesday : Boolean
    var wednesday : Boolean
    var thursday : Boolean
    var friday : Boolean
    var saturday : Boolean
    var sunday : Boolean

    constructor() {
        monday = false
        tuesday = false
        wednesday = false
        thursday = false
        friday = false
        saturday = false
        sunday = false
    }

    constructor(dayList : ArrayList<Boolean>) {
        monday = dayList[0]
        tuesday = dayList[1]
        wednesday = dayList[2]
        thursday = dayList[3]
        friday = dayList[4]
        saturday = dayList[5]
        sunday = dayList[6]
    }

    constructor(monday : Boolean, tuesday : Boolean, wednesday : Boolean, thursday : Boolean, friday : Boolean, saturday : Boolean, sunday : Boolean) {
        this.monday = monday
        this.tuesday = tuesday
        this.wednesday = wednesday
        this.thursday = thursday
        this.friday = friday
        this.saturday = saturday
        this.sunday = sunday
    }
}