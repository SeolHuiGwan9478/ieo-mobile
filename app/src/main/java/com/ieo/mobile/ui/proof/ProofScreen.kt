package com.ieo.mobile.ui.proof

import com.ieo.mobile.ui.component.ruled
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import com.ieo.mobile.domain.proof.ReceiptPhoto
import com.ieo.mobile.ui.component.ZoomableImage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var viewing by remember { mutableStateOf<ReceiptPhoto?>(null) }
    var confirmingSend by remember { mutableStateOf(false) }
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
                    // 결제 방식이 인수증인 운행만 종이 인수증을 받는다 (04 인수증)
                    state.delivery?.takeIf { it.required }?.let { delivery ->
                        Heading("인수증")
                        DeliveryCard(delivery, state, labels, onPhoto = { choosing = true }, onOpen = { viewing = it },
                            onSend = { confirmingSend = true })
                    }
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
                    Text("올린 사진은 주선사에게 보내기 전까지 지울 수 있어요.", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
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

    viewing?.let { photo ->
        val photos = state.delivery?.photos.orEmpty()
        val index = photos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
        val file = state.photoFiles[photo.id]
        val bitmap = remember(file) { file?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
        val sent = state.delivery?.isSent == true
        // 화면 전체로 보고, 두 손가락으로 벌리거나 두 번 눌러 확대한다 (보내기 전에 글씨·도장이 잘 나왔는지 본다)
        Dialog(onDismissRequest = { viewing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black).safeDrawingPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp)) {
                    Text(if (photos.size > 1) "종이 인수증 ${index + 1} / ${photos.size}" else "종이 인수증", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewing = null }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = White)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        ZoomableImage(bitmap, contentDescription = "종이 인수증 사진 ${index + 1}", modifier = Modifier.fillMaxSize())
                    } else {
                        Text("사진을 불러오지 못했어요.", fontSize = 16.sp, color = White)
                    }
                }
                Text("두 번 누르거나 두 손가락으로 벌리면 크게 보여요", fontSize = 14.sp, color = androidx.compose.ui.graphics.Color(0xFFBDBDBD),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    // 끝에서 다음은 처음으로 돌아간다
                    if (photos.size > 1) {
                        TonalButton("이전", { viewing = photos[(index - 1 + photos.size) % photos.size] }, modifier = Modifier.weight(1f))
                    }
                    if (!sent) {
                        TonalButton("지우기", { viewing = null; viewModel.deletePhoto(photo.id) }, modifier = Modifier.weight(1f))
                    }
                    if (photos.size > 1) {
                        TonalButton("다음", { viewing = photos[(index + 1) % photos.size] }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (confirmingSend) {
        val count = state.delivery?.photos?.size ?: 0
        AlertDialog(
            onDismissRequest = { confirmingSend = false },
            title = { Text("주선사에게 보낼까요?") },
            text = { Text("종이 인수증 사진 ${count}장을 보내요. 보낸 뒤에는 사진을 바꿀 수 없어요.", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid) },
            confirmButton = {
                TextButton(onClick = { confirmingSend = false; viewModel.sendReceipt() }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("보내기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingSend = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("취소", fontSize = 16.sp, color = TextMid) }
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
        QuietButton("시각 수정", onEdit)
    }
}

/** 종이 인수증 카드: 찍은 사진 목록, [찍기], [주선사에게 보내기]. 보낸 뒤에는 보낸 시각과 사진만 */
@Composable
private fun DeliveryCard(
    delivery: DeliveryProof,
    state: ProofState,
    labels: DayTimeFormatter,
    onPhoto: () -> Unit,
    onOpen: (ReceiptPhoto) -> Unit,
    onSend: () -> Unit,
) {
    Card {
        if (delivery.isSent) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(Icons.Outlined.CheckCircle, tint = IeoStatus.colors.done, background = androidx.compose.ui.graphics.Color(0xFFE6F4EA), size = 38.dp)
                Text("주선사에게 보냈어요", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = IeoStatus.colors.done)
            }
            Text("${labels.dayTime(delivery.sentAt!!)} · 사진 ${delivery.photos.size}장", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
        } else {
            Text(if (delivery.photos.isEmpty()) "종이 인수증을 찍어 보내요" else "사진 ${delivery.photos.size}장 · 아직 안 보냈어요",
                fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            Text(
                if (delivery.canUpload || delivery.photos.isNotEmpty()) "글씨와 도장이 잘 보이게 찍어 주세요. 여러 장이면 ${delivery.maxPhotos}장까지 올려요"
                else "하차지에 도착한 뒤 올릴 수 있어요",
                fontSize = 16.sp, lineHeight = 23.sp, color = TextMid,
            )
        }
        if (delivery.photos.isNotEmpty()) {
            // 3칸까지. 더 있으면 마지막 칸에 남은 장수를 덮어 쓴다
            val more = delivery.photos.size - THUMBS
            Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                delivery.photos.take(THUMBS).forEachIndexed { index, photo ->
                    PhotoThumb(photo, state.photoFiles[photo.id], more = if (index == THUMBS - 1 && more > 0) more + 1 else 0, onClick = { onOpen(photo) })
                }
            }
        }
        if (delivery.canUpload) {
            TonalButton(
                if (delivery.photos.isEmpty()) "종이 인수증 찍기" else "한 장 더 찍기",
                onPhoto,
                enabled = state.busy == null,
                icon = Icons.Outlined.PhotoCamera,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (delivery.canSend) {
            PrimaryButton("주선사에게 보내기", onSend, busy = state.busy == ProofAction.SendReceipt, icon = Icons.AutoMirrored.Outlined.Send)
        }
    }
}

private const val THUMBS = 3

/** [more]가 0보다 크면 사진 위에 "+N장"을 덮는다 */
@Composable
private fun PhotoThumb(photo: ReceiptPhoto, file: ByteArray?, more: Int, onClick: () -> Unit) {
    val bitmap = remember(file) { file?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    Box(
        Modifier.size(88.dp).clip(RoundedCornerShape(10.dp)).background(CanvasGray).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = "종이 인수증 사진 ${photo.id}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Outlined.Image, contentDescription = "종이 인수증 사진", tint = TextSub)
        }
        if (more > 0) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Text("+${more}장", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
            }
        }
    }
}
