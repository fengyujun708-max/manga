package com.mangaverse.app.history.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.main.ui.MainActivity

class HistoryActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val intent = Intent(this, MainActivity::class.java).apply {
			putExtra(AppSettings.KEY_NAV_MAIN, "history")
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
		}
		startActivity(intent)
		finish()
	}
}
