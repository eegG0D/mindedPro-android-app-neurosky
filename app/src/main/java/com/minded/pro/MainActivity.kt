package com.minded.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.minded.pro.ui.MonitorScreen
import com.minded.pro.ui.MonitorViewModel
import com.minded.pro.ui.PermissionPrompt
import com.minded.pro.ui.theme.MindedProTheme

/**
 * The app's only activity. It gates the monitor on the runtime Bluetooth
 * permission (required from Android 12) and otherwise just hosts Compose.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MindedProTheme {
                val context = LocalContext.current
                var granted by remember {
                    mutableStateOf(
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT,
                            ) == PackageManager.PERMISSION_GRANTED,
                    )
                }
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { result -> granted = result }

                LaunchedEffect(granted) {
                    if (granted) {
                        viewModel.onConnectAllowed()
                    } else {
                        launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                }

                if (granted) {
                    MonitorScreen(viewModel)
                } else {
                    PermissionPrompt(
                        onGrant = { launcher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                    )
                }
            }
        }
    }
}
