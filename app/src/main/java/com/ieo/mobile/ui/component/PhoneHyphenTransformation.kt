package com.ieo.mobile.ui.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.ieo.mobile.domain.auth.PhoneFormat

/** 휴대폰 번호 입력: 숫자에 하이픈을 그려 준다 (010-1234-5678). 값에는 숫자만 남는다. 가입과 내 정보 수정에서 쓴다 */
object PhoneHyphenTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = PhoneFormat.hyphen(digits)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var seen = 0
                formatted.forEachIndexed { index, c ->
                    if (seen == offset) return index
                    if (c != '-') seen++
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset).count { it != '-' }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
