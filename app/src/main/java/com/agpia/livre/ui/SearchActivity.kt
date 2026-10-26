package com.agpia.livre.ui

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.agpia.livre.R
import com.agpia.livre.ReadingActivity
import com.agpia.livre.data.BookRepository
import com.agpia.livre.databinding.ActivitySearchBinding
import com.agpia.livre.utils.applySystemBarInsets
import java.text.Normalizer

data class SearchResult(
    val chapterNumber: Int,
    val chapterTitle: String,
    val sectionId: Int,
    val sectionTitle: String,
    val matchedText: String
)

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var searchAdapter: SearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Le clavier et la barre de navigation ne doivent pas masquer les résultats.
        binding.recyclerViewResults.applySystemBarInsets(bottom = true, includeIme = true)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.search)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter { result ->
            val intent = Intent().apply {
                putExtra(ReadingActivity.EXTRA_CHAPTER_NUMBER, result.chapterNumber)
                putExtra(ReadingActivity.EXTRA_VERSE_NUMBER, result.sectionId)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
        binding.recyclerViewResults.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = searchAdapter
        }
    }

    private fun setupSearch() {
        binding.editTextSearch.doAfterTextChanged { performSearch(it?.toString().orEmpty()) }
        binding.editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            actionId == EditorInfo.IME_ACTION_SEARCH
        }
        binding.editTextSearch.requestFocus()
    }

    private fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            searchAdapter.submitList(emptyList())
            binding.textViewNoResults.isVisible = false
            return
        }

        val normalizedQuery = normalize(trimmed)
        val results = mutableListOf<SearchResult>()

        BookRepository.getChapters().forEach { chapter ->
            chapter.sections.forEach { section ->
                val content = section.content
                val titleMatch = normalize(section.title).contains(normalizedQuery)
                val matchIndex = normalize(content).indexOf(normalizedQuery)
                if (titleMatch || matchIndex != -1) {
                    val snippet = if (matchIndex != -1) {
                        snippetAround(content, matchIndex, normalizedQuery.length)
                    } else {
                        snippetAround(content, 0, 0)
                    }
                    results.add(
                        SearchResult(
                            chapterNumber = chapter.number,
                            chapterTitle = chapter.title,
                            sectionId = section.id,
                            sectionTitle = section.title,
                            matchedText = snippet
                        )
                    )
                }
            }
        }

        binding.textViewNoResults.isVisible = results.isEmpty()
        if (results.isEmpty()) {
            binding.textViewNoResults.text = getString(R.string.no_results_found, trimmed)
        }
        searchAdapter.submitList(results)
    }

    /** Extrait centré sur la correspondance, coupé sur des limites de mots. */
    private fun snippetAround(text: String, matchIndex: Int, matchLength: Int): String {
        var start = (matchIndex - 60).coerceAtLeast(0)
        var end = (matchIndex + matchLength + 100).coerceAtMost(text.length)
        if (start > 0) {
            val space = text.indexOf(' ', start)
            if (space != -1 && space < matchIndex) start = space + 1
        }
        if (end < text.length) {
            val space = text.lastIndexOf(' ', end)
            if (space > matchIndex + matchLength) end = space
        }
        return buildString {
            if (start > 0) append('…')
            append(text.substring(start, end).replace('\n', ' ').trim())
            if (end < text.length) append('…')
        }
    }

    /**
     * Normalisation pour une recherche insensible à la casse et aux accents.
     * La décomposition NFD suivie de la suppression des diacritiques conserve la longueur
     * des chaînes pour l'alphabet latin, ce qui permet de réutiliser les index sur le texte d'origine.
     */
    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace('’', '\'')
            .lowercase()

    companion object {
        private val DIACRITICS = Regex("\\p{Mn}+")
    }
}
