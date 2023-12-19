package com.example.android_nixie_v2

import android.content.res.Resources
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch

class AlarmAdapter(private val handler: Handler?, private val resources : Resources) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    var enable : Boolean = true
    var enableSwitchListener : Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.alarm_list_item, parent, false)
        return AlarmViewHolder(itemView)
    }

    override fun onBindViewHolder(holder : AlarmViewHolder, position: Int) {
        var currentAlarm = AlarmManager.getCurrentAlarm(holder.absoluteAdapterPosition)
        holder.alarmCardText.text = currentAlarm.description
        holder.alarmCardSwitchButton.isChecked = currentAlarm.enable

        if (AlarmManager.isEditMode) {
            holder.alarmCardSelectBox.isChecked = currentAlarm.checked
        }

        holder.alarmCard.setOnClickListener {
            if (AlarmManager.isEditMode) {
                val msg = Message()
                msg.arg1 = ProjectDefs.NOTIFY_CHANGE_REC_VIEW
                msg.arg2 = holder.absoluteAdapterPosition
                currentAlarm.checked = currentAlarm.checked.not()
                handler?.sendMessage(msg)
            } else {
                currentAlarm = AlarmManager.getCurrentAlarm(holder.absoluteAdapterPosition)
                val msg = Message()
                msg.arg1 = ProjectDefs.NAV_TO_ALARM_FRAG
                AlarmManager.setOnWorkAlarm(currentAlarm.mode, holder.absoluteAdapterPosition)
                handler?.sendMessage(msg)
            }
        }

        holder.alarmCard.setOnLongClickListener {
            currentAlarm = AlarmManager.getCurrentAlarm(holder.absoluteAdapterPosition)
            val msg = Message()
            msg.arg1 = ProjectDefs.CHANGE_EDIT_MODE
            AlarmManager.isEditMode = AlarmManager.isEditMode.not()
            currentAlarm.checked = AlarmManager.isEditMode
            handler?.sendMessage(msg)
            false
        }

        holder.alarmCardSwitchButton.setOnClickListener {
            currentAlarm = AlarmManager.getCurrentAlarm(holder.absoluteAdapterPosition)
            currentAlarm.enable = currentAlarm.enable.not()
            holder.alarmCardSwitchButton.isChecked = currentAlarm.enable
            setCardLetters(holder, currentAlarm)

            val alarmMsg = Message()
            alarmMsg.arg1 = ProjectDefs.ENABLE_ALARM_ACTION_START
            handler?.sendMessage(alarmMsg)

            BleManager.enableAlarmToRemote(currentAlarm, handler!!)
        }

        holder.alarmCardSelectBox.setOnClickListener() {
            currentAlarm = AlarmManager.getCurrentAlarm(holder.absoluteAdapterPosition)
            currentAlarm.checked = currentAlarm.checked.not()
            holder.alarmCardSelectBox.isChecked = currentAlarm.checked

            val msg = Message()
            msg.arg1 = ProjectDefs.NOTIFY_CHANGE_REC_VIEW
            msg.arg2 = holder.absoluteAdapterPosition
            handler!!.sendMessage(msg)
        }

        holder.alarmCardTimeText.text = currentAlarm.getTimeText()

        setCardLetters(holder, currentAlarm)
        setEditMode(holder, currentAlarm)
        setEnableMode(holder)
    }

    override fun getItemCount(): Int {
        return AlarmManager.getCurrentAlarmSet().size
    }

    class AlarmViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val alarmCard = itemView.findViewById<MaterialCardView?>(R.id.alarmCard)
        val alarmCardText = itemView.findViewById<android.widget.TextView?>(R.id.alarmCardText)
        val alarmCardSwitchButton = itemView.findViewById<MaterialSwitch?>(R.id.alarmCardSwitchButton)
        val alarmCardTimeText = itemView.findViewById<android.widget.TextView?>(R.id.alarmCardTimeText)
        val alarmCardSelectBox = itemView.findViewById<CheckBox?>(R.id.alarmCardSelectBox)

        val mondayLetter = itemView.findViewById<android.widget.TextView?>(R.id.mondayLetter)
        val tuesdayLetter = itemView.findViewById<android.widget.TextView?>(R.id.tuesdayLetter)
        val wednesdayLetter = itemView.findViewById<android.widget.TextView?>(R.id.wednesdayLetter)
        val thursdayLetter = itemView.findViewById<android.widget.TextView?>(R.id.thursdayLetter)
        val fridayLetter = itemView.findViewById<android.widget.TextView?>(R.id.fridayLetter)
        val saturdayLetter = itemView.findViewById<android.widget.TextView?>(R.id.saturdayLetter)
        val sundayLetter = itemView.findViewById<android.widget.TextView?>(R.id.sundayLetter)

        val mondayLetterDot = itemView.findViewById<android.widget.TextView?>(R.id.mondayLetterDot)
        val tuesdayLetterDot = itemView.findViewById<android.widget.TextView?>(R.id.tuesdayLetterDot)
        val wednesdayLetterDot = itemView.findViewById<android.widget.TextView?>(R.id.wednesdayLetterDot)
        val thursdayLetterDot = itemView.findViewById<android.widget.TextView?>(R.id.thursdayLetterDot)
        val fridayLetterDot = itemView.findViewById<android.widget.TextView?>(R.id.fridayLetterDot)
        val saturdayLetterDot = itemView.findViewById<android.widget.TextView?>(R.id.saturdayLetterDot)
        val sundayLetterDot = itemView.findViewById<android.widget.TextView?>(R.id.sundayLetterDot)
    }

    private fun setCardLetters (holder : AlarmViewHolder, alarm : Alarm) {
        val enabledTextColor : Int
        val disabledTextColor : Int

        if (alarm.enable) {
            enabledTextColor = ResourcesCompat.getColor(resources, R.color.orange, null)
            disabledTextColor = ResourcesCompat.getColor(resources, R.color.my_text_color, null)
        } else {
            enabledTextColor = ResourcesCompat.getColor(resources, R.color.my_text_color, null)
            disabledTextColor = ResourcesCompat.getColor(resources, R.color.my_text_color_disable, null)
        }

        if (alarm.mode == AlarmMode.WEEKLY)
        {
            if (alarm.daySet.monday && alarm.daySet.tuesday && alarm.daySet.wednesday && alarm.daySet.thursday && alarm.daySet.friday && alarm.daySet.saturday && alarm.daySet.sunday) {

                holder.mondayLetter.setTextColor(enabledTextColor)

                holder.mondayLetter.text = "Everyday"
                holder.tuesdayLetter.text = ""
                holder.wednesdayLetter.text = ""
                holder.thursdayLetter.text = ""
                holder.fridayLetter.text = ""
                holder.saturdayLetter.text = ""
                holder.sundayLetter.text = ""

                holder.mondayLetterDot.text = ""
                holder.tuesdayLetterDot.text = ""
                holder.wednesdayLetterDot.text = ""
                holder.thursdayLetterDot.text = ""
                holder.fridayLetterDot.text = ""
                holder.saturdayLetterDot.text = ""
                holder.sundayLetterDot.text = ""
                return
            }

            holder.mondayLetter.text = "m"
            holder.tuesdayLetter.text = "t"
            holder.wednesdayLetter.text = "w"
            holder.thursdayLetter.text = "t"
            holder.fridayLetter.text = "f"
            holder.saturdayLetter.text = "s"
            holder.sundayLetter.text = "s"

            if (alarm.daySet.monday) {
                holder.mondayLetter.setTextColor(enabledTextColor)
                holder.mondayLetterDot.text = "•"
                holder.mondayLetterDot.setTextColor(enabledTextColor)
            } else {
                holder.mondayLetter.setTextColor(disabledTextColor)
                holder.mondayLetterDot.text = ""
                holder.mondayLetterDot.setTextColor(disabledTextColor)
            }

            if (alarm.daySet.tuesday) {
                holder.tuesdayLetter.setTextColor(enabledTextColor)
                holder.tuesdayLetterDot.text = "•"
                holder.tuesdayLetterDot.setTextColor(enabledTextColor)
            } else {
                holder.tuesdayLetter.setTextColor(disabledTextColor)
                holder.tuesdayLetterDot.text = ""
                holder.tuesdayLetterDot.setTextColor(disabledTextColor)
            }

            if (alarm.daySet.wednesday) {
                holder.wednesdayLetter.setTextColor(enabledTextColor)
                holder.wednesdayLetterDot.text = "•"
                holder.wednesdayLetterDot.setTextColor(enabledTextColor)
            } else {
                holder.wednesdayLetter.setTextColor(disabledTextColor)
                holder.wednesdayLetterDot.text = ""
                holder.wednesdayLetterDot.setTextColor(disabledTextColor)
            }

            if (alarm.daySet.thursday) {
                holder.thursdayLetter.setTextColor(enabledTextColor)
                holder.thursdayLetterDot.text = "•"
                holder.thursdayLetterDot.setTextColor(enabledTextColor)
            } else {
                holder.thursdayLetter.setTextColor(disabledTextColor)
                holder.thursdayLetterDot.text = ""
                holder.thursdayLetterDot.setTextColor(disabledTextColor)
            }

            if (alarm.daySet.friday) {
                holder.fridayLetter.setTextColor(enabledTextColor)
                holder.fridayLetterDot.text = "•"
                holder.fridayLetterDot.setTextColor(enabledTextColor)
            } else {
                holder.fridayLetter.setTextColor(disabledTextColor)
                holder.fridayLetterDot.text = ""
                holder.fridayLetterDot.setTextColor(disabledTextColor)
            }

            if (alarm.daySet.saturday) {
                holder.saturdayLetter.setTextColor(enabledTextColor)
                holder.saturdayLetterDot.text = "•"
                holder.saturdayLetterDot.setTextColor(enabledTextColor)
            } else {
                holder.saturdayLetter.setTextColor(disabledTextColor)
                holder.saturdayLetterDot.text = ""
                holder.saturdayLetterDot.setTextColor(disabledTextColor)
            }

            if (alarm.daySet.sunday) {
                holder.sundayLetter.setTextColor(enabledTextColor)
                holder.sundayLetterDot.text = "•"
                holder.sundayLetterDot.setTextColor(enabledTextColor)
            } else {
                holder.sundayLetter.setTextColor(disabledTextColor)
                holder.sundayLetterDot.text = ""
                holder.sundayLetterDot.setTextColor(disabledTextColor)
            }
        } else {
            holder.mondayLetter.setTextColor(enabledTextColor)
            holder.mondayLetter.text = alarm.getDateText()
            holder.tuesdayLetter.text = ""
            holder.wednesdayLetter.text = ""
            holder.thursdayLetter.text = ""
            holder.fridayLetter.text = ""
            holder.saturdayLetter.text = ""
            holder.sundayLetter.text = ""

            holder.mondayLetterDot.text = ""
            holder.tuesdayLetterDot.text = ""
            holder.wednesdayLetterDot.text = ""
            holder.thursdayLetterDot.text = ""
            holder.fridayLetterDot.text = ""
            holder.saturdayLetterDot.text = ""
            holder.sundayLetterDot.text = ""
        }
    }

    fun setEditMode(holder : AlarmViewHolder, alarm : Alarm) {
        if (AlarmManager.isEditMode) {
            if (alarm.checked) {
                holder.alarmCard.strokeWidth = 3
            } else {
                holder.alarmCard.strokeWidth = 0
            }

            holder.alarmCardSelectBox.isChecked = alarm.checked
            holder.alarmCardSelectBox.visibility = View.VISIBLE
            holder.alarmCardSwitchButton.visibility = View.INVISIBLE

        } else {
            holder.alarmCardSelectBox.visibility = View.GONE
            holder.alarmCardSwitchButton.visibility = View.VISIBLE
            holder.alarmCard.strokeWidth = 0
        }
    }

    fun setEnableMode(holder : AlarmViewHolder) {
        holder.alarmCard.isEnabled = enable
        holder.alarmCardSwitchButton.isEnabled = enable
        holder.alarmCardSelectBox.isEnabled = enable
    }
}