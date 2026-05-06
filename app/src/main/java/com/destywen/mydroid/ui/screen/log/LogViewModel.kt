package com.destywen.mydroid.ui.screen.log

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.MyApplication
import com.destywen.mydroid.data.local.LogDao
import com.destywen.mydroid.data.local.LogEntity
import com.destywen.mydroid.domain.AppLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogViewModel(private val logDao: LogDao) : ViewModel() {
    val logs: StateFlow<List<LogEntity>> =
        logDao.getRecentLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDebugLog() = viewModelScope.launch {
        logDao.deleteDebugLog()
        AppLogger.i("LogViewModel", "clear debug log")
    }

    companion object {
        fun Factory(application: Application) = viewModelFactory {
            initializer {
                val app = application as MyApplication
                LogViewModel(app.database.logDao())
            }
        }
    }
}
