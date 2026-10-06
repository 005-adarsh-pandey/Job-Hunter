package com.example.jobhunter.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ResumeAnalysisResponse(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("file_name")
    val fileName: String? = null,

    @SerializedName("detected_skills")
    val detectedSkills: List<String> = emptyList(),

    @SerializedName("matches")
    val matches: List<JobMatchItem> = emptyList()
) : Serializable

data class JobMatchItem(
    @SerializedName("job_id")
    val jobId: Int = 0,

    @SerializedName("job_title")
    val jobTitle: String = "Job",

    @SerializedName("score")
    val score: Double = 0.0,

    @SerializedName("missing_skills")
    val missingSkills: List<String> = emptyList()
) : Serializable
