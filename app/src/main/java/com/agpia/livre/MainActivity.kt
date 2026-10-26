package com.agpia.livre

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.agpia.livre.data.BILINGUAL_SEPARATOR
import com.agpia.livre.data.BookRepository
import com.agpia.livre.data.ContentUpdater
import com.agpia.livre.databinding.ActivityMainBinding
import com.agpia.livre.ui.AboutActivity
import com.agpia.livre.ui.BookmarksActivity
import com.agpia.livre.ui.ChapterAdapter
import com.agpia.livre.ui.SearchActivity
import com.agpia.livre.ui.SettingsActivity
import com.agpia.livre.utils.PreferencesManager
import com.agpia.livre.utils.applySystemBarInsets

/** Accueil : liste des heures de prière + reprise de la dernière lecture. */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var chapterAdapter: ChapterAdapter
    private lateinit var prefsManager: PreferencesManager

    /** Recherche et marque-pages renvoient une position (heure, partie) à ouvrir. */
    private val navigateLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val chapterNum = data.getIntExtra(ReadingActivity.EXTRA_CHAPTER_NUMBER, -1)
                if (chapterNum != -1) {
                    openReading(chapterNum, data.getIntExtra(ReadingActivity.EXTRA_VERSE_NUMBER, -1))
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerViewChapters.applySystemBarInsets(bottom = true)

        prefsManager = PreferencesManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.home_title)

        setupRecyclerView()
        binding.cardResume.setOnClickListener { openReading(prefsManager.getLastChapter()) }

        // Vérification silencieuse des mises à jour du contenu, en arrière-plan (au plus une fois par semaine).
        // Rien n'est affiché : un nouveau contenu est pris en compte au prochain chargement d'une heure.
        ContentUpdater.checkIfDue(this)
    }

    override fun onResume() {
        super.onResume()
        // Titres dans les langues choisies (peuvent avoir changé depuis les paramètres).
        refreshChapters()
    }

    private fun refreshChapters() {
        chapterAdapter.submitList(BookRepository.getChapters())
        val last = BookRepository.getChapter(prefsManager.getLastChapter())
        binding.textViewResumeTitle.text = last?.title?.replace(BILINGUAL_SEPARATOR, "\n") ?: ""
    }

    private fun setupRecyclerView() {
        chapterAdapter = ChapterAdapter { chapter -> openReading(chapter.number) }
        binding.recyclerViewChapters.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chapterAdapter
        }
    }

    private fun openReading(chapterNumber: Int, sectionId: Int = -1) {
        val intent = Intent(this, ReadingActivity::class.java).apply {
            putExtra(ReadingActivity.EXTRA_CHAPTER_NUMBER, chapterNumber)
            if (sectionId > 0) putExtra(ReadingActivity.EXTRA_VERSE_NUMBER, sectionId)
        }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                navigateLauncher.launch(Intent(this, SearchActivity::class.java))
                true
            }
            R.id.action_bookmarks -> {
                navigateLauncher.launch(Intent(this, BookmarksActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
