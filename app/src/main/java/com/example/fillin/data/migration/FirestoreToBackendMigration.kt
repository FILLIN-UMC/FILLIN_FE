package com.example.fillin.data.migration

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.fillin.R
import com.example.fillin.data.api.TokenManager
import com.example.fillin.data.db.FirestoreRepository
import com.example.fillin.data.SharedReportData
import com.example.fillin.data.db.ReportDocument
import com.example.fillin.data.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Firestore에 저장된 제보를 백엔드 DB로 마이그레이션
 * - 로그인 상태에서만 실행
 * - 이미 마이그레이션된 제보는 건너뜀
 */
class FirestoreToBackendMigration(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository,
    private val reportRepository: ReportRepository
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun migrate(): Result<MigrationResult> = withContext(Dispatchers.IO) {
        if (TokenManager.getBearerToken(context) == null) {
            return@withContext Result.success(MigrationResult(successCount = 0, skipReason = "not_logged_in"))
        }

        val firestoreReports = firestoreRepository.getReports()
        if (firestoreReports.isEmpty()) {
            return@withContext Result.success(MigrationResult(successCount = 0, skipReason = "no_reports"))
        }

        val migratedIds = loadMigratedIds().toMutableSet()
        val sampleReportIds = SharedReportData.getSampleReportDocumentIds(context)
        val sampleSignatures = buildSampleSignatures()  // 이미 Firestore에 있는 샘플 식별용
        var successCount = 0
        var failCount = 0

        for (doc in firestoreReports) {
            if (doc.documentId in migratedIds) continue
            if (doc.documentId in sampleReportIds) continue  // 샘플(데모) 제보는 백엔드로 이전하지 않음
            if (isSampleReportByContent(doc, sampleSignatures)) continue  // 내용으로 샘플 식별 (기존 데이터용)

            val result = migrateOne(doc)
            when (result) {
                MigrateStatus.SUCCESS -> {
                    successCount++
                    migratedIds.add(doc.documentId)
                }
                MigrateStatus.FAILED -> failCount++
            }
        }

        saveMigratedIds(migratedIds)
        Result.success(MigrationResult(successCount = successCount, failCount = failCount))
    }

    private suspend fun migrateOne(doc: ReportDocument): MigrateStatus {
        val d = doc.data
        return try {
            val imageUri = if (d.imageUrl.isNotBlank()) {
                downloadImageToFile(d.imageUrl)
            } else null

            if (imageUri == null && d.imageUrl.isNotBlank()) {
                Log.w("FirestoreMigration", "이미지 다운로드 실패, 이미지 없이 시도: ${doc.documentId}")
            }

            val category = d.category.ifBlank { "발견" }
            val title = d.title.ifBlank { "제보" }
            val location = d.location.ifBlank { "" }
            val lat = if (d.latitude != 0.0 || d.longitude != 0.0) d.latitude else 37.5665
            val lon = if (d.latitude != 0.0 || d.longitude != 0.0) d.longitude else 126.9780

            val uriToUse = imageUri ?: createPlaceholderImage()
            val result = reportRepository.uploadReport(
                category = category,
                title = title,
                location = location,
                imageUri = uriToUse,
                latitude = lat,
                longitude = lon
            )

            if (result != null) {
                Log.d("FirestoreMigration", "마이그레이션 성공: ${doc.documentId} -> reportId=${result.documentId}")
                MigrateStatus.SUCCESS
            } else {
                Log.e("FirestoreMigration", "마이그레이션 실패: ${doc.documentId}")
                MigrateStatus.FAILED
            }
        } catch (e: Exception) {
            Log.e("FirestoreMigration", "마이그레이션 예외: ${doc.documentId}", e)
            MigrateStatus.FAILED
        }
    }

    private suspend fun downloadImageToFile(url: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.connect()

            val file = File(context.cacheDir, "migrate_img_${System.currentTimeMillis()}.jpg")
            connection.getInputStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e("FirestoreMigration", "이미지 다운로드 실패: $url", e)
            null
        }
    }

    private fun createPlaceholderImage(): Uri {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_report_img)
                ?: throw IllegalStateException("bitmap null")
            val file = File(context.cacheDir, "migrate_placeholder_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e("FirestoreMigration", "플레이스홀더 생성 실패", e)
            val file = File(context.cacheDir, "migrate_placeholder_${System.currentTimeMillis()}.jpg")
            file.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) // minimal JPEG header
            Uri.fromFile(file)
        }
    }

    private fun buildSampleSignatures(): Set<String> = emptySet()

    private fun isSampleReportByContent(doc: ReportDocument, sampleSignatures: Set<String>): Boolean {
        val d = doc.data
        val sig = "${d.title}|${d.location}|${d.latitude}|${d.longitude}"
        return sig in sampleSignatures
    }

    private fun loadMigratedIds(): Set<String> {
        return prefs.getStringSet(KEY_MIGRATED_IDS, null) ?: emptySet()
    }

    private fun saveMigratedIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_MIGRATED_IDS, ids).apply()
    }

    private enum class MigrateStatus { SUCCESS, FAILED }

    data class MigrationResult(
        val successCount: Int,
        val failCount: Int = 0,
        val skipReason: String? = null
    )

    companion object {
        private const val PREFS_NAME = "fillin_migration"
        private const val KEY_MIGRATED_IDS = "firestore_migrated_to_backend_ids"
    }
}
