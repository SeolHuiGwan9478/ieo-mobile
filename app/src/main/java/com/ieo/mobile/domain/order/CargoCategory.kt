package com.ieo.mobile.domain.order

/**
 * 화물 종류 (서버 CargoCategory, DSP-001). 주선사가 오더를 올릴 때 고른다.
 * 차주는 배차 추천에서 뺄 종류를 고를 수 있다 (REC-004)
 */
enum class CargoCategory(val label: String, val excludable: Boolean) {
    /** 일반을 빼면 추천이 거의 사라지므로 고르는 칸에 두지 않는다 */
    GENERAL("일반", excludable = false),
    COLD("냉동·냉장", excludable = true),
    MOVING("이사", excludable = true),
    HAZARDOUS("위험물", excludable = true),
    HEAVY("중량물", excludable = true),
    LONG("장척물", excludable = true),
    CONSTRUCTION("건자재", excludable = true),
    FARM("농수산물", excludable = true),
    WASTE("폐기물", excludable = true),
    ;

    companion object {
        /** 모르는 값(서버에 새로 생긴 종류)은 null */
        fun fromName(name: String): CargoCategory? = entries.firstOrNull { it.name == name }
    }
}
