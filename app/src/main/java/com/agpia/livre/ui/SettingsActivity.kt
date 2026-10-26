package com.agpia.livre.ui

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.agpia.livre.BuildConfig
import com.agpia.livre.R
import com.agpia.livre.data.BookRepository
import com.agpia.livre.data.ContentUpdater
import com.agpia.livre.databinding.ActivitySettingsBinding
import com.agpia.livre.utils.PreferencesManager
import com.agpia.livre.utils.applySystemBarInsets
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
        setupContent()
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
        val effective = if (BookRepository.primaryLanguage == PreferencesManager.LANG_AR) size + 2f else size
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

    // ---- Langues : listes construites depuis le manifeste du contenu ----

    private fun setupLanguage() {
        val languages = BookRepository.languages
        val primary = BookRepository.primaryLanguage

        binding.dropdownPrimaryLanguage.setSimpleItems(languages.map { it.nativeName }.toTypedArray())
        binding.dropdownPrimaryLanguage.setText(
            languages.find { it.code == primary }?.nativeName ?: primary, false
        )
        binding.dropdownPrimaryLanguage.setOnItemClickListener { _, _, position, _ ->
            val code = languages[position].code
            if (code == BookRepository.primaryLanguage) return@setOnItemClickListener
            prefsManager.setPrimaryLanguage(code)
            setResult(RESULT_OK)
            // L'interface suit la langue principale : AppCompat recrée les activités si la locale change.
            if (!prefsManager.applyAppLocale(BookRepository.uiLocaleFor(code))) {
                refreshSecondaryList()
                updateFontPreview(binding.sliderFontSize.value)
            }
        }
        refreshSecondaryList()
    }

    /** « Aucune » + toutes les langues sauf la principale. */
    private fun refreshSecondaryList() {
        val primary = BookRepository.primaryLanguage
        val candidates = BookRepository.languages.filter { it.code != primary }
        val labels = listOf(getString(R.string.secondary_none)) + candidates.map { it.nativeName }
        binding.dropdownSecondaryLanguage.setSimpleItems(labels.toTypedArray())
        val current = BookRepository.secondaryLanguage
        binding.dropdownSecondaryLanguage.setText(
            candidates.find { it.code == current }?.nativeName ?: labels[0], false
        )
        binding.dropdownSecondaryLanguage.setOnItemClickListener { _, _, position, _ ->
            val code = if (position == 0) null else candidates[position - 1].code
            if (code != BookRepository.secondaryLanguage) {
                prefsManager.setSecondaryLanguage(code)
                setResult(RESULT_OK)
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

    // ---- Contenu : version, mises à jour, restauration, à propos ----

    private fun setupContent() {
        refreshContentInfo()
        binding.buttonCheckUpdates.setOnClickListener { checkUpdates() }
        binding.buttonRestoreEmbedded.setOnClickListener {
            ContentUpdater.restoreEmbedded(this)
            setResult(RESULT_OK)
            refreshContentInfo()
            showStatus(getString(R.string.content_restored))
        }
        binding.buttonAbout.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }
        if (BuildConfig.DEBUG) {
            // Debug : appui long sur la version pour surcharger l'URL de contenu (serveur local de test).
            binding.textViewContentVersion.setOnLongClickListener { showUrlOverrideDialog(); true }
        }
    }

    private fun refreshContentInfo() {
        val manifest = BookRepository.manifest
        val source = getString(
            if (BookRepository.source == BookRepository.Source.DOWNLOADED) R.string.content_source_downloaded
            else R.string.content_source_embedded
        )
        binding.textViewContentVersion.text =
            getString(R.string.content_version, manifest.contentVersion, manifest.updatedAt, source)
        binding.buttonRestoreEmbedded.isEnabled = BookRepository.source == BookRepository.Source.DOWNLOADED
    }

    private fun checkUpdates() {
        binding.buttonCheckUpdates.isEnabled = false
        showStatus(getString(R.string.update_checking))
        ContentUpdater.check(this) { result ->
            if (isDestroyed) return@check
            binding.buttonCheckUpdates.isEnabled = true
            val message = when (result) {
                ContentUpdater.Result.UpToDate -> getString(R.string.update_up_to_date)
                is ContentUpdater.Result.Updated -> {
                    setResult(RESULT_OK)
                    getString(R.string.update_installed, result.version)
                }
                is ContentUpdater.Result.Failed ->
                    getString(if (result.network) R.string.update_network_error else R.string.update_invalid)
            }
            refreshContentInfo()
            showStatus(message)
        }
    }

    private fun showStatus(message: String) {
        binding.textViewUpdateStatus.text = message
        binding.textViewUpdateStatus.isVisible = true
    }

    private fun showUrlOverrideDialog() {
        val input = EditText(this).apply {
            setText(prefsManager.getContentBaseUrlOverride().orEmpty())
            hint = getString(R.string.content_url_override_hint)
            setSingleLine()
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.content_url_override_title)
            .setMessage(ContentUpdater.baseUrl(this))
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                prefsManager.setContentBaseUrlOverride(input.text?.toString())
                showStatus(ContentUpdater.baseUrl(this))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
