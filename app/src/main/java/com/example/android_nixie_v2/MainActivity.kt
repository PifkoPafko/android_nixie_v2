package com.example.android_nixie_v2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.android_nixie_v2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestPermissionLauncher = registerForActivityResult( ActivityResultContracts.RequestPermission() ) { isGranted: Boolean ->
            if (!isGranted) {
                val toast = Toast.makeText(this, "Aplikacja musi mieć pozwolenie na używanie Bluetooth.", Toast.LENGTH_LONG)
                toast.show()
                finishAndRemoveTask()
            }
        }

        when { ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissionLauncher.launch( Manifest.permission.BLUETOOTH )
            }
        }

        when { ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissionLauncher.launch( Manifest.permission.BLUETOOTH_CONNECT )
            }
        }

        val result = BleManager.bleManagerInit(this)
        if ( !result ) {
            val toast = Toast.makeText(this, "Błąd inicjalizacji kontrolera Bluetooth.", Toast.LENGTH_LONG)
            toast.show()
            finishAndRemoveTask()
        }
    }
}