package com.example.android_nixie_v2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.android_nixie_v2.AlarmCharacteristic.prepareAlarmPayload
import com.example.android_nixie_v2.databinding.FragmentAlarmBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

class AlarmFragment : Fragment() {

    private var _binding: FragmentAlarmBinding? = null
    private lateinit var alarm : Alarm

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.arg1) {
                ProjectDefs.CREATE_ALARM_SUCCESS -> {
                    alarm.id = msg.arg2
                    AlarmManager.addAlarm(alarm)
                    AlarmManager.resetOnWorkAlarm()
                    AlarmManager.mode = alarm.mode

                    findNavController().navigate(R.id.action_AlarmFragment_to_MenuFragment)
                }

                ProjectDefs.MODIFY_ALARM_SUCCESS -> {
                    AlarmManager.editOnWorkAlarm(alarm)
                    AlarmManager.resetOnWorkAlarm()
                    AlarmManager.mode = alarm.mode

                    findNavController().navigate(R.id.action_AlarmFragment_to_MenuFragment)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AlarmManager.isAlarmNew) {
            alarm = Alarm()
        } else {
            alarm = AlarmManager.getOnWorkAlarm()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentAlarmBinding.inflate(inflater, container, false)

        binding.repeatModeToggleGroup.check(alarm.getButtonId())
        binding.timeInputEditText.setText(alarm.getTimeText())

        setComponentsVisibility()

        binding.descriptionInputEditText.setText(alarm.description)
        binding.soundSlider.value = alarm.soundLevel.toFloat()

        binding.repeatModeToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.singleButton -> alarm.mode = AlarmMode.SINGLE
                    R.id.weeklyButton -> alarm.mode = AlarmMode.WEEKLY
                    R.id.monthlyButton -> alarm.mode = AlarmMode.MONTHLY
                    R.id.yearlyButton -> alarm.mode = AlarmMode.YEARLY
                }
                setComponentsVisibility()
            }
        }

        binding.timePickerBtn.setOnClickListener { view ->
            val timePicker = MaterialTimePicker.Builder()
                                .setTheme(R.style.TimePicker)
                                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                                .setTimeFormat(TimeFormat.CLOCK_24H)
                                .setHour(alarm.time.hour)
                                .setMinute(alarm.time.minute)
                                .setTitleText("Select appointment time")
                                .build()

            timePicker.addOnPositiveButtonClickListener { _ ->
                alarm.time = LocalTime.of(timePicker.hour, timePicker.minute)
                binding.timeInputEditText.setText(alarm.getTimeText())
            }

            timePicker.show(parentFragmentManager, "tag")
        }

        binding.timeInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().matches(Regex("[0-1]?[0-9]:[0-5][0-9]")) || s.toString().matches(Regex("2[0-3]:[0-5][0-9]"))) {
                    binding.timeInputEditTextLayout.error = null
                } else {
                    binding.timeInputEditTextLayout.error = getString(R.string.timeInputError)
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })

        binding.datePickerBtn.setOnClickListener { _ ->
            val constraintsBuilder = CalendarConstraints.Builder()

            if(alarm.mode == AlarmMode.SINGLE) constraintsBuilder.setValidator(DateValidatorPointForward.now());
            val datePicker = MaterialDatePicker.Builder.datePicker().setCalendarConstraints(constraintsBuilder.build()).setTheme(R.style.DatePicker).build()

            datePicker.show(parentFragmentManager, "tag");

            datePicker.addOnPositiveButtonClickListener { selection ->
                val requestDate = Date(selection).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                alarm.date = requestDate
                binding.dateTextView.text = alarm.getDateText()
            }
        }

        binding.monCheckBox.setOnCheckedChangeListener { _, isChecked ->
            alarm.daySet.monday = isChecked
        }

        binding.tueCheckBox.setOnCheckedChangeListener { _, isChecked ->
            alarm.daySet.tuesday = isChecked
        }

        binding.wenCheckBox.setOnCheckedChangeListener { _, isChecked ->
            alarm.daySet.wednesday = isChecked
        }

        binding.thuCheckBox.setOnCheckedChangeListener { _, isChecked ->
            alarm.daySet.thursday = isChecked
        }

        binding.friCheckBox.setOnCheckedChangeListener { _, isChecked ->
            alarm.daySet.friday = isChecked
        }

        binding.satCheckBox.setOnCheckedChangeListener { _, isChecked ->
            alarm.daySet.saturday = isChecked
        }

        binding.sunCheckBox.setOnCheckedChangeListener { _, isChecked ->
            alarm.daySet.sunday = isChecked
        }

        binding.saveButton.setOnClickListener { _ ->
            if ( binding.timeInputEditTextLayout.error != null ) {
                Toast.makeText(context, "Wrong time format", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if ( alarm.mode == AlarmMode.SINGLE ) {
                val localTimeNow = LocalDateTime.now()
                val localTimeAlarm = LocalDateTime.of(alarm.date, alarm.time)

                if (localTimeAlarm.isBefore(localTimeNow)) {
                    Toast.makeText(context, "Alarm is set in the past", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            binding.progressIndicator.visibility = View.VISIBLE
            disableLayout ()

            alarm.description = binding.descriptionInputEditText.text.toString()
            alarm.soundLevel = binding.soundSlider.value.toInt()
            alarm.enable = true

            BleManager.writeAlarmToRemote(alarm, handler)
        }

        binding.cancelButton.setOnClickListener { _ ->
            findNavController().navigate(R.id.action_AlarmFragment_to_MenuFragment)
        }

        return binding.root
    }

    fun setComponentsVisibility () {
        if (alarm.mode == AlarmMode.WEEKLY) {
            binding.dateLinLayout.visibility = View.GONE
            binding.daysLayout.visibility = View.VISIBLE
            binding.monCheckBox.isChecked = alarm.daySet.monday
            binding.tueCheckBox.isChecked = alarm.daySet.tuesday
            binding.wenCheckBox.isChecked = alarm.daySet.wednesday
            binding.thuCheckBox.isChecked = alarm.daySet.thursday
            binding.friCheckBox.isChecked = alarm.daySet.friday
            binding.satCheckBox.isChecked = alarm.daySet.saturday
            binding.sunCheckBox.isChecked = alarm.daySet.sunday
        } else {
            binding.dateLinLayout.visibility = View.VISIBLE
            binding.dateTextView.text = alarm.getDateText()
            binding.daysLayout.visibility = View.GONE
        }
    }

    fun disableLayout () {
        binding.repeatModeToggleGroup.isEnabled = false
        binding.timeInputEditTextLayout.isEnabled = false
        binding.timePickerBtn.isEnabled = false
        binding.datePickerBtn.isEnabled = false
        binding.descriptionInputEditText.isEnabled = false
        binding.soundSlider.isEnabled = false
        binding.saveButton.isEnabled = false
        binding.cancelButton.isEnabled = false

        binding.monCheckBox.isEnabled = false
        binding.tueCheckBox.isEnabled = false
        binding.wenCheckBox.isEnabled = false
        binding.thuCheckBox.isEnabled = false
        binding.friCheckBox.isEnabled = false
        binding.satCheckBox.isEnabled = false
        binding.sunCheckBox.isEnabled = false
    }
}