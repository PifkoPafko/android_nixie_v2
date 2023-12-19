package com.example.android_nixie_v2

import android.content.DialogInterface
import android.content.res.ColorStateList
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
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android_nixie_v2.databinding.FragmentWifiBinding
import com.google.android.material.datepicker.MaterialTextInputPicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class WifiFragment : Fragment() {

    private var _binding: FragmentWifiBinding? = null
    private val binding get() = _binding!!

    private lateinit var wifiRecyclerView : RecyclerView
    private lateinit var wifiAdapter : WifiAdapter
    private lateinit var handler : Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                when (msg.arg1) {
                    1 -> {
                        wifiAdapter.notifyDataSetChanged()
                    }

                    2 -> {
                        wifiAdapter.enable = true
                        setEnableLayout(true)
                        wifiAdapter.notifyDataSetChanged()
                    }

                    3 -> {
                        showDialog(msg.arg2)
                    }

                    4 -> {
                        wifiAdapter.enable = true
                        setEnableLayout(true)
                        wifiAdapter.notifyDataSetChanged()
                        Toast.makeText(requireContext(), "Wi-Fi connected", Toast.LENGTH_LONG).show()
                    }

                    5 -> {
                        wifiAdapter.enable = true
                        setEnableLayout(true)
                        wifiAdapter.notifyDataSetChanged()
                        Toast.makeText(requireContext(), "Connecting to Wi-Fi failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentWifiBinding.inflate(inflater, container, false)

        wifiRecyclerView = binding.wifiRecView
        wifiRecyclerView.layoutManager = LinearLayoutManager(context)

        wifiAdapter = WifiAdapter(handler)
        wifiRecyclerView.adapter = wifiAdapter

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_WifiFragment_to_MenuFragment)
        }

        binding.searchButton.setOnClickListener {
            wifiAdapter.enable = false
            wifiAdapter.notifyDataSetChanged()

            setEnableLayout(false)
            WifiSet.wifiList.clear()
            BleManager.startWifiSearch(handler)
        }

        return binding.root
    }

    fun setEnableLayout(enable : Boolean)
    {
        binding.backButton.isEnabled = enable
        binding.searchButton.isEnabled = enable

        if (enable) {
            binding.progressIndicator.visibility = View.INVISIBLE
        } else {
            binding.progressIndicator.visibility = View.VISIBLE
        }
    }

    fun showDialog (wifiIndex : Int) {
        val viewDialog = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_wifi, null)
        val passwordTextInput = viewDialog.findViewById<TextInputEditText>(R.id.passwordEditText)

        MaterialAlertDialogBuilder(requireContext())
            .setView(viewDialog)
            .setPositiveButton("Connect") { _, _ ->
                wifiAdapter.enable = false
                wifiAdapter.notifyDataSetChanged()

                setEnableLayout(false)
                BleManager.startWifiConnect(handler, wifiIndex, passwordTextInput.text.toString())
            }
            .setNegativeButton("Cancel") { _, _ ->

            }.show()
    }
}
