package com.ieo.mobile.ui.proof

import com.ieo.mobile.ui.component.ruled
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.CheckCircle
import com.ieo.mobile.ui.component.StepTimeline
import com.ieo.mobile.ui.component.IconBadge
import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.data.proof.PaperPhotoReader
import com.ieo.mobile.domain.proof.DeliveryProof
import com.ieo.mobile.domain.proof.WaitRecord
import com.ieo.mobile.domain.trip.StopType
import com.ieo.mobile.ui.common.DayTimeFormatter
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.QuietButton
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.launch

private fun duration(minutes: Long): String = when {
    minutes < 60 -> "${minutes}분"
    minutes % 60 == 0L -> "${minutes / 60}시간"
    else -> "${minutes / 60}시간 ${minutes % 60}분"
}

/** 대기 카드의 큰 한 줄 */
fun waitHeadline(record: WaitRecord, now: Instant = Instant.now()): String = when {
    record.isWaiting -> "지금까지 ${duration(Duration.between(record.arrivedAt, now).toMinutes().coerceAtLeast(0))} 기다렸어요"
    record.waitMinutes != null -> "${duration(record.waitMinutes)} 기다렸어요"
    else -> "대기 시간을 나누지 못했어요"
}

/** 얼마를 청구할 수 있는지, 왜 금액이 없는지를 말로 */
fun waitChargeText(record: WaitRecord): String = when {
    record.isUndivided -> "작업 시작 기록이 없어 대기비를 계산하지 않았어요"
    !record.hasPolicy -> "이 주선사는 대기비 기준이 없어 시간만 기록했어요"
    record.chargeableAmount == null -> "작업이 시작되면 대기비를 계산해요"
    record.chargeableAmount == 0L -> "무료 대기 시간 안이라 청구할 금액은 없어요"
    else -> "청구할 수 있는 대기비 ${formatWon(record.chargeableAmount)}"
}

private fun newCaptureUri(context: Context): Uri {
    val folder = File(context.cacheDir, "proofs").apply { mkdirs() }
    return FileProvider.getUriForFile(context, "${context.packageName}.files", File(folder, "paper.jpg"))
}

