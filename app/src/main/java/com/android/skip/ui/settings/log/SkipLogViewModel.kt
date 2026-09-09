package com.android.skip.ui.settings.log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.skip.R
import com.android.skip.util.DataStoreUtils
import com.blankj.utilcode.util.StringUtils.getString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SkipLogViewModel @Inject constructor(
    private val skipLogRepository: SkipLogRepository
) : ViewModel() {
    val enable: LiveData<Boolean> = skipLogRepository.enable

    private val _dailyLogs = MutableLiveData<List<SkipLogDay>>(emptyList())
    val dailyLogs: LiveData<List<SkipLogDay>> = _dailyLogs

    init {
        loadDailyLogs()
    }

    fun changeEnable(enable: Boolean) {
        skipLogRepository.changeEnable(enable)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                DataStoreUtils.putData(getString(R.string.store_skip_log), enable)
            }
        }
    }

    fun loadDailyLogs() {
        viewModelScope.launch {
            val logs = withContext(Dispatchers.IO) {
                skipLogRepository.readDailyLogs()
            }
            _dailyLogs.postValue(logs)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                skipLogRepository.clearAllLogs()
            }
            _dailyLogs.postValue(emptyList())
        }
    }
}