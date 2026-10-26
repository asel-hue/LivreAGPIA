package com.agpia.livre.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.agpia.livre.R
import com.agpia.livre.ReadingActivity
import com.agpia.livre.data.BookRepository
import com.agpia.livre.databinding.ActivityBookmarksBinding
import com.agpia.livre.utils.PreferencesManager
import com.agpia.livre.utils.applySystemBarInsets
import com.google.android.material.snackbar.Snackbar

data class BookmarkItem(
    val chapterNumber: Int,
    val chapterTitle: String,
    val sectionId: Int,
    val sectionTitle: String,
    val preview: String
)

class BookmarksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookmarksBinding
    private lateinit var bookmarksAdapter: BookmarksAdapter
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerViewBookmarks.applySystemBarInsets(bottom = true)

        prefsManager = PreferencesManager(this)

        setupToolbar()
        setupRecyclerView()
        loadBookmarks()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.bookmarks)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        bookmarksAdapter = BookmarksAdapter(
            onItemClick = { bookmark ->
                val intent = Intent().apply {
                    putExtra(ReadingActivity.EXTRA_CHAPTER_NUMBER, bookmark.chapterNumber)
                    putExtra(ReadingActivity.EXTRA_VERSE_NUMBER, bookmark.sectionId)
                }
                setResult(RESULT_OK, intent)
                finish()
            },
            onDeleteClick = { bookmark ->
                prefsManager.removeBookmark(bookmark.chapterNumber, bookmark.sectionId)
                loadBookmarks()
                Snackbar.make(binding.root, R.string.bookmark_removed, Snackbar.LENGTH_SHORT).show()
            }
        )
        binding.recyclerViewBookmarks.apply {
            layoutManager = LinearLayoutManager(this@BookmarksActivity)
            adapter = bookmarksAdapter
        }
    }

    private fun loadBookmarks() {
        val bookmarks = prefsManager.getBookmarks().mapNotNull { key ->
            val parts = key.split(":")
            val chapterNumber = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val sectionId = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            val chapter = BookRepository.getChapter(chapterNumber) ?: return@mapNotNull null
            val section = BookRepository.getSection(chapterNumber, sectionId) ?: return@mapNotNull null
            BookmarkItem(
                chapterNumber = chapterNumber,
                chapterTitle = chapter.title,
                sectionId = sectionId,
                sectionTitle = section.title,
                preview = section.content.trim().replace('\n', ' ')
            )
        }.sortedWith(compareBy({ it.chapterNumber }, { it.sectionId }))

        binding.textViewNoBookmarks.isVisible = bookmarks.isEmpty()
        binding.recyclerViewBookmarks.isVisible = bookmarks.isNotEmpty()
        bookmarksAdapter.submitList(bookmarks)
    }
}