/** 배차 상세에서 여는 증빙 화면 */
@Composable
fun ProofScreen(viewModel: ProofViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reader = remember(context) { PaperPhotoReader(context) }
    val labels = remember { DayTimeFormatter() }
    var choosing by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WaitRecord?>(null) }
    var preview by remember { mutableStateOf<ByteArray?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    val close = {
        viewModel.close()
        onClose()
    }
    BackHandler(onBack = close)

    LaunchedEffect(state.share) {
        state.share?.let { text ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            runCatching { context.startActivity(Intent.createChooser(send, "보낼 곳을 골라 주세요")) }
            viewModel.shareConsumed()
        }
    }

    fun readPhoto(uri: Uri) {
        scope.launch {
            val bytes = reader.read(uri)
            if (bytes == null) viewModel.photoUnreadable() else preview = bytes
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = captureUri
        if (taken && uri != null) readPhoto(uri)
    }
    fun launchCamera() {
        val uri = newCaptureUri(context)
        captureUri = uri
        runCatching { camera.launch(uri) }.onFailure { viewModel.photoUnreadable() }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }
    val album = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::readPhoto) }

    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = close) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("증빙", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            when {
                state.isLoading -> SkeletonCards(2)
                state.loadFailed -> ErrorNotice(state.error.orEmpty(), viewModel::retry)
                else -> {
                    if (state.error != null) ErrorNotice(state.error.orEmpty())
                    state.message?.let { Text(it, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IeoStatus.colors.done, modifier = Modifier.padding(horizontal = 4.dp)) }
                    Heading("대기")
                    if (state.waits.isEmpty()) {
                        Card { Text("상하차지에 도착하면 차량 앱이 자동으로 기록해요", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid) }
                    }
                    state.waits.forEach { record ->
                        WaitCard(record, labels, onEdit = { editing = record })
                    }
                    if (state.waits.isNotEmpty()) {
                        // 운행 하나에 링크 하나: 상차지·하차지 기록을 함께 보낸다 (WAIT-020)
                        PrimaryButton(
                            "대기 증빙 보내기",
                            viewModel::shareWaitProof,
                            busy = state.busy == ProofAction.WaitLink,
                            icon = Icons.Outlined.Share,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Heading("인수증")
                    DeliveryCard(state, labels, viewModel, onPhoto = { choosing = true })
                }
            }
        }
    }

    editing?.let { record ->
        EditWaitDialog(
            record = record,
            labels = labels,
            onSave = { times ->
                editing = null
                viewModel.editWait(record, times)
            },
            onDismiss = { editing = null },
        )
    }

    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text("종이 인수증 사진") },
            text = { Text("글씨와 도장이 잘 보이게 올려 주세요.", fontSize = 16.sp, color = TextMid) },
            confirmButton = {
                TextButton(onClick = { choosing = false; permission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("사진 찍기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    choosing = false
                    album.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text("앨범에서 고르기", fontSize = 16.sp, color = TextMid) }
            },
        )
    }

    preview?.let { bytes ->
        val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("이 사진을 올릴까요?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    bitmap?.let { Image(it, contentDescription = "올릴 종이 인수증 사진", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)) }
                    Text("올리면 인수가 끝난 것으로 처리되고 되돌릴 수 없어요.", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
                }
            },
            confirmButton = {
                TextButton(onClick = { preview = null; viewModel.uploadPaper(bytes) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("올리기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                }
            },
            dismissButton = {
                TextButton(onClick = { preview = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("취소", fontSize = 16.sp, color = TextMid) }
            },
        )
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().ruled().padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun WaitCard(record: WaitRecord, labels: DayTimeFormatter, onEdit: () -> Unit) {
    Card {
        val stop = if (record.stopType == StopType.PICKUP) "상차" else "하차"
        Text("$stop · ${record.siteName}" + if (record.edited) " · 수정됨" else "", fontSize = 16.sp, color = TextSub)
        Text(waitHeadline(record), fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        val amount = (record.chargeableAmount ?: 0) > 0
        Text(waitChargeText(record), fontSize = 16.sp, lineHeight = 23.sp, fontWeight = if (amount) FontWeight.Bold else FontWeight.Normal, color = if (amount) IeoStatus.colors.done else TextMid)
        StepTimeline(listOf("도착" to labels.time(record.arrivedAt), "작업" to record.workStartedAt?.let(labels::time), "출발" to record.departedAt?.let(labels::time)))
        QuietButton("시각 고치기", onEdit)
    }
}

@Composable
private fun DoneTitle() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IconBadge(Icons.Outlined.CheckCircle, tint = IeoStatus.colors.done, background = androidx.compose.ui.graphics.Color(0xFFE6F4EA), size = 38.dp)
        Text("인수 완료", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = IeoStatus.colors.done)
    }
}

@Composable
private fun DeliveryCard(state: ProofState, labels: DayTimeFormatter, viewModel: ProofViewModel, onPhoto: () -> Unit) {
    val delivery: DeliveryProof? = state.delivery
    Card {
        when {
            delivery?.isSigned == true -> {
                DoneTitle()
                Text("${delivery.signerName ?: "수하인"}님이 ${labels.dayTime(delivery.signedAt!!)}에 서명했어요", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
                delivery.issueNote?.takeIf { it.isNotBlank() }?.let {
                    Text("남긴 이상 사항: $it", fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold, color = IeoStatus.colors.warning)
                }
            }
            delivery?.isDone == true -> {
                DoneTitle()
                Text("종이 인수증 사진 ${delivery.photoCount}장을 올렸어요", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
            }
            else -> {
                Text("아직 서명 전이에요", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                Text("수하인이 링크를 열어 앱 없이 서명해요", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
                PrimaryButton("수하인에게 서명 요청", viewModel::shareDeliveryLink, busy = state.busy == ProofAction.DeliveryLink, icon = Icons.Outlined.Draw, modifier = Modifier.padding(top = 6.dp))
                if (delivery != null) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { QuietButton("서명 확인", viewModel::checkSigned) }
                }
            }
        }
        if (!state.paperAllowed && delivery?.isDone != true) {
            Text("종이 인수증 사진은 하차지에 도착한 뒤 올릴 수 있어요", fontSize = 16.sp, lineHeight = 23.sp, color = TextSub)
        } else if (delivery == null || delivery.canAddPhoto) {
            TonalButton(
                if (delivery?.isDone == true) "사진 추가" else "종이 인수증 찍기",
                onPhoto,
                enabled = state.busy == null,
                icon = Icons.Outlined.PhotoCamera,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
