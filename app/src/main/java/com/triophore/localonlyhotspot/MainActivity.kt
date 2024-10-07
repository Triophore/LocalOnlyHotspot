package com.triophore.localonlyhotspot

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.triophore.localonlyhotspot.ui.theme.LocalOnlyHotspotTheme
import com.triophore.weaver.Weaver
import com.triophore.weaver.WeaverImpl
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val weaver = Weaver.create(this@MainActivity)
        val notificationManager = NotificationManager(this@MainActivity).apply {
            setNotification(logoResId = R.drawable.ic_launcher_foreground)
        }
        MainScope().launch {
            weaver.tetherState.collect {
                if (it)
                    notificationManager.createNotification()
                else
                    notificationManager.dismissNotification()
            }
        }
        setContent {
            var ssid by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            LocalOnlyHotspotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UI(ssid = ssid,
                        onSsidChange = { ssid = it },
                        password = password,
                        onPasswordChange = { password = it },
                        innerPadding = innerPadding,
                        onStartClick = {
                            if (ssid.isEmpty() && password.isEmpty())
                                toast("SSID and Password cannot be empty")
                            else if (ssid.isEmpty())
                                toast("SSID cannot be empty")
                            else if (password.isEmpty())
                                toast("Password cannot be empty")
                            else
                                weaver.startTethering(ssid, password)
                        },
                        onStopClick = {weaver.stopTethering()})
                }
            }
        }
    }
    private fun toast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun UI(
    ssid: String,
    onSsidChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    innerPadding: PaddingValues,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(30.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            label = {Text( text = "SSID")},
            value = ssid,
            onValueChange = onSsidChange)
        OutlinedTextField(
            label = {Text( text = "Password")},
            value = password,
            onValueChange = onPasswordChange
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp), onClick = onStartClick
        ) {
            Text("Start")
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp), onClick = onStopClick
        ) {
            Text("Stop")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LocalOnlyHotspotTheme {
        UI(ssid = "",
            onSsidChange = {},
            password = "",
            onPasswordChange = {},
            innerPadding = PaddingValues(10.dp),
            onStartClick = {},
            onStopClick = {})
    }
}