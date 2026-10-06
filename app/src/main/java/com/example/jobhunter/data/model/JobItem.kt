package com.example.jobhunter.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class JobItem(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("title", alternate = ["job_title", "role", "position", "jobTitle"])
    val title: String = "Job Title",

    @SerializedName("company", alternate = ["company_name", "employer", "organization", "companyName"])
    val company: String = "Company",

    @SerializedName("location", alternate = ["job_location", "city", "place", "work_location"])
    val location: String? = "Remote",

    @SerializedName("matchScore", alternate = ["match_score", "score", "match_percentage", "compatibility", "matchPercentage"])
    val matchScore: Int = 85,

    @SerializedName("salaryRange", alternate = ["salary_range", "salary", "compensation", "pay"])
    val salaryRange: String? = null,

    @SerializedName("jobType", alternate = ["job_type", "type", "employment_type", "workType"])
    val jobType: String? = "Full-time",

    @SerializedName("description", alternate = ["job_description", "summary", "overview", "details"])
    val description: String? = null,

    @SerializedName("matchedSkills", alternate = ["matched_skills", "skills", "key_skills", "matchedSkillsList"])
    val matchedSkills: List<String> = emptyList(),

    @SerializedName("applyUrl", alternate = ["apply_url", "url", "link", "job_url"])
    val applyUrl: String? = null
) : Serializable
