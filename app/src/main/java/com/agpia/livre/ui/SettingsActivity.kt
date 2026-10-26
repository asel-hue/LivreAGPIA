package com.agpia.livre.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.agpia.livre.R
import com.agpia.livre.databinding.ActivitySettingsBinding
import com.agpia.livre.utils.PreferencesManager
import com.agpia.livre.utils.applySystemBarInsets

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.scrollView.applySystemBarInsets(bottom = true)

        prefsManager = PreferencesManager(this)

        setupToolbar()
        setupFontSize()
        setupTitleAlignment()
        setupLanguage()
        setupTheme()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupFontSize() {
        binding.sliderFontSize.valueFrom = PreferencesManager.MIN_FONT_SIZE
        binding.sliderFontSize.valueTo = PreferencesManager.MAX_FONT_SIZE
        binding.sliderFontSize.stepSize = PreferencesManager.FONT_SIZE_STEP
        binding.sliderFontSize.setLabelFormatter { value -> getString(R.string.font_size_value, value.toInt()) }

        updateFontPreview(prefsManager.getFontSize())
        binding.sliderFontSize.value = prefsManager.getFontSize()

        binding.sliderFontSize.addOnChangeListener { _, value, fromUser ->
            updateFontPreview(value)
            if (fromUser) {
                prefsManager.setFontSize(value)
                setResult(RESULT_OK)
            }
        }
    }

    private fun updateFontPreview(size: Float) {
        binding.textViewFontSizeValue.text = getString(R.string.font_size_value, size.toInt())
        // Même règle que l'écran de lecture : l'arabe est affiché un peu plus grand.
        val effective = if (prefsManager.isArabic()) size + 2f else size
        binding.textViewFontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, effective)
        // Même règle que SectionAdapter : titre = corps + 2 sp.
        binding.textViewPreviewTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, effective + 2f)
    }

    private fun setupTitleAlignment() {
        applyPreviewTitleAlignment()
        binding.toggleTitleAlignment.check(
            if (prefsManager.isSectionTitleCentered()) R.id.buttonAlignCenter else R.id.buttonAlignStart
        )
        binding.toggleTitleAlignment.addOnButtonCheckedListener { _, buttonId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val alignment = if (buttonId == R.id.buttonAlignCenter) PreferencesManager.ALIGN_CENTER
            else PreferencesManager.ALIGN_START
            if (alignment != prefsManager.getSectionTitleAlignment()) {
                prefsManager.setSectionTitleAlignment(alignment)
                applyPreviewTitleAlignment()
                setResult(RESULT_OK)
            }
        }
    }

    private fun applyPreviewTitleAlignment() {
        binding.textViewPreviewTitle.textAlignment =
            if (prefsManager.isSectionTitleCentered()) View.TEXT_ALIGNMENT_CENTER
            else View.TEXT_ALIGNMENT_VIEW_START
    }

    private fun setupLanguage() {
        binding.toggleLanguage.check(
            when (prefsManager.getContentLanguage()) {
                PreferencesManager.LANG_AR -> R.id.buttonLanguageArabic
                PreferencesManager.LANG_BOTH -> R.id.buttonLanguageBoth
                else -> R.id.buttonLanguageFrench
            }
        )

        binding.toggleLanguage.addOnButtonCheckedListener { _, buttonId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val language = when (buttonId) {
                R.id.buttonLanguageArabic -> PreferencesManager.LANG_AR
                R.id.buttonLanguageBoth -> PreferencesManager.LANG_BOTH
                else -> PreferencesManager.LANG_FR
            }
            if (language != prefsManager.getContentLanguage()) {
                prefsManager.setContentLanguage(language)
                setResult(RESULT_OK)
                // L'interface suit la langue du contenu (arabe seul → arabe, sinon français) :
                // AppCompat recrée les activités si la locale change.
                if (!prefsManager.applyAppLocale()) updateFontPreview(binding.sliderFontSize.value)
            }
        }
    }

    private fun setupTheme() {
        val checkedId = when (prefsManager.getThemeMode()) {
            PreferencesManager.THEME_LIGHT -> R.id.buttonThemeLight
            PreferencesManager.THEME_DARK -> R.id.buttonThemeDark
            else -> R.id.buttonThemeSystem
        }
        binding.toggleTheme.check(checkedId)

        binding.toggleTheme.addOnButtonCheckedListener { _, buttonId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (buttonId) {
                R.id.buttonThemeLight -> PreferencesManager.THEME_LIGHT
                R.id.buttonThemeDark -> PreferencesManager.THEME_DARK
                else -> PreferencesManager.THEME_SYSTEM
            }
            if (mode != prefsManager.getThemeMode()) {
                prefsManager.setThemeMode(mode)
                setResult(RESULT_OK)
                // AppCompat recrée automatiquement les activités concernées.
                prefsManager.applyNightMode()
            }
        }
    }
}
