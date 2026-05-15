package com.zybooks.studyhelper3

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.imageview.ShapeableImageView
import com.zybooks.studyhelper3.viewmodel.StatsViewModel
import java.util.Locale

/**
 * TaskMaster Stats Activity - Gamified Version
 * Redesigned as a Player Profile with Quest Stats and Achievement Inventory.
 */
class StatsActivity : AppCompatActivity() {

    private lateinit var productivityScoreText: TextView
    private lateinit var tasksCompletedText: TextView
    private lateinit var focusTimeText: TextView
    private lateinit var sessionsCountText: TextView
    private lateinit var streakCountText: TextView
    private lateinit var dailyGoalProgress: ProgressBar
    private lateinit var weeklyGoalProgress: ProgressBar
    
    private lateinit var badgeFirstQuest: ShapeableImageView
    private lateinit var badgeStreak: ShapeableImageView
    private lateinit var badgeFocusWarrior: ShapeableImageView
    private lateinit var badgeBossSlayer: ShapeableImageView

    private val statsViewModel: StatsViewModel by lazy {
        ViewModelProvider(this)[StatsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stats)
        
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
        productivityScoreText = findViewById(R.id.productivity_score_text)
        tasksCompletedText = findViewById(R.id.tasks_completed_count)
        focusTimeText = findViewById(R.id.total_focus_time_text)
        sessionsCountText = findViewById(R.id.sessions_count)
        streakCountText = findViewById(R.id.streak_count)
        dailyGoalProgress = findViewById(R.id.daily_goal_progress)
        weeklyGoalProgress = findViewById(R.id.weekly_goal_progress)
        
        badgeFirstQuest = findViewById(R.id.badge_first_quest)
        badgeStreak = findViewById(R.id.badge_streak)
        badgeFocusWarrior = findViewById(R.id.badge_focus_warrior)
        badgeBossSlayer = findViewById(R.id.badge_boss_slayer)

        setupObservers()
    }

    private fun setupObservers() {
        statsViewModel.productivityScore.observe(this) { score ->
            productivityScoreText.text = String.format(Locale.getDefault(), "%d%%", score)
        }

        statsViewModel.completedTasks.observe(this) { count ->
            tasksCompletedText.text = count.toString()
            
            // Unlock Achievements
            if (count >= 1) {
                unlockBadge(badgeFirstQuest)
            }
            if (count >= 10) {
                unlockBadge(badgeBossSlayer)
            }
            
            // Update Boss Goals (Mock thresholds: Daily 5, Weekly 20)
            val dailyProgress = (count.toFloat() / 5 * 100).toInt().coerceAtMost(100)
            dailyGoalProgress.progress = dailyProgress
            
            val weeklyProgress = (count.toFloat() / 20 * 100).toInt().coerceAtMost(100)
            weeklyGoalProgress.progress = weeklyProgress
        }

        statsViewModel.totalTimeMs.observe(this) { totalMs ->
            val ms = totalMs ?: 0L
            val hours = ms / (1000 * 60 * 60)
            val minutes = (ms / (1000 * 60)) % 60
            
            if (hours > 0) {
                focusTimeText.text = String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
            } else {
                focusTimeText.text = String.format(Locale.getDefault(), "%dm", minutes)
            }

            // Unlock Focus Warrior (1 hour focused)
            if (hours >= 1) {
                unlockBadge(badgeFocusWarrior)
            }
        }
        
        statsViewModel.streakCount.observe(this) { streak ->
            streakCountText.text = String.format(Locale.getDefault(), "%d Days", streak)
            if (streak >= 3) {
                unlockBadge(badgeStreak)
            }
        }
        
        // Mocking sessions count as it's not yet in the DB repo
        sessionsCountText.text = "12"
    }

    private fun unlockBadge(badge: ShapeableImageView) {
        badge.setBackgroundResource(R.drawable.badge_unlocked_bg)
    }
}
