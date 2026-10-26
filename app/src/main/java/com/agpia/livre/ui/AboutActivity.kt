package com.agpia.livre.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.agpia.livre.BuildConfig
import com.agpia.livre.R
import com.agpia.livre.data.BookRepository
import com.agpia.livre.databinding.ActivityAboutBinding
import com.agpia.livre.utils.applySystemBarInsets
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** « À propos » : versions, sources des textes, police copte, droits et confidentialité. */
class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.scrollView.applySystemBarInsets(bottom = true)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.about)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.textViewAppVersion.text =
            getString(R.string.about_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        val manifest = BookRepository.manifest
        binding.textViewContentVersion.text =
            getString(R.string.about_content_version, manifest.contentVersion, manifest.updatedAt)

        binding.textViewSources.setHtml(R.string.about_sources_text)
        binding.textViewFont.setHtml(R.string.about_font_text)
        binding.textViewRights.setHtml(R.string.about_rights_text)
        binding.buttonLicense.setOnClickListener { showLicense() }
    }

    private fun TextView.setHtml(resId: Int) {
        text = HtmlCompat.fromHtml(getString(resId), HtmlCompat.FROM_HTML_MODE_COMPACT)
        movementMethod = LinkMovementMethod.getInstance()
    }

    private fun showLicense() {
        val text = try {
            assets.open(LICENSE_PATH).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Licence introuvable : $LICENSE_PATH", e)
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_font_title)
            .setMessage(text)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    companion object {
        private const val TAG = "AboutActivity"
        private const val LICENSE_PATH = "licenses/OFL-NotoSansCoptic.txt"
    }
}
