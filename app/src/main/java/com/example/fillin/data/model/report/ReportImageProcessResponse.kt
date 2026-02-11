package com.example.fillin.data.model.report

//번호판 감지 및 모자이크 API(POST /api/reports/image-process)의 결과를 처리하기 위해 필요.
import com.google.gson.annotations.SerializedName

/** 이미지 전처리(번호판 모자이크) 응답 */
data class ReportImageProcessResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,       // [추가] 서버의 상태 코드
    @SerializedName("message") val message: String? = null, // [추가] 👈 이 줄이 있어야 ViewModel 에러가 해결됩니다!
    @SerializedName("data") val data: ImageProcessData? = null
)

data class ImageProcessData(
    @SerializedName("hasLicensePlate") val hasLicensePlate: Boolean = false,
    @SerializedName("processedImageUrl") val processedImageUrl: String? = null // 번호판 없으면 null
)