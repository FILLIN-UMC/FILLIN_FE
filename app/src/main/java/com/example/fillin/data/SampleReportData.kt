package com.example.fillin.data

import com.example.fillin.R
import com.example.fillin.domain.model.ReporterInfo

/**
 * 사용자 정보 등 앱에서 공통으로 사용하는 데이터
 * (하드코딩된 샘플 제보 데이터는 제거됨 - API 데이터만 사용)
 */
object SampleReportData {

    /**
     * 현재 사용자 정보 (로그인된 사용자)
     */
    val currentUser = ReporterInfo(
        userId = 1,
        nickname = "홍길동",
        profileImageResId = R.drawable.ic_profile_img
    )
}
