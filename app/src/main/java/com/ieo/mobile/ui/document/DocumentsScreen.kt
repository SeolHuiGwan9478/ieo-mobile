package com.ieo.mobile.ui.document

import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import com.ieo.mobile.R
import androidx.compose.ui.res.painterResource
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.outlined.PhotoLibrary
import com.ieo.mobile.ui.component.PrimaryButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import kotlinx.coroutines.launch

/** 상태 한 단어와 색 */
@Composable
internal fun statusText(item: DocumentItem): Pair<String, Color> = when (item.status) {
    null -> "올려 주세요" to TextSub
    DocumentStatus.PENDING -> "확인하고 있어요" to Orange600
    DocumentStatus.APPROVED -> "확인 완료" to Orange700
    DocumentStatus.REJECTED -> "반려 · 눌러서 사유 보기" to Orange700
}

/** 홈 카드와 내 정보에 쓰는 한 줄 요약 */
fun documentsSummary(documents: DriverDocuments): String = when {
    documents.verified -> "인증됨"
    documents.rejectedCount > 0 -> "반려 ${documents.rejectedCount}건"
    documents.missingCount > 0 -> "${documents.missingCount}가지 남음"
    else -> "확인 중"
}

/** 서류 종류마다의 색 그림 (내 정보 메뉴와 같은 그림체) */
internal fun iconOf(type: String): Int = when (type) {
    "BUSINESS_REGISTRATION" -> R.drawable.ic_menu_business
    "VEHICLE_REGISTRATION" -> R.drawable.ic_doc_vehicle
    "DRIVER_LICENSE" -> R.drawable.ic_doc_license
    "FREIGHT_QUALIFICATION" -> R.drawable.ic_doc_qualification
    "CONSIGNMENT_CONTRACT" -> R.drawable.ic_doc_contract
    "CARGO_INSURANCE" -> R.drawable.ic_doc_insurance
    else -> R.drawable.ic_menu_document
}

