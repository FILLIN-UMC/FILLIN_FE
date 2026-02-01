package com.example.fillin.data.db

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.fillin.data.SampleReportData
import com.example.fillin.domain.model.ReportType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date

// 이미지를 먼저 올리고, 그 주소를 포함해 텍스트 데이터를 저장하는 함수입니다.
class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance() // 1. Firebase 서비스 객체 초기화
    private val storage = FirebaseStorage.getInstance()
    // suspend를 사용하면 비동기 처리가 가능해져서, 데이터를 주고받는 동안 앱 화면이 멈추지(Freeze) 않습니다.
    suspend fun uploadReport(
        category: String,
        title: String,
        location: String,
        imageUri: Uri,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        createdAtMillis: Long? = null
    ): UploadedReportResult? {
        return try {
            // 1. Storage에 이미지 업로드 (파일명은 시간순으로 중복 방지)
            val fileName = "reports/${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child(fileName)
            storageRef.putFile(imageUri).await()

            // 2. 업로드된 이미지의 URL 가져오기
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // 3. Firestore에 데이터 저장 (위도/경도 포함)
            val timestamp = createdAtMillis?.let { Timestamp(Date(it)) } ?: Timestamp.now()
            val reportData = ReportData(
                category = category,
                title = title,
                location = location,
                imageUrl = downloadUrl,
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp
            )
            // "reports" 컬렉션에 저장
            val docRef = db.collection("reports").add(reportData).await()
            UploadedReportResult(
                documentId = docRef.id,
                imageUrl = downloadUrl,
                category = category,
                title = title,
                location = location
            )
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "uploadReport 실패: ${e.message}", e)
            null
        }
    }

    /**
     * 샘플 제보 데이터를 Firebase Storage + Firestore로 마이그레이션합니다.
     * drawable 리소스를 이미지로 변환해 업로드한 뒤 Firestore에 저장합니다.
     */
    suspend fun migrateSampleReportsToFirebase(context: Context): Result<Int> {
        return try {
            val sampleReports = SampleReportData.getSampleReports()
            var successCount = 0
            for (reportWithLocation in sampleReports) {
                val report = reportWithLocation.report
                val drawableResId = report.imageResId ?: continue
                val uri = drawableToUri(context, drawableResId) ?: continue
                val category = when (report.type) {
                    ReportType.DANGER -> "위험"
                    ReportType.INCONVENIENCE -> "불편"
                    ReportType.DISCOVERY -> "발견"
                }
                val result = uploadReport(
                    category = category,
                    title = report.meta,
                    location = report.title,
                    imageUri = uri,
                    latitude = reportWithLocation.latitude,
                    longitude = reportWithLocation.longitude,
                    createdAtMillis = report.createdAtMillis
                )
                if (result != null) successCount++
            }
            Result.success(successCount)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "migrateSampleReports 실패: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun drawableToUri(context: Context, drawableResId: Int): Uri? {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableResId)
                ?: return null
            val file = File(context.cacheDir, "migrate_${drawableResId}_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "drawableToUri 실패: ${e.message}", e)
            null
        }
    }

    /** 피드백 카운트, 부정 피드백 시점 목록, 유효성 지속 시점을 Firestore에 업데이트 */
    suspend fun updateReportFeedback(
        documentId: String,
        positiveCount: Int,
        negativeCount: Int,
        positive70SustainedSinceMillis: Long? = null,
        positive40to60SustainedSinceMillis: Long? = null,
        negativeFeedbackTimestamps: List<Long>? = null
    ) {
        try {
            val updates = mutableMapOf<String, Any>(
                "positiveFeedbackCount" to positiveCount,
                "negativeFeedbackCount" to negativeCount
            )
            updates["positive70SustainedSinceMillis"] = positive70SustainedSinceMillis?.let { it }
                ?: FieldValue.delete()
            updates["positive40to60SustainedSinceMillis"] = positive40to60SustainedSinceMillis?.let { it }
                ?: FieldValue.delete()
            if (negativeFeedbackTimestamps != null) {
                updates["negativeFeedbackTimestamps"] = negativeFeedbackTimestamps
            }
            db.collection("reports").document(documentId).update(updates).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "updateReportFeedback 실패: ${e.message}", e)
        }
    }

    /** 좋아요 상태를 Firestore에 업데이트 (arrayUnion/arrayRemove) */
    suspend fun updateReportLike(documentId: String, userId: String, isLiked: Boolean) {
        try {
            val docRef = db.collection("reports").document(documentId)
            if (isLiked) {
                docRef.update("likedByUserIds", FieldValue.arrayUnion(userId)).await()
            } else {
                docRef.update("likedByUserIds", FieldValue.arrayRemove(userId)).await()
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "updateReportLike 실패: ${e.message}", e)
        }
    }

    /** Firestore에 저장된 제보 목록 조회 (앱 재시작 시 지도에 표시용) */
    suspend fun getReports(): List<ReportDocument> {
        return try {
            val snapshot = db.collection("reports").get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.toObject(ReportData::class.java) ?: return@mapNotNull null
                ReportDocument(documentId = doc.id, data = data)
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "getReports 실패: ${e.message}", e)
            emptyList()
        }
    }
}

/** Firestore 문서 ID + 데이터 (조회 결과용) */
data class ReportDocument(
    val documentId: String,
    val data: ReportData
)

/** 업로드 성공 시 지도에 추가할 수 있도록 반환하는 데이터 */
data class UploadedReportResult(
    val documentId: String,
    val imageUrl: String,
    val category: String,
    val title: String,
    val location: String
)

/*
   Firebase Storage (창고)
     - 저장 대상: 사진, 영상, PDF 같은 **용량이 큰 이진 파일(Binary File)**입니다.
     - 역할: 파일을 안전하게 보관하고, 그 파일을 인터넷에서 볼 수 있는 **'주소(Download URL)'**를 생성해 줍니다.
   Firebase Firestore (장부)
     -  저장 대상: 카테고리, 제목, 장소, 그리고 위에서 받은 '이미지 주소' 같은 텍스트 데이터
     - 역할: 데이터를 체계적으로 정리하여 빠른 검색, 필터링, 정렬을 가능하게 합니다

  코드 흐름
     1. Storage 방문: putFile(imageUri)로 사진을 **창고(Storage)**에 먼저 넣습니다.
     2. 주소 수령: downloadUrl을 통해 창고에 저장된 사진을 찾아갈 수 있는 **영수증(URL)**을 받습니다.
     3. Firestore 방문: add(reportData)를 통해 제목, 장소와 함께 방금 받은 **영수증(URL)**을 **장부(Firestore)**에 기록합니다.
 */
