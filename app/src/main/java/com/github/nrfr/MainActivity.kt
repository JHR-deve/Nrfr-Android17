package com.github.nrfr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.nrfr.ui.screens.AboutScreen
import com.github.nrfr.ui.screens.MainScreen
import com.github.nrfr.ui.screens.ShizukuNotReadyScreen
import com.github.nrfr.ui.theme.NrfrTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private var isShizukuReady by mutableStateOf(false)
    private var isPhoneReadReady by mutableStateOf(false)
    private var showAbout by mutableStateOf(false)
    private val phonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPhoneReadReady = granted
        if (granted) checkShizukuStatus()
    }
    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener {
        _, grantResult ->
        isShizukuReady = grantResult == PackageManager.PERMISSION_GRANTED
        if (!isShizukuReady) {
            Toast.makeText(this, "需要 Shizuku 权限才能运行", Toast.LENGTH_LONG).show()
        }
    }
    private val shizukuBinderListener = Shizuku.OnBinderReceivedListener {
        checkShizukuStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        isPhoneReadReady = checkSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
        if (!isPhoneReadReady) phonePermission.launch(Manifest.permission.READ_PHONE_STATE)
        else checkShizukuStatus()

        // 添加 Shizuku 权限监听器
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        // 添加 Shizuku 绑定监听器
        Shizuku.addBinderReceivedListener(shizukuBinderListener)

        setContent {
            NrfrTheme {
                if (showAbout) {
                    AboutScreen(onBack = { showAbout = false })
                } else if (isShizukuReady && isPhoneReadReady) {
                    MainScreen(onShowAbout = { showAbout = true })
                } else if (!isPhoneReadReady) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("读取已激活 SIM 卡需要电话状态权限；本应用不会读取或修改 SIM 本体。")
                    }
                } else {
                    ShizukuNotReadyScreen()
                }
            }
        }
    }

    private fun checkShizukuStatus() {
        if (!isPhoneReadReady) return
        isShizukuReady = if (Shizuku.getBinder() == null) {
            Toast.makeText(this, "请先安装并启用 Shizuku", Toast.LENGTH_LONG).show()
            false
        } else {
            val hasPermission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                Shizuku.requestPermission(0)
            }
            hasPermission
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        Shizuku.removeBinderReceivedListener(shizukuBinderListener)
    }
}
