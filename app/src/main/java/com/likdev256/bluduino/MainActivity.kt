package com.likdev256.bluduino

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.window.DialogProperties
import com.likdev256.bluduino.bluetooth.BluetoothUiState
import com.likdev256.bluduino.bluetooth.BluetoothViewModel
import com.likdev256.bluduino.ui.theme.BluDuinoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val bluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { /* Not needed */ }

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val canEnableBluetooth = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else true

            if(canEnableBluetooth && !isBluetoothEnabled) {
                enableBluetoothLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )
            }
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }

        setContent {
            val viewModel = hiltViewModel<BluetoothViewModel>()
            val state by viewModel.state.collectAsState()

            BluDuinoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Main app content
                    HomeScreen(state)
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(state: BluetoothUiState) {
    // Use a mutable state to control the visibility of the dialog
    val showDialog = remember { mutableStateOf(false) }

    val bluetoothViewModel = hiltViewModel<BluetoothViewModel>()

    // Define the animation for the Connect button's alpha (blinking effect)
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val alphaAnimation by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    // Determine the alpha value based on whether the connection is established
    val alphaValue: Float
    var color = MaterialTheme.colorScheme.error
    var connectButtonText = "Connect"
    if (state.isConnected) {
        alphaValue = 1f
        color = MaterialTheme.colorScheme.primary
        connectButtonText = "Connected"
    } else {
        alphaValue = alphaAnimation
    }

    // Main UI
    Scaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { showDialog.value = true },
                modifier = Modifier
                    .padding(24.dp)
                    .alpha(alphaValue)
                    .size(150.dp, 50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = color
                )
            ) {
                Text(text = connectButtonText)
            }

            // Bluetooth Dialog
            if (showDialog.value) {
                BluetoothDialog(
                    onDismissRequest = {
                        showDialog.value = false
                        bluetoothViewModel.stopDiscovery()
                    },
                    state = state
                )
            }

            // Joystick
            val joystickColor = linearGradient(
                listOf(
                    MaterialTheme.colorScheme.inversePrimary,
                    MaterialTheme.colorScheme.inverseOnSurface
                )
            )
            JoyStick(joystickColor, MaterialTheme.colorScheme.primary) { xOffset, yOffset ->
                // Send joystick movement
                    val message = "${xOffset.toInt()},${yOffset.toInt()}"
                    bluetoothViewModel.sendMessage(message)
            }
        }
    }
}

// BluetoothDialog function
@Composable
fun BluetoothDialog(
    onDismissRequest: () -> Unit,
    state: BluetoothUiState,
) {
    val bluetoothViewModel = hiltViewModel<BluetoothViewModel>()

    // Start Bluetooth discovery when the dialog is launched
    LaunchedEffect(Unit) {
        bluetoothViewModel.startDiscovery()
    }

    // Define a variable to hold the visibility of the connected dialog
    val showConnectedDialog = state.isConnected

    // Define a variable to hold the visibility of the connecting dialog
    val showConnectingDialog = state.isConnecting

    // Animate the visibility transition for the connected dialog
    AnimatedVisibility(visible = showConnectedDialog) {
        // Display the small dialog containing only the word "Connected"
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Connected Icon",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Connected",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp) // Add space between icon and text
                    )
                }
            },
            confirmButton = {
                Button(onClick = onDismissRequest) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { bluetoothViewModel.disconnectFromDevice() }) {
                    Text("Disconnect")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = true,
                decorFitsSystemWindows = true
            )
        )
    }

    // Animate the visibility transition for the connecting dialog
    AnimatedVisibility(visible = showConnectingDialog) {
        // Display the small dialog containing the circular progress indicator and the word "Connecting"
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Connecting",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp) // Add space between progress indicator and text
                    )
                }
            },
            confirmButton = {},
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = true,
                decorFitsSystemWindows = true
            )
        )
    }

    // Animate the visibility transition for the Bluetooth devices dialog
    AnimatedVisibility(visible = !showConnectedDialog && !showConnectingDialog) {
        // Original dialog content
        AlertDialog(
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true
            ),
            onDismissRequest = onDismissRequest,
            title = {
                // Add a Row to contain the title and CircularProgressIndicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Bluetooth Devices",
                        fontWeight = FontWeight.Bold
                    )
                    // Add CircularProgressIndicator based on discovery state
                    if (state.isDiscoverStarted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Section for paired devices
                    item {
                        Text(
                            text = "Paired Devices",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(state.pairedDevices) { device ->
                        // Display device name and MAC address
                        val displayText = "${device.name ?: "(No name)"} - ${device.address}"
                        Text(
                            text = displayText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { bluetoothViewModel.connectToDevice(device) }
                                .padding(16.dp)
                        )
                    }

                    // Section for scanned devices
                    item {
                        Text(
                            text = "Scanned Devices",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(state.scannedDevices) { device ->
                        // Display device name and MAC address
                        val displayText = "${device.name ?: "(No name)"} - ${device.address}"
                        Text(
                            text = displayText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { bluetoothViewModel.connectToDevice(device) }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = bluetoothViewModel::startDiscovery) {
                    Text(text = "Refresh")
                }
            },
            dismissButton = {
                Button(onClick = onDismissRequest) {
                    Text(text = "Close")
                }
            }
        )
    }
}


// BluetoothDeviceItem function
@SuppressLint("MissingPermission")
@Composable
fun BluetoothDeviceItem(device: BluetoothDevice, onClick: (BluetoothDevice) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(device) }
    ) {
        Text(text = "${device.name} (${device.address})")
    }
}

@Composable
fun BTErrorDialog(
    onDismissRequest: () -> Unit,
    onRelaunch: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = "Warning Icon", modifier = Modifier.size(30.dp))
        },
        title = { Text(text = "Bluetooth Unavailable") },
        text = { Text("Bluetooth is essential for the proper function of this app") },
        dismissButton = {
            Button(onClick = onExit) {
                Text("Exit")
            }
        },
        confirmButton = {
            Button(onClick = onRelaunch) {
                Text("Relaunch")
            }
        }
    )
}