/** 내 정보 > 서류. 서류마다 칸을 누르면 그 서류 페이지로 간다 (창을 띄우지 않는다) */
@Composable
fun DocumentsScreen(viewModel: DocumentsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editing = state.documents?.items?.firstOrNull { it.type == state.editing }
    if (editing != null) {
        DocumentPage(editing, state, viewModel)
        return
    }
    BackHandler(onBack = viewModel::close)
    fun open(item: DocumentItem) {
        if (state.uploading == null) viewModel.edit(item)
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
                                pair.forEach { item -> DocumentTile(item, state.uploading == item.type, Modifier.weight(1f)) { open(item) } }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    if (documents.optional.isNotEmpty()) {
                        Text("해당하는 분만 올려 주세요", fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(top = 26.dp, bottom = 2.dp))
                        documents.optional.forEach { DocumentRow(it, state.uploading == it.type) { open(it) } }
                    }
                    Text(
                        "올린 서류는 확인하는 담당자만 볼 수 있고, 탈퇴하면 함께 지워져요.",
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

/** 서류 페이지 제목: 지금 상태에 맞춰 */
internal fun pageTitle(item: DocumentItem): String = when (item.status) {
    null -> "${objectOf(item.label)}\n올려 주세요"
    DocumentStatus.PENDING -> "${objectOf(item.label)}\n확인하고 있어요"
    DocumentStatus.APPROVED -> "${item.label}\n확인이 끝났어요"
    DocumentStatus.REJECTED -> "${objectOf(item.label)}\n다시 올려 주세요"
}

/**
 * 서류 하나의 페이지: 지금 상태, (사업자등록증이면 번호·개업일), 사진 고르기 → 크게 확인 → 올리기.
 * 사진은 갤러리에서만 고른다. 반려됐으면 사유를 맨 아래 버튼 바로 위에 보여 준다
 */
@Composable
private fun DocumentPage(item: DocumentItem, state: DocumentsState, viewModel: DocumentsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reader = remember(context) { PaperPhotoReader(context) }
    var photo by remember(item.type) { mutableStateOf<ByteArray?>(null) }
    var number by remember(item.type) { mutableStateOf("") }
    var opened by remember(item.type) { mutableStateOf("") }
    val openedOn = BusinessFields.openedOn(opened)
    val businessReady = !item.needsBusinessInfo || (openedOn != null && (number.isEmpty() || number.length == 10))
    val uploading = state.uploading == item.type
    BackHandler(onBack = viewModel::stopEditing)

    val album = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            scope.launch {
                val bytes = reader.read(it)
                if (bytes == null) viewModel.photoUnreadable() else photo = bytes
            }
        }
    }
    val pick = { album.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding().imePadding()) {
        TopBar(item.label, viewModel::stopEditing)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).background(CanvasGray, CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(iconOf(item.type)), contentDescription = null, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(pageTitle(item), fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            }
            when (item.status) {
                DocumentStatus.PENDING -> StatusNote("담당자가 서류를 보고 있어요. 결과가 나오면 홈과 이 화면에서 알려 드려요.")
                DocumentStatus.APPROVED -> StatusNote("새 서류로 바꿔야 할 때만 다시 올려 주세요.")
                else -> Text("서류 전체와 글씨가 잘 보이는 사진을 골라 주세요.", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid, modifier = Modifier.padding(top = 12.dp))
            }
            if (item.canSubmit) {
                if (item.needsBusinessInfo) {
                    Text("등록증에 적힌 그대로 넣어 주세요. 국세청에서 사업자 상태를 확인해요.", fontSize = 15.sp, lineHeight = 21.sp, color = TextSub, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
                    FormField(number, { number = it.filter(Char::isDigit).take(10) }, "사업자등록번호 (비우면 내 정보의 번호)", KeyboardType.Number, placeholder = "123-45-67890", visualTransformation = HyphenAt(3, 5))
                    Spacer(Modifier.height(10.dp))
                    FormField(opened, { opened = it.filter(Char::isDigit).take(8) }, "개업연월일", KeyboardType.Number, placeholder = "2020-01-31", visualTransformation = HyphenAt(4, 6))
                }
                Spacer(Modifier.height(22.dp))
                PhotoBox(photo, pick)
            }
            state.error?.let { Box(Modifier.padding(top = 12.dp)) { ErrorNotice(it) } }
            Spacer(Modifier.height(20.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            // 반려 사유는 서류마다 맨 아래, 다시 올리는 버튼 바로 위에
            item.latest?.rejectMessage?.takeIf { item.status == DocumentStatus.REJECTED }?.let {
                Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).background(Orange50, RoundedCornerShape(12.dp)).padding(14.dp)) {
                    Text("반려된 이유", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Orange700)
                    Text(it, fontSize = 15.sp, lineHeight = 21.sp, color = TextMid, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (item.canSubmit) {
                PrimaryButton(
                    if (item.status == null) "올리기" else "다시 올리기",
                    { photo?.let { bytes -> viewModel.submit(item, bytes, if (item.needsBusinessInfo) BusinessInfo(number.takeIf { it.length == 10 }, openedOn!!) else null) } },
                    enabled = photo != null && businessReady,
                    busy = uploading,
                )
            }
        }
    }
}

@Composable
private fun StatusNote(text: String) {
    Text(text, fontSize = 16.sp, lineHeight = 23.sp, color = TextMid, modifier = Modifier.padding(top = 12.dp).fillMaxWidth().background(CanvasGray.copy(alpha = 0.6f), RoundedCornerShape(12.dp)).padding(14.dp))
}

/** 고른 사진을 크게. 아직 없으면 눌러서 고르는 자리. 고른 뒤에도 눌러서 바꿀 수 있다 */
@Composable
private fun PhotoBox(photo: ByteArray?, onPick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    if (photo == null) {
        Column(
            Modifier.fillMaxWidth().height(220.dp).background(CanvasGray.copy(alpha = 0.5f), shape).border(1.dp, LineGray, shape).clickable(onClick = onPick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Orange600, modifier = Modifier.size(44.dp))
            Text("사진 선택", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 10.dp))
            Text("갤러리에서 서류 사진을 골라 주세요", fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(top = 2.dp))
        }
    } else {
        val bitmap = remember(photo) { BitmapFactory.decodeByteArray(photo, 0, photo.size)?.asImageBitmap() }
        bitmap?.let {
            Image(it, contentDescription = "올릴 서류 사진", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).border(1.dp, LineGray, shape).padding(4.dp))
        }
        TextButton(onClick = onPick, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = TextMid, modifier = Modifier.size(20.dp))
            Text("다른 사진 선택", fontSize = 15.sp, color = TextMid, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

/** 필수 서류 칸 수만큼 나눈 막대와 "확인 완료 2 / 4" */
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
        Text("확인 완료 ${documents.approvedRequired} / ${documents.requiredTotal}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMid)
    }
}

/** 맨 위: 서류 그림과 한 줄 안내 */
@Composable
private fun Hero(documents: DriverDocuments) {
    val done = documents.verified
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.ic_doc_hero), contentDescription = null, modifier = Modifier.size(84.dp))
        Spacer(Modifier.height(16.dp))
        Text(if (done) "서류 확인이 모두 끝났어요" else "서류 ${documents.requiredTotal}가지만 올리면\n배차를 받을 수 있어요", fontSize = 23.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = TextStrong, textAlign = TextAlign.Center)
        Text(
            if (done) "이제 오더에 배차를 요청할 수 있어요" else "갖고 계신 서류 사진을 올려 주세요.\n운행 안내와 장부는 지금도 사용할 수 있어요",
            fontSize = 16.sp, lineHeight = 23.sp, color = TextMid, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** 필수 서류 한 칸: 그림, 이름, 상태. 상태에 따라 테두리와 바탕이 바뀐다. 어떤 상태든 눌러서 서류 페이지로 */
@Composable
private fun DocumentTile(item: DocumentItem, uploading: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val (label, color) = statusText(item)
    val shape = RoundedCornerShape(14.dp)
    // 주황 한 가지로 단계가 갈수록 진하게: 올리기 전(흰색) → 확인 중(옅은 주황) → 확인 완료(진한 주황). 반려는 테두리로 눈에 띄게
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
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp).background(if (item.status == DocumentStatus.APPROVED || item.status == DocumentStatus.PENDING) White else CanvasGray, CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(iconOf(item.type)), contentDescription = null, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.weight(1f))
            when {
                uploading -> CircularProgressIndicator(strokeWidth = 2.dp, color = Orange600, modifier = Modifier.size(22.dp))
                else -> Icon(statusIcon(item.status), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(item.label, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, color = TextStrong, maxLines = 2)
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.padding(top = 2.dp))
    }
}

private fun statusIcon(status: DocumentStatus?): ImageVector = when (status) {
    null -> Icons.Outlined.AddPhotoAlternate
    DocumentStatus.PENDING -> Icons.Outlined.HourglassTop
    DocumentStatus.APPROVED -> Icons.Filled.CheckCircle
    DocumentStatus.REJECTED -> Icons.Outlined.ErrorOutline
}

/** 선택 서류 한 줄 */
@Composable
private fun DocumentRow(item: DocumentItem, uploading: Boolean, onClick: () -> Unit) {
    val (label, color) = statusText(item)
    Row(
        Modifier.fillMaxWidth().ruled().clickable(onClick = onClick).heightIn(min = 64.dp).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(iconOf(item.type)), contentDescription = null, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextStrong)
            if (item.status != null) Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
        if (uploading) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = Orange600, modifier = Modifier.size(22.dp))
        } else {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = TextSub)
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
