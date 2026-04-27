package com.destywen.mydroid.ui.screen.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.AppContainer
import com.destywen.mydroid.data.local.ScheduleDao
import com.destywen.mydroid.data.local.ScheduleEntity
import com.destywen.mydroid.data.local.ScheduleGroupEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduleScreenState(
    val schedules: List<ScheduleEntity> = emptyList(),
    val groups: List<ScheduleGroupEntity> = emptyList(),
    val selectedGroupId: Long? = null,
    val status: String? = null,
)

class ScheduleViewModel(
    private val scheduleDao: ScheduleDao
) : ViewModel() {
    private val _schedules = scheduleDao.getAll()
    private val _groups = scheduleDao.getAllGroups()
    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    private val _status = MutableStateFlow<String?>(null)

    val state: StateFlow<ScheduleScreenState> = combine(_schedules, _groups, _selectedGroupId, _status) { items, groups, selectedGroupId, status ->
        val filtered = if (selectedGroupId != null) {
            items.filter { it.groupId == selectedGroupId }
        } else {
            items
        }
        val sorted = filtered.sortedBy { it.due ?: Long.MAX_VALUE }
        ScheduleScreenState(
            schedules = sorted.filter { !it.isCompleted } + sorted.filter { it.isCompleted },
            groups = groups,
            selectedGroupId = selectedGroupId,
            status = status
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleScreenState())

    fun selectGroup(groupId: Long?) {
        _selectedGroupId.value = groupId
    }

    fun addGroup(name: String) = viewModelScope.launch {
        scheduleDao.upsertGroup(ScheduleGroupEntity(name = name))
    }

    fun deleteGroup(groupId: Long) = viewModelScope.launch {
        scheduleDao.clearGroupFromSchedules(groupId)
        scheduleDao.deleteGroupById(groupId)
    }

    fun addSchedule(title: String, description: String?, due: Long?, groupId: Long? = _selectedGroupId.value) = viewModelScope.launch {
        scheduleDao.upsert(
            ScheduleEntity(
                title = title,
                description = description?.takeIf { it.isNotBlank() },
                due = due?.takeIf { it > 0 },
                groupId = groupId
            )
        )
    }

    fun updateSchedule(id: Long, title: String, description: String?, due: Long?, groupId: Long?) = viewModelScope.launch {
        _schedules.first().find { it.id == id }?.let {
            scheduleDao.upsert(it.copy(title = title, description = description, due = due, groupId = groupId))
        }
    }

    fun checkSchedule(id: Long, checked: Boolean) = viewModelScope.launch {
        _schedules.first().find { it.id == id }?.let {
            scheduleDao.upsert(it.copy(isCompleted = checked))
        }
    }

    fun deleteSchedule(id: Long) = viewModelScope.launch {
        scheduleDao.deleteById(id)
    }

    companion object {
        fun Factory(container: AppContainer) = viewModelFactory {
            initializer {
                ScheduleViewModel(container.scheduleDao)
            }
        }
    }
}