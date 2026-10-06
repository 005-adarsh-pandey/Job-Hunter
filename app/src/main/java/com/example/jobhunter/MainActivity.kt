package com.example.jobhunter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.jobhunter.data.api.ApiClient
import com.example.jobhunter.data.model.ResumeAnalysisResponse
import com.example.jobhunter.data.repository.JobRepository
import com.example.jobhunter.data.repository.UploadResult
import com.example.jobhunter.databinding.ActivityMainBinding
import com.example.jobhunter.databinding.DialogEndpointSettingsBinding
import com.example.jobhunter.util.FileUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var jobRepository: JobRepository

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    private val supportedMimeTypes = arrayOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )

    private val filePickerFallbackLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { handleSelectedFile(it) }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jobRepository = JobRepository(this)

        updateEndpointSummary()
        setupListeners()
    }

    private fun setupListeners() {
        // Open file picker on dropzone or browse button
        binding.uploadDropZone.setOnClickListener {
            launchFilePicker()
        }
        binding.btnSelectFile.setOnClickListener {
            launchFilePicker()
        }

        // Remove selected file
        binding.btnRemoveFile.setOnClickListener {
            clearSelectedFile()
        }

        // Settings / Endpoint config
        binding.btnSettings.setOnClickListener {
            showEndpointSettingsDialog()
        }
        binding.cardEndpointInfo.setOnClickListener {
            showEndpointSettingsDialog()
        }

        // Upload and match button
        binding.btnMatchJobs.setOnClickListener {
            processResumeAndMatch()
        }
    }

    private fun launchFilePicker() {
        try {
            filePickerLauncher.launch(supportedMimeTypes)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes)
            }
            filePickerFallbackLauncher.launch(intent)
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        val fileInfo = FileUtils.getFileInfo(this, uri)

        if (!FileUtils.isSupportedResume(fileInfo.name, fileInfo.mimeType)) {
            Snackbar.make(
                binding.root,
                "Please select a PDF or DOC/DOCX file.",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        selectedFileUri = uri
        selectedFileName = fileInfo.name

        // Update UI
        binding.tvFileName.text = fileInfo.name
        val extBadge = fileInfo.extension.uppercase()
        binding.tvFileSize.text = "$extBadge • ${fileInfo.sizeFormatted}"

        // Set document icon tint
        if (fileInfo.extension.equals("pdf", ignoreCase = true)) {
            binding.ivDocIcon.setColorFilter(ContextCompat.getColor(this, R.color.danger))
        } else {
            binding.ivDocIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary))
        }

        binding.cardSelectedFile.visibility = View.VISIBLE
        binding.btnMatchJobs.isEnabled = true

        Snackbar.make(
            binding.root,
            "Resume selected: ${fileInfo.name}",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun clearSelectedFile() {
        selectedFileUri = null
        selectedFileName = null
        binding.cardSelectedFile.visibility = View.GONE
    }

    private fun updateEndpointSummary() {
        val currentUrl = ApiClient.getEndpointUrl(this)
        binding.tvEndpointSummary.text = "Endpoint: $currentUrl"
    }

    private fun showEndpointSettingsDialog() {
        val dialogBinding = DialogEndpointSettingsBinding.inflate(LayoutInflater.from(this))
        val currentUrl = ApiClient.getEndpointUrl(this)

        dialogBinding.etEndpoint.setText(currentUrl)
        dialogBinding.switchMockMode.visibility = View.GONE

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val newUrl = dialogBinding.etEndpoint.text?.toString()?.trim()
                if (!newUrl.isNullOrEmpty()) {
                    ApiClient.setEndpointUrl(this, newUrl)
                }
                updateEndpointSummary()
                Toast.makeText(this, "Endpoint updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Reset to Default") { _, _ ->
                ApiClient.setEndpointUrl(this, ApiClient.DEFAULT_ENDPOINT)
                updateEndpointSummary()
                Toast.makeText(this, "Reset to live default endpoint", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun processResumeAndMatch() {
        val uri = selectedFileUri
        val fileName = selectedFileName

        if (uri == null || fileName == null) {
            Snackbar.make(
                binding.root,
                "Please select a resume file first.",
                Snackbar.LENGTH_LONG
            ).setAction("Select") {
                launchFilePicker()
            }.show()
            return
        }

        setLoading(true, "Uploading resume & matching jobs from portal...")

        lifecycleScope.launch {
            val result = jobRepository.uploadResumeAndMatchJobs(uri, fileName)
            setLoading(false)

            when (result) {
                is UploadResult.Success -> {
                    navigateToMatchedJobs(result.data, fileName)
                }
                is UploadResult.Error -> {
                    showErrorDialog(result.message)
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean, message: String = "") {
        if (isLoading) {
            binding.btnMatchJobs.isEnabled = false
            binding.layoutLoading.visibility = View.VISIBLE
            binding.tvLoadingStatus.text = message
            binding.uploadDropZone.isEnabled = false
        } else {
            binding.btnMatchJobs.isEnabled = true
            binding.layoutLoading.visibility = View.GONE
            binding.uploadDropZone.isEnabled = true
        }
    }

    private fun navigateToMatchedJobs(data: ResumeAnalysisResponse, fallbackFileName: String) {
        val intent = Intent(this, MatchedJobsActivity::class.java).apply {
            putExtra(MatchedJobsActivity.EXTRA_RESPONSE_DATA, data)
            putExtra(MatchedJobsActivity.EXTRA_FALLBACK_FILE_NAME, fallbackFileName)
        }
        startActivity(intent)
    }

    private fun showErrorDialog(errorMessage: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Matching Error")
            .setMessage(errorMessage)
            .setPositiveButton("Retry") { _, _ ->
                processResumeAndMatch()
            }
            .setNeutralButton("Check Endpoint") { _, _ ->
                showEndpointSettingsDialog()
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }
}