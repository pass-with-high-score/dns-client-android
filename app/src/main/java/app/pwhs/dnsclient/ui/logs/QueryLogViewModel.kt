package app.pwhs.dnsclient.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.dnsclient.data.local.dao.DnsQueryLogDao
import app.pwhs.dnsclient.data.local.entity.DnsQueryLogEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QueryLogViewModel(
    private val queryLogDao: DnsQueryLogDao
) : ViewModel() {

    val logs: StateFlow<List<DnsQueryLogEntity>> = queryLogDao.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = queryLogDao.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val avgLatency: StateFlow<Double?> = queryLogDao.getAverageLatency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun clearLogs() {
        viewModelScope.launch {
            queryLogDao.clearAll()
        }
    }
}
