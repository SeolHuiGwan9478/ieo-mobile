package com.ieo.mobile.ui.document

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.PhotoLibrary
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.TonalButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.ieo.mobile.ui.component.LineGray
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange100
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.data.proof.PaperPhotoReader
import com.ieo.mobile.domain.document.BusinessInfo
import com.ieo.mobile.domain.document.DocumentItem
import com.ieo.mobile.domain.document.DocumentStatus
import com.ieo.mobile.domain.document.DriverDocuments
import com.ieo.mobile.domain.document.DriverRecord
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.FormField
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.component.ruled
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import java.io.File
import kotlinx.coroutines.launch

private fun newCaptureUri(context: Context): Uri {
    val folder = File(context.cacheDir, "documents").apply { mkdirs() }
    return FileProvider.getUriForFile(context, "${context.packageName}.files", File(folder, "document.jpg"))
}

/** 상태 한 단어와 색 */
@Composable
internal fun statusText(item: DocumentItem): Pair<String, Color> = when (item.status) {
    null -> "등록 전" to TextSub
    DocumentStatus.PENDING -> "심사 중" to Orange600
    DocumentStatus.APPROVED -> "승인" to Orange700
    DocumentStatus.REJECTED -> "반려 · 다시 올려 주세요" to Orange700
}

/** 홈 카드와 내 정보에 쓰는 한 줄 요약 */
fun documentsSummary(documents: DriverDocuments): String = when {
    documents.verified -> "인증됨"
    documents.rejectedCount > 0 -> "반려 ${documents.rejectedCount}건"
    documents.missingCount > 0 -> "${documents.missingCount}가지 남음"
    else -> "심사 중"
}

