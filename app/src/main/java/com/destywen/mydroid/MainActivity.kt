package com.destywen.mydroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.destywen.mydroid.data.local.AppDatabase
import com.destywen.mydroid.data.local.AppLogger
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.ui.screen.MainApp
import com.destywen.mydroid.ui.theme.MyDroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.get(this)
        val settings = AppSettings(this)
        AppLogger.init(db.logDao())
        AppLogger.i("MainActivity", "------应用启动，祝一切顺利哦------")
        setContent {
            MyDroidTheme {
                MainApp(db, settings)
            }
        }
    }
}
