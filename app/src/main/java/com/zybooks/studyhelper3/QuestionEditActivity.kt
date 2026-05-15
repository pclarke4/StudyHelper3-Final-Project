package com.zybooks.studyhelper3

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.materialswitch.MaterialSwitch
import com.zybooks.studyhelper3.model.Priority
import com.zybooks.studyhelper3.model.Question
import com.zybooks.studyhelper3.model.Subject
import com.zybooks.studyhelper3.viewmodel.QuestionDetailViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * TaskMaster Add/Edit Task Activity
 * Phase 3: Handles comprehensive task metadata with fixed navigation and async logic.
 * Polished: Updated to match Quest theme and fixed unused parameters.
 */
class QuestionEditActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var priorityChipGroup: ChipGroup
    private lateinit var difficultyChipGroup: ChipGroup
    private lateinit var datePickerButton: Button
    private lateinit var recurringSwitch: MaterialSwitch
    private lateinit var saveButton: Button
    
    private var questionId = -1L
    private var subjectId = -1L
    private var question: Question = Question()
    private var subject: Subject? = null
    private var selectedDueDate: String? = null
    private var lastClickTime: Long = 0

    private val questionDetailViewModel: QuestionDetailViewModel by lazy {
        ViewModelProvider(this)[QuestionDetailViewModel::class.java]
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    private val dbDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_edit)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_bar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        titleEditText = findViewById(R.id.question_edit_text)
        descriptionEditText = findViewById(R.id.answer_edit_text)
        categoryChipGroup = findViewById(R.id.category_chip_group)
        priorityChipGroup = findViewById(R.id.priority_chip_group)
        difficultyChipGroup = findViewById(R.id.difficulty_chip_group)
        datePickerButton = findViewById(R.id.date_picker_button)
        recurringSwitch = findViewById(R.id.recurring_switch)
        saveButton = findViewById(R.id.save_button)

        setupDatePicker()

        findViewById<Button>(R.id.cancel_button).setOnClickListener { finish() }
        saveButton.setOnClickListener { 
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            saveTask() 
        }

        questionId = intent.getLongExtra(EXTRA_QUESTION_ID, -1L)
        subjectId = intent.getLongExtra(QuestionActivity.EXTRA_SUBJECT_ID, -1L)
        
        if (subjectId != -1L) {
            toolbar.title = getString(R.string.edit_task)
            saveButton.text = getString(R.string.update_task)
            loadSubject(subjectId)
            
            questionDetailViewModel.getQuestions(subjectId).observe(this) { questions: List<Question>? ->
                if (!questions.isNullOrEmpty()) {
                    question = questions[0]
                }
            }
        } else {
            toolbar.title = getString(R.string.add_task)
            saveButton.text = getString(R.string.save_task)
        }

        if (questionId != -1L) {
            questionDetailViewModel.loadQuestion(questionId)
            questionDetailViewModel.questionLiveData.observe(this) { q: Question? ->
                if (q != null) {
                    question = q
                    if (descriptionEditText.text.isEmpty()) {
                        descriptionEditText.setText(q.answer)
                    }
                }
            }
        }
    }

    private fun loadSubject(id: Long) {
        questionDetailViewModel.getSubject(id).observe(this) { s: Subject? ->
            if (s != null) {
                subject = s
                titleEditText.setText(s.title)
                descriptionEditText.setText(s.description)
                
                // Set Priority Chip
                when (s.priority) {
                    Priority.LOW -> priorityChipGroup.check(R.id.chip_low)
                    Priority.MEDIUM -> priorityChipGroup.check(R.id.chip_medium)
                    Priority.HIGH -> priorityChipGroup.check(R.id.chip_high)
                }
                
                // Set Difficulty Chip
                val difficultyChipId = when (s.difficulty) {
                    1 -> R.id.chip_diff_1
                    2 -> R.id.chip_diff_2
                    3 -> R.id.chip_diff_3
                    4 -> R.id.chip_diff_4
                    5 -> R.id.chip_diff_5
                    else -> R.id.chip_diff_3
                }
                difficultyChipGroup.check(difficultyChipId)
                
                recurringSwitch.isChecked = s.isRecurring
                
                // Set Category Chip
                for (i in 0 until categoryChipGroup.childCount) {
                    val view = categoryChipGroup.getChildAt(i)
                    if (view is Chip && view.text.toString().equals(s.category, ignoreCase = true)) {
                        view.isChecked = true
                        break
                    }
                }

                s.dueDate?.let { dateStr ->
                    selectedDueDate = dateStr
                    try {
                        val localDate = LocalDate.parse(dateStr, dbDateFormatter)
                        datePickerButton.text = localDate.format(dateFormatter)
                    } catch (_: Exception) {
                        datePickerButton.text = dateStr
                    }
                }
            }
        }
    }

    private fun setupDatePicker() {
        datePickerButton.setOnClickListener {
            var selection = MaterialDatePicker.todayInUtcMilliseconds()
            
            selectedDueDate?.let {
                try {
                    selection = LocalDate.parse(it, dbDateFormatter)
                        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                } catch (_: Exception) {
                    // Fallback to today if parsing fails
                }
            }

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date_time)
                .setSelection(selection)
                .build()
            
            datePicker.show(supportFragmentManager, "DATE_PICKER")
            datePicker.addOnPositiveButtonClickListener { millis ->
                val localDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                selectedDueDate = localDate.format(dbDateFormatter)
                datePickerButton.text = localDate.format(dateFormatter)
            }
        }
    }

    private fun saveTask() {
        val title = titleEditText.text.toString().trim()
        val desc = descriptionEditText.text.toString().trim()

        if (title.isEmpty()) {
            titleEditText.error = getString(R.string.title_required)
            Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show()
            return
        }

        val priority = when (priorityChipGroup.checkedChipId) {
            R.id.chip_low -> Priority.LOW
            R.id.chip_high -> Priority.HIGH
            else -> Priority.MEDIUM
        }
        
        val difficulty = when (difficultyChipGroup.checkedChipId) {
            R.id.chip_diff_1 -> 1
            R.id.chip_diff_2 -> 2
            R.id.chip_diff_4 -> 4
            R.id.chip_diff_5 -> 5
            else -> 3
        }
        
        val selectedCategoryId = categoryChipGroup.checkedChipId
        val category = if (selectedCategoryId != -1) {
            findViewById<Chip>(selectedCategoryId)?.text?.toString() ?: "General"
        } else {
            "General"
        }

        questionDetailViewModel.viewModelScope.launch {
            try {
                if (subject == null) {
                    // ADD MODE
                    val newSubject = Subject(
                        title = title,
                        description = desc,
                        category = category,
                        priority = priority,
                        difficulty = difficulty,
                        dueDate = selectedDueDate,
                        isRecurring = recurringSwitch.isChecked,
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    val newId = questionDetailViewModel.addSubjectSync(newSubject)
                    
                    val newQuestion = Question(
                        text = title,
                        answer = desc,
                        subjectId = newId
                    )
                    questionDetailViewModel.addQuestion(newQuestion)
                    
                    Toast.makeText(this@QuestionEditActivity, getString(R.string.task_added_toast, title), Toast.LENGTH_SHORT).show()
                } else {
                    // EDIT MODE
                    subject?.let {
                        it.title = title
                        it.description = desc
                        it.category = category
                        it.priority = priority
                        it.difficulty = difficulty
                        it.dueDate = selectedDueDate
                        it.isRecurring = recurringSwitch.isChecked
                        it.updatedAt = System.currentTimeMillis()
                        questionDetailViewModel.updateSubject(it)
                    }
                    
                    question.text = title
                    question.answer = desc
                    question.subjectId = subject?.id ?: subjectId
                    
                    if (question.id == 0L) {
                        questionDetailViewModel.addQuestion(question)
                    } else {
                        questionDetailViewModel.updateQuestion(question)
                    }
                    
                    Toast.makeText(this@QuestionEditActivity, R.string.task_updated, Toast.LENGTH_SHORT).show()
                }
                
                setResult(RESULT_OK)
                finish()
            } catch (_: Exception) {
                Toast.makeText(this@QuestionEditActivity, R.string.error_saving_quest, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_QUESTION_ID = "com.zybooks.studyhelper.question_id"
    }
}
