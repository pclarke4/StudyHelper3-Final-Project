package com.zybooks.studyhelper3

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.zybooks.studyhelper3.model.Priority
import com.zybooks.studyhelper3.model.Subject
import com.zybooks.studyhelper3.viewmodel.SubjectFilter
import com.zybooks.studyhelper3.viewmodel.SubjectListViewModel
import com.zybooks.studyhelper3.viewmodel.SubjectSortOrder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * TaskMaster Home/Dashboard Activity
 * Gamified Version: XP, Levels, and Quest Ranks.
 * Fixed: Added null safety, click debouncing, and robust date parsing.
 * Fixed: Dark mode support for task cards and priority badges.
 * Fixed: Inflation crash by using correct themed context in Adapter.
 */
class SubjectActivity : AppCompatActivity(),
    SubjectDialogFragment.OnSubjectEnteredListener {

    private var subjectAdapter = SubjectAdapter(mutableListOf())
    private lateinit var subjectRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var levelText: TextView
    private lateinit var rankTitleText: TextView
    private lateinit var searchEditText: EditText
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var sortChipGroup: ChipGroup

    private val subjectListViewModel: SubjectListViewModel by lazy {
        ViewModelProvider(this)[SubjectListViewModel::class.java]
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())
    private val dbDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private var lastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        applySettings()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_subject)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        progressBar = findViewById(R.id.daily_progress_bar)
        progressText = findViewById(R.id.progress_percentage_text)
        levelText = findViewById(R.id.level_text)
        rankTitleText = findViewById(R.id.rank_title_text)
        searchEditText = findViewById(R.id.search_edit_text)
        filterChipGroup = findViewById(R.id.filter_chip_group)
        sortChipGroup = findViewById(R.id.sort_chip_group)

        findViewById<ExtendedFloatingActionButton>(R.id.add_subject_button).setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            val intent = Intent(this, QuestionEditActivity::class.java)
            startActivity(intent)
        }

        setupNavigation()
        setupSearch()
        setupFilters()
        setupSorting()

        subjectRecyclerView = findViewById(R.id.subject_recycler_view)
        subjectRecyclerView.layoutManager = LinearLayoutManager(this)
        subjectRecyclerView.adapter = subjectAdapter

        subjectListViewModel.subjectListLiveData.observe(this) { subjectsList ->
            updateUI(subjectsList ?: emptyList())
        }

        subjectListViewModel.progressPercentage.observe(this) { percentage ->
            updateProgressUI(percentage ?: 0)
        }
        
        subjectListViewModel.totalSubjects.observe(this) { total ->
            val count = total ?: 0
            val level = (count / 5) + 1
            levelText.text = getString(R.string.level_format, level)
        }
    }

    private fun applySettings() {
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                subjectListViewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.filter_active -> SubjectFilter.INCOMPLETE
                R.id.filter_completed -> SubjectFilter.COMPLETED
                R.id.filter_high_priority -> SubjectFilter.HIGH_PRIORITY
                else -> SubjectFilter.ALL
            }
            subjectListViewModel.setFilter(filter)
        }
    }

    private fun setupSorting() {
        sortChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val sortOrder = when (checkedIds.firstOrNull()) {
                R.id.sort_priority -> SubjectSortOrder.PRIORITY
                R.id.sort_difficulty -> SubjectSortOrder.DIFFICULTY
                R.id.sort_alpha -> SubjectSortOrder.ALPHABETIC
                else -> SubjectSortOrder.NEW_FIRST
            }
            subjectListViewModel.setSortOrder(sortOrder)
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.nav_add_task).setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            startActivity(Intent(this, QuestionEditActivity::class.java))
        }
        findViewById<View>(R.id.nav_timer).setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            startActivity(Intent(this, TimerActivity::class.java))
        }
        findViewById<View>(R.id.nav_history).setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<View>(R.id.nav_stats).setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            startActivity(Intent(this, StatsActivity::class.java))
        }
        findViewById<View>(R.id.nav_calendar).setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        findViewById<View>(R.id.nav_settings).setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < 500) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateUI(subjectList: List<Subject>) {
        subjectAdapter.updateSubjects(subjectList)
    }

    private fun updateProgressUI(percentage: Int) {
        progressBar.progress = percentage
        progressText.text = getString(R.string.xp_format, percentage)
        
        rankTitleText.text = when {
            percentage <= 20 -> getString(R.string.rank_rookie)
            percentage <= 40 -> getString(R.string.rank_scout)
            percentage <= 60 -> getString(R.string.rank_grinder)
            percentage <= 80 -> getString(R.string.rank_knight)
            else -> getString(R.string.rank_legend)
        }
    }

    override fun onSubjectEntered(subjectText: String) {
        if (subjectText.trim().isNotEmpty()) {
            val subject = Subject(title = subjectText.trim())
            subjectListViewModel.addSubject(subject)
            Toast.makeText(this, getString(R.string.task_added_toast, subjectText), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show()
        }
    }

    private inner class SubjectHolder(inflater: LayoutInflater, parent: ViewGroup?) :
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
            checkBox.setOnClickListener {
                subject?.let {
                    it.isCompleted = checkBox.isChecked
                    it.updatedAt = System.currentTimeMillis()
                    if (it.isCompleted) {
                        it.completionDate = System.currentTimeMillis()
                    } else {
                        it.completionDate = null
                    }
                    subjectListViewModel.updateSubject(it)
                    if (it.isCompleted) {
                        Toast.makeText(this@SubjectActivity, R.string.task_completed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        fun bind(subject: Subject) {
            this.subject = subject
            subjectTextView.text = subject.title
            categoryTextView.text = subject.category
            difficultyTextView.text = getString(R.string.difficulty_level_format, subject.difficulty)
            checkBox.isChecked = subject.isCompleted
            
            // Priority Badge
            priorityLabel.text = subject.priority.name
            val priorityColor = when (subject.priority) {
                Priority.HIGH -> ContextCompat.getColor(this@SubjectActivity, R.color.priority_high)
                Priority.MEDIUM -> ContextCompat.getColor(this@SubjectActivity, R.color.priority_medium)
                Priority.LOW -> ContextCompat.getColor(this@SubjectActivity, R.color.priority_low)
            }
            priorityLabel.backgroundTintList = ColorStateList.valueOf(priorityColor)

            if (subject.isCompleted) {
                subjectTextView.paintFlags = subjectTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                dueDateTextView.visibility = View.GONE
                timeSpentTextView.visibility = View.GONE
                priorityLabel.visibility = View.GONE
            } else {
                subjectTextView.paintFlags = subjectTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                dueDateTextView.visibility = View.VISIBLE
                priorityLabel.visibility = View.VISIBLE
                
                if (subject.dueDate != null) {
                    try {
                        val localDate = LocalDate.parse(subject.dueDate, dbDateFormatter)
                        dueDateTextView.text = localDate.format(dateFormatter)
                    } catch (e: Exception) {
                        dueDateTextView.text = subject.dueDate
                    }
                } else {
                    dueDateTextView.text = getString(R.string.due_today)
                }
                
                if (subject.timeSpentMs > 0) {
                    val minutes = (subject.timeSpentMs / 1000) / 60
                    if (minutes > 0) {
                        timeSpentTextView.visibility = View.VISIBLE
                        timeSpentTextView.text = getString(R.string.focused_time_format, minutes.toInt())
                    } else {
                        timeSpentTextView.visibility = View.GONE
                    }
                } else {
                    timeSpentTextView.visibility = View.GONE
                }
            }
        }

        override fun onClick(view: View) {
            if (System.currentTimeMillis() - lastClickTime < 500) return
            lastClickTime = System.currentTimeMillis()
            
            subject?.let {
                val intent = Intent(this@SubjectActivity, QuestionActivity::class.java)
                intent.putExtra(QuestionActivity.EXTRA_SUBJECT_ID, it.id)
                intent.putExtra(QuestionActivity.EXTRA_SUBJECT_TEXT, it.title)
                startActivity(intent)
            }
        }
    }

    private inner class SubjectAdapter(private var subjectList: MutableList<Subject>) :
        RecyclerView.Adapter<SubjectHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return SubjectHolder(layoutInflater, parent)
        }

        override fun onBindViewHolder(holder: SubjectHolder, position: Int) {
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
