package com.example.jobhunter.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class JobMatchResponse(
    @SerializedName("success")
    val success: Boolean = true,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("jobs", alternate = ["matched_jobs", "matchedJobs", "data", "results", "job_matches"])
    val jobs: List<JobItem>? = null,

    @SerializedName("total", alternate = ["total_matches", "count", "totalMatches"])
    val total: Int? = null
) : Serializable