/** 내 정보 > 서류. 차주가 필수 서류 4종을 찍어 올린다. 서류를 누르면 그 서류만 올리는 페이지로 간다 (창을 띄우지 않는다) */
@Composable
fun DocumentsScreen(viewModel: DocumentsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editing = state.documents?.items?.firstOrNull { it.type == state.editing }
    if (editing != null) {
        UploadPage(editing, state, viewModel)
        return
    }
    BackHandler(onBack = viewModel::close)
    fun start(item: DocumentItem) {
        if (item.canSubmit && state.uploading == null) viewModel.edit(item)
    }

    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        TopBar("서류", viewModel::close)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            val documents = state.documents
            when {
                documents == null && state.loadFailed -> ErrorNotice(state.error ?: "서류를 불러오지 못했어요", viewModel::refresh)
                documents == null -> SkeletonCards(4)
                else -> {
                    Hero(documents)
                    Spacer(Modifier.height(22.dp))
                    Progress(documents)
                    Spacer(Modifier.height(16.dp))
                    state.message?.let { Text(it, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, color = TextMid, modifier = Modifier.padding(bottom = 10.dp)) }
                    // 필수 서류는 그림이 있는 네모 칸 2열 (차종 고르기와 같은 모양)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        documents.required.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { item -> DocumentTile(item, state.uploading == item.type, Modifier.weight(1f)) { start(item) } }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    documents.required.filter { it.status == DocumentStatus.REJECTED }.forEach { RejectNotice(it) { start(it) } }
                    if (documents.optional.isNotEmpty()) {
                        Text("선택 · 해당하면 올려 주세요", fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(top = 26.dp, bottom = 2.dp))
                        documents.optional.forEach { DocumentRow(it, state.uploading == it.type) { start(it) } }
                    }
                    Text(
                        "서류 사진은 심사하는 운영자만 보고, 탈퇴하면 지워요.",
                        fontSize = 14.sp, lineHeight = 20.sp, color = TextSub, modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
    }
}

/**
 * 서류 하나 올리기 페이지: (사업자등록증이면 번호·개업일) → 사진 찍기 또는 앨범에서 고르기 → 크게 확인 → 올리기.
 * 창 없이 한 화면에서 끝낸다
 */
@Composable
private fun UploadPage(item: DocumentItem, state: DocumentsState, viewModel: DocumentsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reader = remember(context) { PaperPhotoReader(context) }
    var photo by remember(item.type) { mutableStateOf<ByteArray?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    var number by remember(item.type) { mutableStateOf("") }
    var opened by remember(item.type) { mutableStateOf("") }
    val openedOn = BusinessFields.openedOn(opened)
    val businessReady = !item.needsBusinessInfo || (openedOn != null && (number.isEmpty() || number.length == 10))
    val uploading = state.uploading == item.type
    BackHandler(onBack = viewModel::stopEditing)

    fun readPhoto(uri: Uri) {
        scope.launch {
            val bytes = reader.read(uri)
            if (bytes == null) viewModel.photoUnreadable() else photo = bytes
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
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) launchCamera() }
    val album = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::readPhoto) }
    val pickAlbum = { album.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val takePhoto = { permission.launch(Manifest.permission.CAMERA) }

    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding().imePadding()) {
        TopBar(item.label, viewModel::stopEditing)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(Orange50, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(iconOf(item.type), contentDescription = null, tint = Orange600, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text("${objectOf(item.label)}\n올려 주세요", fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            }
            Text("네 모서리가 다 나오고 글씨가 또렷하게 보이게 찍어 주세요.", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid, modifier = Modifier.padding(top = 12.dp))
            item.latest?.rejectMessage?.takeIf { item.status == DocumentStatus.REJECTED }?.let {
                Column(Modifier.fillMaxWidth().padding(top = 14.dp).background(Orange50, RoundedCornerShape(12.dp)).padding(14.dp)) {
                    Text("지난번 반려 사유", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Orange700)
                    Text(it, fontSize = 15.sp, lineHeight = 21.sp, color = TextMid, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (item.needsBusinessInfo) {
                Text("등록증에 적힌 대로 넣어 주세요. 국세청에 사업자 상태를 확인해요", fontSize = 15.sp, lineHeight = 21.sp, color = TextSub, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
                FormField(number, { number = it.filter(Char::isDigit).take(10) }, "사업자등록번호 (비우면 내 정보의 번호)", KeyboardType.Number, placeholder = "123-45-67890", visualTransformation = HyphenAt(3, 5))
                Spacer(Modifier.height(10.dp))
                FormField(opened, { opened = it.filter(Char::isDigit).take(8) }, "개업연월일", KeyboardType.Number, placeholder = "2020-01-31", visualTransformation = HyphenAt(4, 6))
            }
            Spacer(Modifier.height(22.dp))
            PhotoBox(photo)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TonalButton(if (photo == null) "사진 찍기" else "다시 찍기", takePhoto, modifier = Modifier.weight(1f), icon = Icons.Outlined.PhotoCamera)
                TonalButton("앨범 선택", pickAlbum, modifier = Modifier.weight(1f), icon = Icons.Outlined.PhotoLibrary)
            }
            state.error?.let { Box(Modifier.padding(top = 12.dp)) { ErrorNotice(it) } }
            Spacer(Modifier.height(20.dp))
        }
        Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryButton(
                "올리기",
                { photo?.let { bytes -> viewModel.submit(item, bytes, if (item.needsBusinessInfo) BusinessInfo(number.takeIf { it.length == 10 }, openedOn!!) else null) } },
                enabled = photo != null && businessReady,
                busy = uploading,
            )
        }
    }
}

/** 고른 사진을 크게. 아직 없으면 점선 자리 */
@Composable
private fun PhotoBox(photo: ByteArray?) {
    val shape = RoundedCornerShape(14.dp)
    if (photo == null) {
        Column(
            Modifier.fillMaxWidth().height(200.dp).background(CanvasGray.copy(alpha = 0.5f), shape).border(1.dp, LineGray, shape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = TextSub, modifier = Modifier.size(40.dp))
            Text("사진을 찍거나 앨범에서 골라 주세요", fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(top = 8.dp))
        }
    } else {
        val bitmap = remember(photo) { BitmapFactory.decodeByteArray(photo, 0, photo.size)?.asImageBitmap() }
        bitmap?.let {
            Image(it, contentDescription = "올릴 서류 사진", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).border(1.dp, LineGray, shape).padding(4.dp))
        }
    }
}

/** 필수 서류 칸 수만큼 나눈 막대와 "승인 2 / 4" */
@Composable
private fun Progress(documents: DriverDocuments) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            documents.required.forEach { item ->
                val color = when (item.status) {
                    DocumentStatus.APPROVED -> Orange600
                    DocumentStatus.PENDING -> Orange100
                    DocumentStatus.REJECTED -> Orange100
                    null -> CanvasGray
                }
                Box(Modifier.weight(1f).height(6.dp).background(color, RoundedCornerShape(3.dp)))
            }
        }
        Text("승인 ${documents.approvedRequired} / ${documents.requiredTotal}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMid)
    }
}

/** 서류 종류마다의 그림 */
internal fun iconOf(type: String): ImageVector = when (type) {
    "BUSINESS_REGISTRATION" -> Icons.Outlined.Storefront
    "VEHICLE_REGISTRATION" -> Icons.Outlined.LocalShipping
    "DRIVER_LICENSE" -> Icons.Outlined.Badge
    "FREIGHT_QUALIFICATION" -> Icons.Outlined.WorkspacePremium
    "CONSIGNMENT_CONTRACT" -> Icons.Outlined.Handshake
    "CARGO_INSURANCE" -> Icons.Outlined.Shield
    else -> Icons.Outlined.Description
}

/** 맨 위: 큰 방패 그림과 한 줄 안내 (당근 동네 인증처럼 무엇을 하는 화면인지 그림으로) */
@Composable
private fun Hero(documents: DriverDocuments) {
    val done = documents.verified
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(84.dp).background(if (done) Orange100 else Orange50, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (done) Icons.Outlined.VerifiedUser else Icons.Outlined.GppMaybe, contentDescription = null, tint = if (done) Orange700 else Orange600, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(if (done) "서류 확인이 끝났어요" else "서류를 확인하면\n배차를 받을 수 있어요", fontSize = 23.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = TextStrong, textAlign = TextAlign.Center)
        Text(
            if (done) "오더에 바로 배차를 요청할 수 있어요" else "사진 ${documents.requiredTotal}장만 찍어 올려 주세요.\n운행 안내와 장부는 지금도 쓸 수 있어요",
            fontSize = 16.sp, lineHeight = 23.sp, color = TextMid, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** 필수 서류 한 칸: 그림, 이름, 상태. 상태에 따라 테두리와 바탕이 바뀐다 */
@Composable
private fun DocumentTile(item: DocumentItem, uploading: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val (label, color) = statusText(item)
    val shape = RoundedCornerShape(14.dp)
    // 주황 한 가지로 단계가 갈수록 진하게: 등록 전(흰색) → 심사 중(옅은 주황) → 승인(진한 주황). 반려는 테두리로 눈에 띄게
    val (border, fill) = when (item.status) {
        null -> LineGray to White
        DocumentStatus.PENDING -> Orange100 to Orange50
        DocumentStatus.REJECTED -> Orange700 to White
        DocumentStatus.APPROVED -> Orange600 to Orange100
    }
    Column(
        modifier
            .height(156.dp)
            .background(fill, shape)
            .border(if (item.status == DocumentStatus.APPROVED || item.status == DocumentStatus.REJECTED) 1.5.dp else 1.dp, border, shape)
            .clickable(enabled = item.canSubmit, role = Role.Button, onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp).background(if (item.status == null || item.status == DocumentStatus.REJECTED) CanvasGray else White, CircleShape), contentAlignment = Alignment.Center) {
                Icon(iconOf(item.type), contentDescription = null, tint = if (item.status == DocumentStatus.APPROVED) Orange700 else TextMid, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.weight(1f))
            when {
                uploading -> CircularProgressIndicator(strokeWidth = 2.dp, color = Orange600, modifier = Modifier.size(22.dp))
                else -> Icon(statusIcon(item.status), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(item.label, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, color = TextStrong, maxLines = 2)
        Text(if (item.status == null) "눌러서 찍기" else label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (item.status == null) TextSub else color, modifier = Modifier.padding(top = 2.dp))
    }
}

private fun statusIcon(status: DocumentStatus?): ImageVector = when (status) {
    null -> Icons.Outlined.PhotoCamera
    DocumentStatus.PENDING -> Icons.Outlined.HourglassTop
    DocumentStatus.APPROVED -> Icons.Filled.CheckCircle
    DocumentStatus.REJECTED -> Icons.Outlined.ErrorOutline
}

/** 반려된 서류: 사유와 다시 올리기. 칸에는 다 못 쓰니 아래에 따로 */
@Composable
private fun RejectNotice(item: DocumentItem, onRetry: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp).background(Orange50, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("${item.label} 반려", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Orange700)
            item.latest?.rejectMessage?.let { Text(it, fontSize = 15.sp, lineHeight = 21.sp, color = TextMid, modifier = Modifier.padding(top = 2.dp)) }
        }
        TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("다시 올리기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Orange700) }
    }
}

/** 선택 서류 한 줄 */
@Composable
private fun DocumentRow(item: DocumentItem, uploading: Boolean, onClick: () -> Unit) {
    val (label, color) = statusText(item)
    Row(
        Modifier.fillMaxWidth().ruled().clickable(enabled = item.canSubmit, onClick = onClick).heightIn(min = 64.dp).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(iconOf(item.type), contentDescription = null, tint = TextMid, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextStrong)
            if (item.status != null) Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
        when {
            uploading -> CircularProgressIndicator(strokeWidth = 2.dp, color = Orange600, modifier = Modifier.size(22.dp))
            item.status == DocumentStatus.PENDING -> Unit
            else -> Text(if (item.status == null) "올리기" else "다시 올리기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Orange700)
        }
    }
}

/** 숫자만 받아 하이픈을 붙여 보여 준다 */
internal object BusinessFields {
    fun formatNumber(digits: String): String = when {
        digits.length <= 3 -> digits
        digits.length <= 5 -> "${digits.take(3)}-${digits.drop(3)}"
        else -> "${digits.take(3)}-${digits.substring(3, 5)}-${digits.drop(5)}"
    }

    fun formatDate(digits: String): String = when {
        digits.length <= 4 -> digits
        digits.length <= 6 -> "${digits.take(4)}-${digits.drop(4)}"
        else -> "${digits.take(4)}-${digits.substring(4, 6)}-${digits.drop(6)}"
    }

    /** 8자리가 실제 날짜면 2020-01-31 모양으로, 아니면 null */
    fun openedOn(digits: String): String? {
        if (digits.length != 8) return null
        val text = formatDate(digits)
        return runCatching { java.time.LocalDate.parse(text) }.getOrNull()?.takeIf { !it.isAfter(java.time.LocalDate.now()) }?.toString()
    }
}

/**
 * 숫자만 저장하고 화면에만 하이픈을 그린다. 값에 하이픈을 넣으면 커서가 밀려 숫자 순서가 뒤섞인다.
 * [positions]: 이 자리 숫자 앞에 하이픈 (사업자번호 3, 5 → 123-45-67890). 오름차순
 */
internal class HyphenAt(private vararg val positions: Int) : androidx.compose.ui.text.input.VisualTransformation {

    /** 숫자 [length]개일 때 실제로 그려지는 하이픈 자리(숫자 기준) */
    private fun present(length: Int) = positions.filter { it in 1 until length }

    fun format(digits: String): String = buildString {
        val shown = present(digits.length)
        digits.forEachIndexed { i, c ->
            if (i in shown) append('-')
            append(c)
        }
    }

    fun toTransformed(offset: Int, length: Int): Int = offset + present(length).count { it <= offset }

    fun toOriginal(offset: Int, length: Int): Int {
        val hyphens = present(length).mapIndexed { j, p -> p + j }
        return (offset - hyphens.count { it < offset }).coerceIn(0, length)
    }

    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val length = text.length
        val mapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int) = toTransformed(offset, length)
            override fun transformedToOriginal(offset: Int) = toOriginal(offset, length)
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(format(text.text)), mapping)
    }
}

/** 서류 심사를 통과한 차주 표시 (당근의 동네 인증처럼). 이름 옆에 붙인다 */
@Composable
fun VerifiedBadge(modifier: Modifier = Modifier) {
    Row(
        modifier.background(Orange50, RoundedCornerShape(50)).padding(start = 6.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Verified, contentDescription = null, tint = Orange600, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("인증 차주", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Orange700)
    }
}

/** 주선사에게 보이는 내 이력 한 줄 */
fun recordSummary(record: DriverRecord): String =
    "완료 운행 ${record.completedTripCount}건 · 상차 직전 취소 ${record.lateCancelCount}건"
