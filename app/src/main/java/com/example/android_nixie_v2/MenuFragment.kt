package com.example.android_nixie_v2

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColor
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android_nixie_v2.databinding.FragmentMenuBinding


class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var handler : Handler? = null

    private lateinit var alarmRecyclerView : RecyclerView
    private lateinit var adapter : AlarmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                when (msg.arg1) {
                    ProjectDefs.NAV_TO_ALARM_FRAG -> {
                        findNavController().navigate(R.id.action_MenuFragment_to_AlarmFragment)
                    }

                    ProjectDefs.CHANGE_EDIT_MODE -> {
                        setEditVisibility (binding)
                    }

                    ProjectDefs.NOTIFY_CHANGE_REC_VIEW -> {
                        adapter.notifyItemChanged(msg.arg2)
                    }

                    ProjectDefs.NOTIFY_ALL_SET -> {
                        adapter.notifyDataSetChanged()
                        if (BleManager.isNixieWifiConnected) {
                            binding.wifiButton.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_wifi_connected, null)
                            binding.wifiButton.iconTint = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.green, null))
                        } else {
                            binding.wifiButton.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_wifi_disconnected, null)
                            binding.wifiButton.iconTint = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.sundayRed, null))
                        }

                        enableFrag()
                    }

                    ProjectDefs.ENABLE_ALARM_ACTION_START -> {
                        disableFrag()
                    }

                    ProjectDefs.ENABLE_ALARM_ACTION_END -> {
                        enableFrag()
                    }

                    ProjectDefs.DELETE_ALARMS_ACTION_END -> {
                        enableFrag()
                    }

                    ProjectDefs.CONNECTED -> {
                        binding.bluetoothButton.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_bluetooth_connected, null)
                        binding.bluetoothButton.iconTint = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.blue, null))
                    }

                    ProjectDefs.DISCONNECTED -> {
                        binding.bluetoothButton.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_bluetooth_disconnected, null)
                        binding.bluetoothButton.iconTint = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.sundayRed, null))
                        disableFrag()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMenuBinding.inflate(inflater, container, false)

        alarmRecyclerView = binding.AlarmRecView
        alarmRecyclerView.layoutManager = LinearLayoutManager(context)

        adapter = AlarmAdapter(handler, requireContext().resources)
        alarmRecyclerView.adapter = adapter
        BleManager.setMenuHandler(handler)

        binding.chooseFilter.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.singleButton -> AlarmManager.mode = AlarmMode.SINGLE
                    R.id.weeklyButton -> AlarmManager.mode = AlarmMode.WEEKLY
                    R.id.monthlyButton -> AlarmManager.mode = AlarmMode.MONTHLY
                    R.id.yearlyButton -> AlarmManager.mode = AlarmMode.YEARLY
                }

                adapter.notifyDataSetChanged()
            }
        }
        binding.chooseFilter.check(AlarmManager.getButtonId())

        binding.addButton.setOnClickListener { _ ->
            AlarmManager.resetOnWorkAlarm()
            findNavController().navigate(R.id.action_MenuFragment_to_AlarmFragment)
        }

        binding.wifiButton.setOnClickListener { _ ->
            findNavController().navigate(R.id.action_MenuFragment_to_WifiFragment)
        }

        binding.selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            AlarmManager.setChecks(isChecked)
            adapter.notifyDataSetChanged()
        }

        binding.deleteSelectionButton.setOnClickListener {
            val list = AlarmManager.deleteCheckedAlarms()
            disableFrag ()
            BleManager.deleteAlarmToRemote(list, handler!!)
        }

        binding.cancelSelectionButton.setOnClickListener {
            AlarmManager.isEditMode = false
            setEditVisibility (binding)
        }

        binding.bluetoothButton.setOnClickListener {

        }

        if (BleManager.isNixieConnected) {
            binding.bluetoothButton.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_bluetooth_connected, null)
            binding.bluetoothButton.iconTint = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.blue, null))
        } else {
            binding.bluetoothButton.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_bluetooth_disconnected, null)
            binding.bluetoothButton.iconTint = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.sundayRed, null))
        }

        if (BleManager.isNixieWifiConnected) {
            binding.wifiButton.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_wifi_connected, null)
            binding.wifiButton.iconTint = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.green, null))
        } else {
            binding.wifiButton.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_wifi_disconnected, null)
            binding.wifiButton.iconTint = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.sundayRed, null))
        }

        if (BleManager.BluetoothSM.state != BluetoothState.ALL_SET) {
            disableFrag()
        }

        return binding.root
    }

    fun setEditVisibility (binding : FragmentMenuBinding?) {
        if (AlarmManager.isEditMode) {
            binding!!.chooseFilter.visibility = View.GONE
            binding.deleteSelectionButton.visibility = View.VISIBLE
            binding.cancelSelectionButton.visibility = View.VISIBLE
            binding.selectAllLayout.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        } else {
            binding!!.chooseFilter.visibility = View.VISIBLE
            binding.deleteSelectionButton.visibility = View.GONE
            binding.cancelSelectionButton.visibility = View.GONE
            binding.selectAllLayout.visibility = View.GONE
            adapter.notifyDataSetChanged()
            AlarmManager.setChecks(false)
        }
    }

    fun disableFrag () {
        adapter.enable = false
        adapter.notifyDataSetChanged()

        binding.progressIndicator.visibility = View.VISIBLE
        binding.selectAllCheckBox.isEnabled = false
        binding.AlarmRecView.isEnabled = false
        binding.addButton.isEnabled = false
        binding.bluetoothButton.isEnabled = false
        binding.wifiButton.isEnabled = false
        binding.chooseFilter.isEnabled = false
        binding.deleteSelectionButton.isEnabled = false
        binding.cancelSelectionButton.isEnabled = false
    }

    fun enableFrag () {
        adapter.enable = true
        adapter.notifyDataSetChanged()

        binding.progressIndicator.visibility = View.INVISIBLE
        binding.selectAllCheckBox.isEnabled = true
        binding.AlarmRecView.isEnabled = true
        binding.addButton.isEnabled = true
        binding.bluetoothButton.isEnabled = true
        binding.wifiButton.isEnabled = true
        binding.chooseFilter.isEnabled = true
        binding.deleteSelectionButton.isEnabled = true
        binding.cancelSelectionButton.isEnabled = true
    }
}