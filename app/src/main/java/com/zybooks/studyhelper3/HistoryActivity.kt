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
import android.widget.LinearLayout
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
import com.zybooks.studyhelper3.model.Priority
import com.zybooks.studyhelper3.model.Subject
import com.zybooks.studyhelper3.viewmodel.HistoryViewModel

/**
 * TaskMaster History Activity
 * Phase 3: Displays a list of completed tasks and a progress summary.
 * Fixed: Dark mode support for priority badges.
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var weeklyCountText: TextView
    private lateinit var totalTasksText: TextView
    private lateinit var completedTasksText: TextView
    private lateinit var incompleteTasksText: TextView
    private lateinit var completionPercentText: TextView
    private lateinit var streakCountText: TextView
    private lateinit var streakLayout: LinearLayout
    private var historyAdapter = HistoryAdapter(mutableListOf())

    private val historyViewModel: HistoryViewModel by lazy {
        ViewModelProvider(this)[HistoryViewModel::class.java]
    }

    private var lastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_bar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Initialize Summary Views
        historyRecyclerView = findViewById(R.id.history_recycler_view)
        emptyStateLayout = findViewById(R.id.empty_state_layout)
        weeklyCountText = findViewById(R.id.weekly_count_text)
        totalTasksText = findViewById(R.id.total_tasks_text)
        completedTasksText = findViewById(R.id.completed_tasks_count_text)
        incompleteTasksText = findViewById(R.id.incomplete_tasks_text)
        completionPercentText = findViewById(R.id.completion_percent_text)
        streakCountText = findViewById(R.id.streak_count_text)
        streakLayout = findViewById(R.id.streak_layout)

        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        
        setupObservers()
    }

    private fun setupObservers() {
        // Observe the filtered list of completed tasks
        historyViewModel.completedSubjectsLiveData.observe(this) { subjects ->
            updateListUI(subjects ?: emptyList())
        }

        // Observe overall stats for the progress summary
        historyViewModel.totalTasks.observe(this) { total ->
            totalTasksText.text = (total ?: 0).toString()
        }

        historyViewModel.completedCount.observe(this) { completed ->
            completedTasksText.text = (completed ?: 0).toString()
        }

        historyViewModel.incompleteCount.observe(this) { incomplete ->
            incompleteTasksText.text = (incomplete ?: 0).toString()
        }

        historyViewModel.completionPercentage.observe(this) { percentage ->
            completionPercentText.text = getString(R.string.progress_format, percentage ?: 0)
        }

        historyViewModel.streakCount.observe(this) { streak ->
            val count = streak ?: 0
            if (count > 0) {
                streakLayout.visibility = View.VISIBLE
                streakCountText.text = getString(R.string.mock_streak_count).replace("5", count.toString())
            } else {
                streakLayout.visibility = View.GONE
            }
        }
    }

    private fun updateListUI(subjects: List<Subject>) {
        if (subjects.isEmpty()) {
            historyRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
            weeklyCountText.text = getString(R.string.no_completed_tasks)
        } else {
            historyRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            weeklyCountText.text = getString(R.string.history_summary, subjects.size)
            
            historyAdapter = HistoryAdapter(subjects)
            historyRecyclerView.adapter = historyAdapter
        }
    }

    private inner class HistoryHolder(inflater: LayoutInflater, parent: ViewGroup?) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.recycler_view_items, parent, false)),
        View.OnClickListener {

        private var subject: Subject? = null
        private val subjectTextView: TextView = itemView.findViewById(R.id.subject_text_view)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.task_due_date)
        private val timeSpentTextView: TextView = itemView.findViewById(R.id.task_time_spent)
        private val priorityLabel: TextView = itemView.findViewById(R.id.priority_label)
        private val checkBox: CheckBox = itemView.findViewById(R.id.task_checkbox)

        init {
            itemView.setOnClickListener(this)
            checkBox.setOnClickListener {
                subject?.let {
                    it.isCompleted = checkBox.isChecked
                    it.updatedAt = System.currentTimeMillis()
                    if (!it.isCompleted) {
                        it.completionDate = null
                    }
                    // If unchecked, it's no longer completed, update UI immediately
                    historyViewModel.updateSubject(it)
                }
            }
        }

        fun bind(subject: Subject) {
            this.subject = subject
            subjectTextView.text = subject.title
            checkBox.isChecked = subject.isCompleted
            
            // Visual feedback: Strike-through for completed text
            subjectTextView.paintFlags = subjectTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            dueDateTextView.visibility = View.GONE

            // Priority Badge
            priorityLabel.visibility = View.VISIBLE
            priorityLabel.text = when(subject.priority) {
                Priority.HIGH -> getString(R.string.priority_high)
                Priority.MEDIUM -> getString(R.string.priority_medium)
                Priority.LOW -> getString(R.string.priority_low)
            }
            val priorityColor = when (subject.priority) {
                Priority.HIGH -> ContextCompat.getColor(this@HistoryActivity, R.color.priority_high)
                Priority.MEDIUM -> ContextCompat.getColor(this@HistoryActivity, R.color.priority_medium)
                Priority.LOW -> ContextCompat.getColor(this@HistoryActivity, R.color.priority_low)
            }
            priorityLabel.backgroundTintList = ColorStateList.valueOf(priorityColor)

            // Show total time focused on this task
            if (subject.timeSpentMs > 0) {
                val minutes = (subject.timeSpentMs / 1000) / 60
                timeSpentTextView.visibility = View.VISIBLE
                timeSpentTextView.text = getString(R.string.focused_time_format, minutes.toInt())
            } else {
                timeSpentTextView.visibility = View.GONE
            }
        }

        override fun onClick(view: View) {
            if (System.currentTimeMillis() - lastClickTime < 500) return
            lastClickTime = System.currentTimeMillis()
            
            subject?.let {
                val intent = Intent(this@HistoryActivity, QuestionActivity::class.java)
                intent.putExtra(QuestionActivity.EXTRA_SUBJECT_ID, it.id)
                intent.putExtra(QuestionActivity.EXTRA_SUBJECT_TEXT, it.title)
                startActivity(intent)
            }
        }
    }

    private inner class HistoryAdapter(private val subjectList: List<Subject>) :
        RecyclerView.Adapter<HistoryHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
            val layoutInflater = LayoutInflater.from(this@HistoryActivity)
            return HistoryHolder(layoutInflater, parent)
        }

        override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
            if (position < subjectList.size) {
                holder.bind(subjectList[position])
            }
        }

        override fun getItemCount(): Int = subjectList.size
    }
}
