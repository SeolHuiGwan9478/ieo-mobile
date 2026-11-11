package com.ieo.mobile.ui.proof

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.domain.proof.WaitRecord
import com.ieo.mobile.domain.proof.WaitTimeInput
import com.ieo.mobile.domain.proof.WaitTimes
import com.ieo.mobile.ui.common.DayTimeFormatter
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import java.time.Instant

/**
 * 대기 시각 고치기 (WAIT-006). 도착·작업 시작·출발을 "14:05"처럼 고친다. 앞 시각보다 이르면 다음 날로 본다.
 * 고치면 주선사가 보는 증빙에 "수정됨"과 원래 시각이 함께 남는다
 */
@Composable
fun EditWaitDialog(record: WaitRecord, labels: DayTimeFormatter, onSave: (WaitTimes) -> Unit, onDismiss: () -> Unit) {
    var arrived by remember { mutableStateOf(labels.time(record.arrivedAt)) }
    var work by remember { mutableStateOf(record.workStartedAt?.let(labels::time).orEmpty()) }
    var depart by remember { mutableStateOf(record.departedAt?.let(labels::time).orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("대기 시각 고치기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("고치면 주선사가 보는 증빙에 '수정됨'과 원래 시각이 함께 남아요.", fontSize = 15.sp, lineHeight = 22.sp, color = TextMid)
                TimeField("도착", arrived) { arrived = it; error = null }
                TimeField("작업 시작 (없으면 비워 두기)", work) { work = it; error = null }
                TimeField("출발 (없으면 비워 두기)", depart) { depart = it; error = null }
                error?.let { Text(it, fontSize = 15.sp, color = IeoStatus.colors.warning) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                WaitTimeInput.resolve(record.arrivedAt, arrived, work, depart, Instant.now())
                    .onSuccess(onSave)
                    .onFailure { error = it.message }
            }) { Text("고치기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", fontSize = 16.sp, color = TextMid) } },
    )
}

@Composable
private fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(5)) },
        label = { Text(label) },
        placeholder = { Text("14:05") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
