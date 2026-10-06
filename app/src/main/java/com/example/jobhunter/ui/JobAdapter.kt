package com.example.jobhunter.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.jobhunter.R
import com.example.jobhunter.data.model.JobMatchItem
import com.example.jobhunter.databinding.ItemJobCardBinding
import com.google.android.material.chip.Chip
import java.util.Locale

class JobAdapter(
    private var jobList: List<JobMatchItem>,
    private val onJobClick: (JobMatchItem) -> Unit
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    fun updateData(newJobs: List<JobMatchItem>) {
        this.jobList = newJobs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ItemJobCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(jobList[position])
    }

    override fun getItemCount(): Int = jobList.size

    inner class JobViewHolder(private val binding: ItemJobCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(job: JobMatchItem) {
            val context = binding.root.context

            binding.tvJobTitle.text = job.jobTitle
            binding.tvJobId.text = "Portal Job ID: #${job.jobId}"

            // Score Formatting
            val scoreText = if (job.score % 1.0 == 0.0) {
                String.format(Locale.US, "%.0f%% Match", job.score)
            } else {
                String.format(Locale.US, "%.1f%% Match", job.score)
            }
            binding.tvMatchScore.text = scoreText

            // Score Coloring
            when {
                job.score >= 80.0 -> {
                    binding.tvMatchScore.setTextColor(ContextCompat.getColor(context, R.color.success))
                }
                job.score >= 50.0 -> {
                    binding.tvMatchScore.setTextColor(ContextCompat.getColor(context, R.color.warning))
                }
                else -> {
                    binding.tvMatchScore.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                }
            }

            // Missing skills handling
            if (job.missingSkills.isEmpty()) {
                binding.layoutFullMatch.visibility = View.VISIBLE
                binding.layoutMissingSkills.visibility = View.GONE
            } else {
                binding.layoutFullMatch.visibility = View.GONE
                binding.layoutMissingSkills.visibility = View.VISIBLE

                binding.chipGroupMissingSkills.removeAllViews()
                job.missingSkills.forEach { skill ->
                    val chip = Chip(context).apply {
                        text = skill
                        isClickable = false
                        isCheckable = false
                        textSize = 11f
                        setChipBackgroundColorResource(R.color.warning_light)
                        setTextColor(ContextCompat.getColor(context, R.color.warning))
                        setEnsureMinTouchTargetSize(false)
                    }
                    binding.chipGroupMissingSkills.addView(chip)
                }
            }

            binding.root.setOnClickListener { onJobClick(job) }
            binding.btnViewDetails.setOnClickListener { onJobClick(job) }
        }
    }
}
