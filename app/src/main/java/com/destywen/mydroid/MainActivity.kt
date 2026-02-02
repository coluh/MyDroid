package com.destywen.mydroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.destywen.mydroid.data.local.AgentSettings
import com.destywen.mydroid.data.local.AppDatabase
import com.destywen.mydroid.data.local.JournalSettings
import com.destywen.mydroid.ui.screen.MainApp
import com.destywen.mydroid.ui.theme.MyDroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.get(this)
        val agentSettings = AgentSettings(this)
        val journalSettings = JournalSettings(this)
        setContent {
            MyDroidTheme {
                MainApp(db, agentSettings, journalSettings)
            }
        }
    }
}
