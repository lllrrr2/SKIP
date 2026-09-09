package com.android.skip.ui.settings.log

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.android.skip.R
import com.android.skip.ui.components.FlatButton
import com.android.skip.ui.components.ResourceIcon
import com.android.skip.ui.components.RowContent

@Composable
fun SkipLogButton(skipLogViewModel: SkipLogViewModel, onClick: () -> Unit) {
    val enable = skipLogViewModel.enable.observeAsState(false)

    FlatButton(content = {
        RowContent(
            title = R.string.settings_skip_log,
            subTitle = R.string.settings_skip_log_subtitle,
            icon = { ResourceIcon(iconResource = R.drawable.article) },
            checked = enable.value,
            onCheckedChange = {
                skipLogViewModel.changeEnable(it)
            }
        )
    }, onClick = onClick)
}