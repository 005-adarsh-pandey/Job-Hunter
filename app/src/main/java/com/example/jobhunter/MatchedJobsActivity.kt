package com.example.jobhunter

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jobhunter.data.model.JobMatchItem
import com.example.jobhunter.data.model.ResumeAnalysisResponse
import com.example.jobhunter.databinding.ActivityMatchedJobsBinding
import com.example.jobhunter.ui.JobAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class MatchedJobsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESPONSE_DATA = "extra_response_data"
        const val EXTRA_FALLBACK_FILE_NAME = "extra_fallback_file_name"
    }

    private lateinit var binding: ActivityMatchedJobsBinding
    private lateinit var jobAdapter: JobAdapter

    private var allJobs: List<JobMatchItem> = emptyList()
    private var displayedJobs: List<JobMatchItem> = emptyList()
    private var detectedSkills: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchedJobsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        extractIntentData()
        setupRecyclerView()
        setupFilters()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun extractIntentData() {
        val response = intent.getSerializableExtra(EXTRA_RESPONSE_DATA) as? ResumeAnalysisResponse
        val fallbackFileName = intent.getStringExtra(EXTRA_FALLBACK_FILE_NAME) ?: "Uploaded Resume"

        val fileName = response?.fileName ?: fallbackFileName
        binding.tvResumeHeader.text = "Resume: $fileName"

        allJobs = response?.matches ?: emptyList()
        detectedSkills = response?.detectedSkills ?: emptyList()

        binding.tvMatchesCount.text = "Found ${allJobs.size} matching jobs from portal"

        // Populate detected skills chips
        if (detectedSkills.isNotEmpty()) {
            binding.cardDetectedSkills.visibility = View.VISIBLE
            binding.chipGroupDetectedSkills.removeAllViews()

            detectedSkills.forEach { skill ->
                val chip = Chip(this).apply {
                    text = skill
                    isClickable = false
                    isCheckable = false
                    textSize = 12f
                    setChipBackgroundColorResource(R.color.primary_light)
                    setTextColor(ContextCompat.getColor(this@MatchedJobsActivity, R.color.primary_variant))
                    setEnsureMinTouchTargetSize(false)
                }
                binding.chipGroupDetectedSkills.addView(chip)
            }
        } else {
            binding.cardDetectedSkills.visibility = View.GONE
        }

        displayedJobs = allJobs
    }

    private fun setupRecyclerView() {
        jobAdapter = JobAdapter(
            jobList = displayedJobs,
            onJobClick = { job -> showJobDetailsDialog(job) }
        )

        binding.rvMatchedJobs.layoutManager = LinearLayoutManager(this)
        binding.rvMatchedJobs.adapter = jobAdapter

        updateViewsVisibility()
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipFilterHighMatch) -> {
                    displayedJobs = allJobs.filter { it.score >= 80.0 }
                }
                checkedIds.contains(R.id.chipFilterFullSkillMatch) -> {
                    displayedJobs = allJobs.filter { it.missingSkills.isEmpty() }
                }
                else -> {
                    displayedJobs = allJobs
                }
            }
            jobAdapter.updateData(displayedJobs)
            updateViewsVisibility()
        }
    }

    private fun updateViewsVisibility() {
        if (displayedJobs.isEmpty()) {
            binding.rvMatchedJobs.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvMatchedJobs.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }

    private fun showJobDetailsDialog(job: JobMatchItem) {
        val scoreFormatted = if (job.score % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f%%", job.score)
        } else {
            String.format(Locale.US, "%.1f%%", job.score)
        }

        val skillsStatus = if (job.missingSkills.isEmpty()) {
            "✓ All required skills for this position are matched in your resume!"
        } else {
            "Missing Skills for this role:\n• " + job.missingSkills.joinToString("\n• ")
        }

        val detailsMessage = """
            Position: ${job.jobTitle}
            Portal Job ID: #${job.jobId}
            Match Score: $scoreFormatted
            
            $skillsStatus
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle(job.jobTitle)
            .setMessage(detailsMessage)
            .setPositiveButton("OK", null)
            .show()
    }
}
