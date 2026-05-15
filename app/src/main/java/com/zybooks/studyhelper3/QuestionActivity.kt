package com.zybooks.studyhelper3

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.zybooks.studyhelper3.model.Priority
import com.zybooks.studyhelper3.model.Subject
import com.zybooks.studyhelper3.viewmodel.QuestionDetailViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * TaskMaster Task Details Activity
 * Phase 3: Displays full task metadata and provides management actions.
 * Fixed: Added click debouncing and safety checks for navigation.
 */
class QuestionActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var categoryValue: TextView
    private lateinit var priorityValue: TextView
    private lateinit var difficultyValue: TextView
    private lateinit var dueDateValue: TextView
    private lateinit var recurringValue: TextView
    private lateinit var completeButton: MaterialButton
    
    private var subjectId = -1L
    private var subject: Subject? = null
    private var lastClickTime: Long = 0

    private val questionDetailViewModel: QuestionDetailViewModel by lazy {
        ViewModelProvider(this)[QuestionDetailViewModel::class.java]
    }

    private val editTaskResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, R.string.task_updated, Toast.LENGTH_SHORT).show()
        }
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    private val dbDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    companion object {
        const val EXTRA_SUBJECT_ID = "com.zybooks.studyhelper.subject_id"
        const val EXTRA_SUBJECT_TEXT = "com.zybooks.studyhelper.subject_text"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_bar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Initialize Views
        titleTextView = findViewById(R.id.question_text_view)
        descriptionTextView = findViewById(R.id.answer_text_view)
        categoryValue = findViewById(R.id.category_value)
        priorityValue = findViewById(R.id.priority_value)
        difficultyValue = findViewById(R.id.difficulty_value)
        dueDateValue = findViewById(R.id.due_date_value)
        recurringValue = findViewById(R.id.recurring_value)
        completeButton = findViewById(R.id.complete_button)

        // Setup Button Listeners
        findViewById<MaterialButton>(R.id.start_timer_button).setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            val intent = Intent(this, TimerActivity::class.java)
            intent.putExtra(TimerActivity.EXTRA_SUBJECT_ID, subjectId)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.edit_button).setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            editTask()
        }

        findViewById<MaterialButton>(R.id.delete_button).setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            showDeleteConfirmation()
        }

        findViewById<MaterialButton>(R.id.back_button).setOnClickListener {
            finish()
        }

        completeButton.setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            toggleTaskCompletion()
        }

        subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1L)
        if (subjectId != -1L) {
            observeTask()
        } else {
            Toast.makeText(this, "Error: Task not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun observeTask() {
        questionDetailViewModel.getSubject(subjectId).observe(this) { s ->
            if (s != null) {
                subject = s
                updateUI(s)
            }
        }
    }

    private fun updateUI(s: Subject) {
        titleTextView.text = s.title
        descriptionTextView.text = s.description.ifEmpty { getString(R.string.no_questions) }
        categoryValue.text = s.category
        
        priorityValue.text = when(s.priority) {
            Priority.HIGH -> getString(R.string.priority_high)
            Priority.MEDIUM -> getString(R.string.priority_medium)
            Priority.LOW -> getString(R.string.priority_low)
        }
        val priorityColor = when (s.priority) {
            Priority.HIGH -> Color.parseColor("#F44336")
            Priority.MEDIUM -> Color.parseColor("#FF9800")
            Priority.LOW -> Color.parseColor("#4CAF50")
        }
        priorityValue.setTextColor(priorityColor)

        difficultyValue.text = getString(R.string.difficulty_level_format, s.difficulty)
        
        if (s.dueDate != null) {
            try {
                val localDate = LocalDate.parse(s.dueDate, dbDateFormatter)
                dueDateValue.text = localDate.format(dateFormatter)
            } catch (e: Exception) {
                dueDateValue.text = s.dueDate
            }
        } else {
            dueDateValue.text = getString(R.string.due_today)
        }

        recurringValue.text = if (s.isRecurring) getString(R.string.yes) else getString(R.string.no)

        if (s.isCompleted) {
            titleTextView.paintFlags = titleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            completeButton.text = getString(R.string.mark_incomplete)
            completeButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#757575"))
            completeButton.setIconResource(R.drawable.ic_back) // Reuse back icon for reversal
            completeButton.setIconTintResource(R.color.white)
        } else {
            titleTextView.paintFlags = titleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            completeButton.text = getString(R.string.mark_completed)
            completeButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success_green))
            completeButton.setIconResource(R.drawable.check)
            completeButton.setIconTintResource(R.color.white)
        }
    }

    private fun toggleTaskCompletion() {
        subject?.let {
            it.isCompleted = !it.isCompleted
            it.updatedAt = System.currentTimeMillis()
            if (it.isCompleted) {
                it.completionDate = System.currentTimeMillis()
            } else {
                it.completionDate = null
            }
            questionDetailViewModel.updateSubject(it)
            val msgRes = if (it.isCompleted) R.string.task_completed else R.string.task_incomplete
            Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show()
        }
    }

    private fun editTask() {
        val intent = Intent(this, QuestionEditActivity::class.java)
        intent.putExtra(QuestionActivity.EXTRA_SUBJECT_ID, subjectId)
        editTaskResultLauncher.launch(intent)
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_msg)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteTask()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteTask() {
        subject?.let {
            questionDetailViewModel.deleteSubject(it)
            Toast.makeText(this, R.string.task_deleted_toast, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
