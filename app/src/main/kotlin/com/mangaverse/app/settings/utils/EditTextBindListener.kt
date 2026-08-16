package com.mangaverse.app.settings.utils

import android.widget.EditText
import androidx.preference.EditTextPreference
import com.mangaverse.app.core.util.EditTextValidator

class EditTextBindListener(
	private val inputType: Int,
	private val hint: String?,
	private val validator: EditTextValidator?,
) : EditTextPreference.OnBindEditTextListener {

	override fun onBindEditText(editText: EditText) {
		editText.inputType = inputType
		editText.hint = hint
		validator?.attachToEditText(editText)
	}
}
