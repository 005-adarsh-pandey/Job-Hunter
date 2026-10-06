package com.example.jobhunter.data.repository

import android.content.Context
import android.net.Uri
import com.example.jobhunter.data.api.ApiClient
import com.example.jobhunter.data.model.ResumeAnalysisResponse
import com.example.jobhunter.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

sealed class UploadResult {
    data class Success(
        val data: ResumeAnalysisResponse
    ) : UploadResult()

    data class Error(val message: String) : UploadResult()
}

class JobRepository(private val context: Context) {

    suspend fun uploadResumeAndMatchJobs(fileUri: Uri, fileName: String): UploadResult =
        withContext(Dispatchers.IO) {
            val endpointUrl = ApiClient.getEndpointUrl(context)

            // Prepare multipart file
            val tempFile: File? = FileUtils.createTempFileFromUri(context, fileUri, fileName)
            if (tempFile == null || !tempFile.exists()) {
                return@withContext UploadResult.Error("Failed to read the selected resume file.")
            }

            try {
                val fileInfo = FileUtils.getFileInfo(context, fileUri)
                val mediaType = fileInfo.mimeType.toMediaTypeOrNull()
                val requestBody = tempFile.asRequestBody(mediaType)
                // Field name is "file" matching curl --form 'file=@...'
                val multipartPart = MultipartBody.Part.createFormData("file", fileName, requestBody)

                val response = ApiClient.apiService.uploadResume(endpointUrl, multipartPart)

                if (response.isSuccessful) {
                    val rawJson = response.body()?.string() ?: ""
                    if (rawJson.isBlank()) {
                        return@withContext UploadResult.Error("Empty response received from server.")
                    }

                    try {
                        val parsed = ApiClient.getGson().fromJson(rawJson, ResumeAnalysisResponse::class.java)
                        if (parsed != null && (parsed.status == "success" || parsed.matches.isNotEmpty())) {
                            UploadResult.Success(parsed)
                        } else {
                            UploadResult.Error("No matching jobs found in portal for this resume.")
                        }
                    } catch (e: Exception) {
                        UploadResult.Error("Failed to parse response: ${e.localizedMessage ?: e.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val msg = if (!errorBody.isNullOrBlank()) {
                        "Server error (${response.code()}): $errorBody"
                    } else {
                        "Server error (${response.code()}): ${response.message()}"
                    }
                    UploadResult.Error(msg)
                }
            } catch (e: Exception) {
                UploadResult.Error("Connection error: ${e.localizedMessage ?: e.message}\n(Please check internet connectivity and ensure the backend server is reachable)")
            } finally {
                // Clean up cached temporary file
                try {
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                } catch (ignored: Exception) {}
            }
        }
}
