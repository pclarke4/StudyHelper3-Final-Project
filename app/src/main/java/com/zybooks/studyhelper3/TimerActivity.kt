package com.zybooks.studyhelper3

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.zybooks.studyhelper3.model.Subject
import com.zybooks.studyhelper3.viewmodel.TimerViewModel
import java.util.Locale

/**
 * TaskMaster Focus Timer Activity
 * Phase 3: Pomodoro timer with automatic time tracking for the selected quest.
 * Polished: Updated UI to match Quest theme and improved active mission display.
 */
class TimerActivity : AppCompatActivity() {

    private lateinit var timerDisplay: TextView
    private lateinit var timerModeLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startButton: MaterialButton
    private lateinit var pauseButton: MaterialButton
    private lateinit var resetButton: MaterialButton
    private lateinit var backButton: MaterialButton
    private lateinit var sessionCountText: TextView
    private lateinit var activeTaskText: TextView
    private lateinit var activeTaskHint: TextView
    private lateinit var modeToggleGroup: MaterialButtonToggleGroup

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    
    private val FOCUS_TIME: Long = 25 * 60 * 1000
    private val SHORT_BREAK: Long = 5 * 60 * 1000

    private var currentTimeLimit: Long = FOCUS_TIME
    private var timeLeftInMillis: Long = FOCUS_TIME
    private var lastSavedTimeLeft: Long = FOCUS_TIME
    private var sessionsCompleted = 0
    private var subjectId: Long = -1L
    private var subject: Subject? = null

    private val timerViewModel: TimerViewModel by lazy {
        ViewModelProvider(this)[TimerViewModel::class.java]
    }

    private var lastClickTime: Long = 0

    companion object {
        const val EXTRA_SUBJECT_ID = "com.zybooks.studyhelper.subject_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timer)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_bar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        timerDisplay = findViewById(R.id.timer_display)
        timerModeLabel = findViewById(R.id.timer_mode_label)
        progressBar = findViewById(R.id.timer_progress_bar)
        startButton = findViewById(R.id.start_button)
        pauseButton = findViewById(R.id.pause_button)
        resetButton = findViewById(R.id.reset_button)
        backButton = findViewById(R.id.back_button_control)
        sessionCountText = findViewById(R.id.session_count_text)
        modeToggleGroup = findViewById(R.id.mode_toggle_group)
        activeTaskText = findViewById(R.id.active_task_label) 
        activeTaskHint = findViewById(R.id.active_task_hint)

        setupModeToggle()

        startButton.setOnClickListener { startTimer() }
        pauseButton.setOnClickListener { pauseTimer() }
        resetButton.setOnClickListener { resetTimer() }
        backButton.setOnClickListener { finish() }

        subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1L)
        if (subjectId != -1L) {
            timerViewModel.getSubject(subjectId).observe(this) { s ->
                subject = s
                if (s != null) {
                    activeTaskText.text = s.title
                    activeTaskHint.visibility = View.GONE
                } else {
                    activeTaskText.text = getString(R.string.no_quest_selected)
                    activeTaskHint.visibility = View.VISIBLE
                }
            }
        } else {
            activeTaskText.text = getString(R.string.no_quest_selected)
            activeTaskHint.visibility = View.VISIBLE
        }

        updateCountDownText()
        updateProgressBar()
        updateSessionText()
    }

    private fun setupModeToggle() {
        modeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                pauseTimer()
                when (checkedId) {
                    R.id.btn_focus -> {
                        currentTimeLimit = FOCUS_TIME
                        timerModeLabel.text = getString(R.string.focus_label)
                    }
                    R.id.btn_short_break -> {
                        currentTimeLimit = SHORT_BREAK
                        timerModeLabel.text = getString(R.string.break_label)
                    }
                    else -> {
                        currentTimeLimit = FOCUS_TIME
                        timerModeLabel.text = getString(R.string.focus_label)
                    }
                }
                timeLeftInMillis = currentTimeLimit
                lastSavedTimeLeft = timeLeftInMillis
                updateCountDownText()
                updateProgressBar()
            }
        }
    }

    private fun startTimer() {
        if (isTimerRunning) return
        
        lastSavedTimeLeft = timeLeftInMillis
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateCountDownText()
                updateProgressBar()
                
                // Periodic save to prevent data loss on crash (every 60s)
                if (lastSavedTimeLeft - timeLeftInMillis >= 60000) {
                    saveProgress()
                }
            }

            override fun onFinish() {
                isTimerRunning = false
                timeLeftInMillis = 0
                saveProgress()
                updateUIForTimerState()
                sessionsCompleted++
                updateSessionText()
                Toast.makeText(this@TimerActivity, R.string.session_finished, Toast.LENGTH_SHORT).show()
                resetTimer()
            }
        }.start()

        isTimerRunning = true
        updateUIForTimerState()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        isTimerRunning = false
        saveProgress()
        updateUIForTimerState()
    }

    private fun resetTimer() {
        pauseTimer()
        timeLeftInMillis = currentTimeLimit
        lastSavedTimeLeft = timeLeftInMillis
        updateCountDownText()
        updateProgressBar()
    }

    private fun saveProgress() {
        subject?.let {
            val elapsed = lastSavedTimeLeft - timeLeftInMillis
            if (elapsed >= 1000) { 
                it.timeSpentMs += elapsed
                it.updatedAt = System.currentTimeMillis()
                timerViewModel.updateSubject(it)
                lastSavedTimeLeft = timeLeftInMillis
            }
        }
    }

    private fun updateUIForTimerState() {
        startButton.visibility = if (isTimerRunning) View.GONE else View.VISIBLE
        pauseButton.visibility = if (isTimerRunning) View.VISIBLE else View.GONE
    }

    private fun updateCountDownText() {
        val totalSeconds = (timeLeftInMillis / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        timerDisplay.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun updateProgressBar() {
        if (currentTimeLimit > 0) {
            val progress = ((timeLeftInMillis.toFloat() / currentTimeLimit) * 1000).toInt()
            progressBar.progress = progress.coerceIn(0, 1000)
        } else {
            progressBar.progress = 0
        }
    }

    private fun updateSessionText() {
        sessionCountText.text = getString(R.string.sessions_completed_format, sessionsCompleted)
    }

    override fun onDestroy() {
        pauseTimer() 
        super.onDestroy()
    }
}
