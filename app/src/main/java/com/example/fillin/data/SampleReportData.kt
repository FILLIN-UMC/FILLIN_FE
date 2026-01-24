package com.example.fillin.data

import com.example.fillin.R
import com.example.fillin.domain.model.Report
import com.example.fillin.domain.model.ReportStatus
import com.example.fillin.domain.model.ReportType
import com.example.fillin.feature.home.ReportWithLocation
import java.util.Calendar

/**
 * 샘플 제보 데이터
 * 개발 및 테스트용으로 사용되는 제보 데이터입니다.
 */
object SampleReportData {
    
    /**
     * 날짜를 밀리초로 변환하는 헬퍼 함수
     */
    fun dateToMillis(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    /**
     * 임의 제보 데이터 10개
     * - 홍대입구역 근처: 3개
     * - 합정역 근처: 3개
     * - 신촌역 근처: 4개
     */
    fun getSampleReports(): List<ReportWithLocation> {
        return listOf(
            // 홍대입구역 근처 - 1
            ReportWithLocation(
                report = Report(
                    id = 1,
                    title = "서울시 마포구 양화로 188 홍대입구역 1번 출구 앞",
                    meta = "사고 발생",
                    type = ReportType.DANGER,
                    viewCount = 15,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img,
                    createdAtMillis = dateToMillis(2026, 1, 8)
                ),
                latitude = 37.556300,
                longitude = 126.923200
            ),
            // 홍대입구역 근처 - 2
            ReportWithLocation(
                report = Report(
                    id = 2,
                    title = "서울시 마포구 홍익로 3길 15-2",
                    meta = "맨홀 뚜껑 파손",
                    type = ReportType.INCONVENIENCE,
                    viewCount = 8,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_2,
                    createdAtMillis = dateToMillis(2026, 1, 19)
                ),
                latitude = 37.557000,
                longitude = 126.924000
            ),
            // 홍대입구역 근처 - 3
            ReportWithLocation(
                report = Report(
                    id = 3,
                    title = "서울시 마포구 양화로 188 홍대입구역 9번 출구 앞",
                    meta = "고양이 발견",
                    type = ReportType.DISCOVERY,
                    viewCount = 23,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_3,
                    createdAtMillis = dateToMillis(2026, 1, 18)
                ),
                latitude = 37.555500,
                longitude = 126.922500
            ),
            // 합정역 근처 - 1
            ReportWithLocation(
                report = Report(
                    id = 4,
                    title = "서울시 마포구 양화로 160 합정역 1번 출구 앞",
                    meta = "낙하물 위험",
                    type = ReportType.DANGER,
                    viewCount = 12,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img,
                    createdAtMillis = dateToMillis(2026, 1, 22)
                ),
                latitude = 37.550500,
                longitude = 126.914500
            ),
            // 합정역 근처 - 2
            ReportWithLocation(
                report = Report(
                    id = 5,
                    title = "서울시 마포구 양화로 140 합정역 2번 출구 앞",
                    meta = "도로 파손",
                    type = ReportType.INCONVENIENCE,
                    viewCount = 6,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_2,
                    createdAtMillis = dateToMillis(2026, 1, 16)
                ),
                latitude = 37.549500,
                longitude = 126.913500
            ),
            // 합정역 근처 - 3
            ReportWithLocation(
                report = Report(
                    id = 6,
                    title = "서울시 마포구 양화로 150 합정역 3번 출구 앞",
                    meta = "예쁜 꽃 발견",
                    type = ReportType.DISCOVERY,
                    viewCount = 18,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_3,
                    createdAtMillis = dateToMillis(2026, 1, 19)
                ),
                latitude = 37.551000,
                longitude = 126.915000
            ),
            // 신촌역 근처 - 1
            ReportWithLocation(
                report = Report(
                    id = 7,
                    title = "서울시 서대문구 연세로 50 신촌역 1번 출구 앞",
                    meta = "전기 누전 위험",
                    type = ReportType.DANGER,
                    viewCount = 9,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img,
                    createdAtMillis = dateToMillis(2026, 1, 22)
                ),
                latitude = 37.555100,
                longitude = 126.936800
            ),
            // 신촌역 근처 - 2
            ReportWithLocation(
                report = Report(
                    id = 8,
                    title = "서울시 서대문구 연세로 60 신촌역 2번 출구 앞",
                    meta = "인도 파손",
                    type = ReportType.INCONVENIENCE,
                    viewCount = 11,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_2,
                    createdAtMillis = dateToMillis(2026, 1, 15)
                ),
                latitude = 37.556100,
                longitude = 126.937800
            ),
            // 신촌역 근처 - 3
            ReportWithLocation(
                report = Report(
                    id = 9,
                    title = "서울시 서대문구 연세로 40 신촌역 3번 출구 앞",
                    meta = "새로운 벤치 설치",
                    type = ReportType.DISCOVERY,
                    viewCount = 7,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_3,
                    createdAtMillis = dateToMillis(2026, 1, 22)
                ),
                latitude = 37.554100,
                longitude = 126.935800
            ),
            // 신촌역 근처 - 4
            ReportWithLocation(
                report = Report(
                    id = 10,
                    title = "서울시 서대문구 연세로 55 신촌역 4번 출구 앞",
                    meta = "예쁜 벽화 발견",
                    type = ReportType.DISCOVERY,
                    viewCount = 20,
                    status = ReportStatus.ACTIVE,
                    imageResId = R.drawable.ic_report_img_3,
                    createdAtMillis = dateToMillis(2026, 1, 14)
                ),
                latitude = 37.555600,
                longitude = 126.937000
            )
        )
    }
}
