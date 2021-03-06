package cz.prague.cvut.fit.steuejan.amtelapp.activities

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.lifecycle.observe
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import cz.prague.cvut.fit.steuejan.amtelapp.App.Companion.toast
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.databinding.ActivityInputReportBinding
import cz.prague.cvut.fit.steuejan.amtelapp.view_models.activities.InputReportActivityVM

class InputReportActivity : AbstractBaseActivity()
{
    private lateinit var binding: ActivityInputReportBinding
    
    private val viewModel by viewModels<InputReportActivityVM>()

    private val progressDialog by lazy {
        MaterialDialog(this)
            .customView(R.layout.progress_layout)
    }

    companion object
    {
        private const val REPORT_TITLE = "lastReportTitle"
        private const val REPORT_LEAD = "lastReportLead"
        private const val REPORT_TEXT = "lastReportText"
        const val REPORT = "report"
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        binding = ActivityInputReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        setupLayout()
        setArrowBack()
        saveReport()
        publishReport()
        deleteReport()
    }

    override fun onDestroy()
    {
        super.onDestroy()
        with(binding) {
            saveReport.setOnClickListener(null)
            publishReport.setOnClickListener(null)
            deleteReport.setOnClickListener(null)
        }
    }

    override fun onResume()
    {
        super.onResume()
        //display last saved title, lead and text when the report is not published yet
        viewModel.report ?: with(PreferenceManager.getDefaultSharedPreferences(this)) {
            with(binding) {
                title.setText(getString(REPORT_TITLE, ""))
                lead.setText(getString(REPORT_LEAD, ""))
                text.setText(getString(REPORT_TEXT, ""))
            }
        }
    }

    override fun onBackPressed()
    {
        MaterialDialog(this).show {
            title(text = getString(R.string.exit_report_confirmation))
            message(text = getString(R.string.exit_report_message))
            negativeButton()
            positiveButton(text = getString(R.string.leave)) {
                if(viewModel.published)
                {
                    //in order to refresh reports in the ReportsFragment
                    setResult(RESULT_OK)
                    finish()
                }
                else finish()
            }
        }
    }

    private fun setupLayout()
    {
        intent.extras?.let { bundle ->
            viewModel.report = bundle.getParcelable(REPORT)
        }

        //the report has been already published
        viewModel.report?.let {
            setToolbarTitle(getString(R.string.edit_report))
            with(binding) {
                deleteReport.visibility = VISIBLE
                publishReport.visibility = GONE
                saveReport.text = getString(R.string.actualize)
                title.setText(it.title)
                lead.setText(it.lead)
                text.setText(it.text)
                viewModel.published = true
            }
        } ?: setToolbarTitle(getString(R.string.write_report))
    }
    
    private fun saveReport()
    {
        binding.saveReport.setOnClickListener {
            //if the report has been already published, update the report in database
            viewModel.report?.let {
                with(binding) {
                    progressDialog.show()
                    val title = title.text.toString().trim()
                    val lead = lead.text.toString().trim()
                    val text = text.text.toString().trim()
                    viewModel.publishRecord(it.id, title, lead, text)
                }
            }
                //else save the work in progress
            ?: let {
                updateReportInPreferences()
                toast(R.string.saved)
            }
        }
    }

    private fun updateReportInPreferences()
    {
        val title: String?
        val lead: String?
        val text: String?

        //clear data in preferences when the report is published
        if(viewModel.published)
        {
            title = null
            lead = null
            text = null
        }
        else
        {
            title = binding.title.text.toString()
            lead = binding.lead.text.toString()
            text = binding.text.text.toString()
        }

        PreferenceManager
            .getDefaultSharedPreferences(this)
            .edit {
                putString(REPORT_TITLE, title)
                putString(REPORT_LEAD, lead)
                putString(REPORT_TEXT, text)
            }
    }

    private fun publishReport()
    {
        binding.publishReport.setOnClickListener {
            MaterialDialog(this).show {
                title(text = getString(R.string.publish_report_title))
                message(text = getString(R.string.publish_report_message))
                positiveButton(text = getString(R.string.publish)) {
                    progressDialog.show()
                    with(binding) {
                        val title = title.text.toString().trim()
                        val lead = lead.text.toString().trim()
                        val text = text.text.toString().trim()
                        viewModel.publishRecord(null, title, lead, text)
                    }
                }
                negativeButton()
            }
        }

        //invoked when the report was published
        viewModel.reportPublished.observe(this) { published ->
            progressDialog.dismiss()
            if(viewModel.report != null)
            {
                if(published) toast(getString(R.string.updated))
                else toast(getString(R.string.not_updated))
            }
            else
            {
                if(published)
                {
                    toast(getString(R.string.published))
                    updateReportInPreferences()
                }
                else toast(getString(R.string.not_published))
            }
        }
    }

    private fun deleteReport()
    {
        binding.deleteReport.setOnClickListener {
            MaterialDialog(this).show {
                title(text = getString(R.string.delete_report_title))
                positiveButton(R.string.delete) {
                    progressDialog.show()
                    viewModel.deleteReport()
                }
                negativeButton()
            }
        }

        viewModel.reportDeleted.observe(this) { deleted ->
            progressDialog.dismiss()
            if(deleted)
            {
                toast(getString(R.string.deleted))
                with(binding) {
                    title.text?.clear()
                    lead.text?.clear()
                    text.text?.clear()
                }
            }
            else toast(getString(R.string.not_deleted))
        }
    }
}