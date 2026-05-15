package com.zybooks.studyhelper3

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.zybooks.studyhelper3.model.Priority
import com.zybooks.studyhelper3.model.Subject
import com.zybooks.studyhelper3.viewmodel.CalendarViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * TaskMaster Calendar Activity
 * Phase 3: Displays tasks filtered by due date using a horizontal date picker.
 * Polished: Added Quest-themed colors to day chips and improved empty state.
 */
class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var chipGroup: ChipGroup
    private lateinit var selectedDateText: TextView
    private lateinit var emptyStateLayout: View
    private var calendarAdapter = CalendarAdapter(mutableListOf())

    private val calendarViewModel: CalendarViewModel by lazy {
        ViewModelProvider(this)[CalendarViewModel::class.java]
    }

    private val displayFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd", Locale.getDefault())
    private val chipFormatter = DateTimeFormatter.ofPattern("EEE\ndd", Locale.getDefault())

    private var lastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_bar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        calendarRecyclerView = findViewById(R.id.calendar_recycler_view)
        chipGroup = findViewById(R.id.calendar_chip_group)
        selectedDateText = findViewById(R.id.selected_date_text)
        emptyStateLayout = findViewById(R.id.empty_state_layout)

        calendarRecyclerView.layoutManager = LinearLayoutManager(this)
        calendarRecyclerView.adapter = calendarAdapter
        
        setupDatePicker()
        setupObservers()
    }

    private fun setupDatePicker() {
        var date = LocalDate.now()
        
        // Color states for Quest theme
        val chipStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                ContextCompat.getColor(this, R.color.theme_primary),
                ContextCompat.getColor(this, R.color.quest_card_bg)
            )
        )
        
        val textStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.theme_primary)
            )
        )

        // Generate chips for the next 14 days
        for (i in 0 until 14) {
            val currentDate = date
            val timestamp = currentDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            
            val chip = Chip(this)
            chip.text = currentDate.format(chipFormatter)
            chip.isCheckable = true
            chip.isChecked = (i == 0)
            chip.chipBackgroundColor = chipStates
            chip.setTextColor(textStates)
            chip.chipStrokeWidth = 2f
            chip.chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.theme_primary))
            
            chip.setOnClickListener {
                calendarViewModel.selectDate(timestamp)
            }
            
            chipGroup.addView(chip)
            date = date.plusDays(1)
        }
    }

    private fun setupObservers() {
        calendarViewModel.selectedDate.observe(this) { timestamp ->
            timestamp?.let {
                val localDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                selectedDateText.text = localDate.format(displayFormatter)
            }
        }

        calendarViewModel.subjectsForDate.observe(this) { subjects ->
            updateUI(subjects ?: emptyList())
        }
    }

    private fun updateUI(subjects: List<Subject>) {
        if (subjects.isEmpty()) {
            calendarRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            calendarRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            calendarAdapter.updateSubjects(subjects)
        }
    }

    private inner class CalendarHolder(inflater: LayoutInflater, parent: ViewGroup?) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.recycler_view_items, parent, false)),
        View.OnClickListener {

        private var subject: Subject? = null
        private val subjectTextView: TextView = itemView.findViewById(R.id.subject_text_view)
        private val categoryTextView: TextView = itemView.findViewById(R.id.category_label)
        private val difficultyTextView: TextView = itemView.findViewById(R.id.difficulty_label)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.task_due_date)
        private val timeSpentTextView: TextView = itemView.findViewById(R.id.task_time_spent)
        private val priorityLabel: TextView = itemView.findViewById(R.id.priority_label)
        private val checkBox: CheckBox = itemView.findViewById(R.id.task_checkbox)

        init {
            itemView.setOnClickListener(this)
            checkBox.visibility = View.GONE
        }

        fun bind(subject: Subject) {
            this.subject = subject
            subjectTextView.text = subject.title
            categoryTextView.text = subject.category
            difficultyTextView.text = getString(R.string.difficulty_level_format, subject.difficulty)
            
            priorityLabel.text = when(subject.priority) {
                Priority.HIGH -> getString(R.string.priority_high)
                Priority.MEDIUM -> getString(R.string.priority_medium)
                Priority.LOW -> getString(R.string.priority_low)
            }
            
            val priorityColor = when (subject.priority) {
                Priority.HIGH -> ContextCompat.getColor(this@CalendarActivity, R.color.priority_high)
                Priority.MEDIUM -> ContextCompat.getColor(this@CalendarActivity, R.color.priority_medium)
                Priority.LOW -> ContextCompat.getColor(this@CalendarActivity, R.color.priority_low)
            }
            priorityLabel.backgroundTintList = ColorStateList.valueOf(priorityColor)
            priorityLabel.visibility = View.VISIBLE

            if (subject.isCompleted) {
                subjectTextView.paintFlags = subjectTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                subjectTextView.paintFlags = subjectTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            dueDateTextView.visibility = View.GONE
            
            if (subject.timeSpentMs > 0) {
                val mins = (subject.timeSpentMs / 1000) / 60
                timeSpentTextView.visibility = View.VISIBLE
                timeSpentTextView.text = getString(R.string.focused_time_format, mins.toInt())
            } else {
                timeSpentTextView.visibility = View.GONE
            }
        }

        override fun onClick(view: View) {
            if (System.currentTimeMillis() - lastClickTime < 500) return
            lastClickTime = System.currentTimeMillis()
            
            subject?.let {
                val intent = Intent(this@CalendarActivity, QuestionActivity::class.java)
                intent.putExtra(QuestionActivity.EXTRA_SUBJECT_ID, it.id)
                intent.putExtra(QuestionActivity.EXTRA_SUBJECT_TEXT, it.title)
                startActivity(intent)
            }
        }
    }

    private inner class CalendarAdapter(private val subjectList: MutableList<Subject>) :
        RecyclerView.Adapter<CalendarHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarHolder {
            val layoutInflater = LayoutInflater.from(this@CalendarActivity)
            return CalendarHolder(layoutInflater, parent)
        }

        override fun onBindViewHolder(holder: CalendarHolder, position: Int) {
            if (position < subjectList.size) {
                holder.bind(subjectList[position])
            }
        }

        override fun getItemCount(): Int = subjectList.size

        fun updateSubjects(newSubjects: List<Subject>) {
            subjectList.clear()
            subjectList.addAll(newSubjects)
            notifyDataSetChanged()
        }
    }
}
