package com.destywen.mydroid.ui.screen.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.AppContainer
import com.destywen.mydroid.domain.AppLogger
import com.destywen.mydroid.data.local.LogDao
import com.destywen.mydroid.data.local.LogEntity
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
        fun Factory(container: AppContainer) = viewModelFactory {
            initializer {
                LogViewModel(container.logDao)
            }
        }
    }
}
