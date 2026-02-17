package com.example.fillin.data.db

import android.net.Uri

/** 업로드 성공 시 지도에 추가할 수 있도록 반환하는 데이터 */
data class UploadedReportResult(
    val documentId: String,
    val imageUrl: String? = null,
    val imageUri: Uri? = null,
    val category: String,
    val title: String,
    val location: String
)
