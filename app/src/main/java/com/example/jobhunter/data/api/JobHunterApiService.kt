package com.example.jobhunter.data.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface JobHunterApiService {

    /**
     * Uploads the resume file using multipart/form-data.
     * Supports either a relative endpoint or a full custom endpoint URL.
     */
    @Multipart
    @POST
    suspend fun uploadResume(
        @Url endpointUrl: String,
        @Part resumeFile: MultipartBody.Part
    ): Response<ResponseBody>
}
