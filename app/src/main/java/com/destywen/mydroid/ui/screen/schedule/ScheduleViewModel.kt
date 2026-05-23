package com.destywen.mydroid.ui.screen.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.MyApplication
import com.destywen.mydroid.data.local.ScheduleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduleScreenState(
    val schedules: List<ScheduleEntity> = emptyList(),
    val status: String? = null,
)

class ScheduleViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val scheduleDao = (application as MyApplication).database.scheduleDao()
    private val _schedules = scheduleDao.getAll()
    private val _status = MutableStateFlow<String?>(null)

    val state: StateFlow<ScheduleScreenState> =
        combine(_schedules, _status) { items, status ->
            ScheduleScreenState(
                schedules = items.sortedNaturally(),
                status = status
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleScreenState())

    fun addSchedule(title: String, description: String?, due: Long?, groupName: String?) =
        viewModelScope.launch {
            scheduleDao.upsert(
                ScheduleEntity(
                    title = title,
                    description = description?.takeIf { it.isNotBlank() },
                    due = due?.takeIf { it > 0 },
                    groupName = groupName?.takeIf { it.isNotBlank() }
                )
            )
        }

    fun updateSchedule(id: Long, title: String, description: String?, due: Long?, groupName: String?) =
        viewModelScope.launch {
            state.value.schedules.find { it.id == id }?.let {
                scheduleDao.upsert(
                    it.copy(
                        title = title,
                        description = description?.takeIf { it.isNotBlank() },
                        due = due?.takeIf { it > 0 },
                        groupName = groupName?.takeIf { it.isNotBlank() }
                    )
                )
            }
        }

    fun checkSchedule(id: Long, checked: Boolean) = viewModelScope.launch {
        state.value.schedules.find { it.id == id }?.let {
            scheduleDao.upsert(it.copy(isCompleted = checked))
        }
    }

    fun deleteSchedule(id: Long) = viewModelScope.launch {
        scheduleDao.deleteById(id)
    }

    companion object {
        fun Factory(app: Application) = viewModelFactory {
            initializer {
                ScheduleViewModel(app)
            }
        }
    }
}

fun List<ScheduleEntity>.sortedNaturally(): List<ScheduleEntity> {

    data class ItemWithGroup(val schedule: ScheduleEntity, val groupKey: String)

    val items = this.map { schedule ->
        val groupKey = schedule.groupName ?: "singleton_${schedule.id}"
        ItemWithGroup(schedule, groupKey)
    }

    val groups = items.groupBy { it.groupKey }

    data class GroupInfo(
        val groupKey: String,
        val schedules: List<ScheduleEntity>,
        val category: Int, // 0=not complete, has due, 1=not complete, no due, 2=all complete
        val minDue: Long?,
    )

    val groupInfos = groups.map { (key, groupItems) ->
        val schedules = groupItems.map { it.schedule } // all schedules in one group
        val uncompleted = schedules.filter { !it.isCompleted }
        val uncompletedWithDue = uncompleted.filter { it.due != null }

        val category = when {
            uncompletedWithDue.isNotEmpty() -> 0
            uncompleted.any { it.due == null } -> 1
            else -> 2
        }
        val minDue = if (category == 0) uncompletedWithDue.minOf { it.due!! } else null
        GroupInfo(key, schedules, category, minDue)
    }

    val sortedGroups = groupInfos.sortedWith(
        compareBy<GroupInfo> { it.category }
            .thenBy { it.minDue ?: Long.MAX_VALUE }
            .thenBy { it.groupKey }
    )

    return sortedGroups.flatMap { group ->
        group.schedules.sortedWith(
            compareBy<ScheduleEntity> { it.isCompleted }
                .thenByDescending { it.due != null }
                .thenBy { it.due ?: Long.MAX_VALUE }
                .thenBy { it.createdAt }
        )
    }
}