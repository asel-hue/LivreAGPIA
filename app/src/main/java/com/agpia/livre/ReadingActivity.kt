package com.agpia.livre

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.agpia.livre.data.BILINGUAL_SEPARATOR
import com.agpia.livre.data.BookRepository
import com.agpia.livre.data.Chapter
import com.agpia.livre.data.ChapterView
import com.agpia.livre.data.SectionPair
import com.agpia.livre.databinding.ActivityReadingBinding
import com.agpia.livre.ui.BookmarksActivity
import com.agpia.livre.ui.ChapterHeaderAdapter
import com.agpia.livre.ui.SearchActivity
import com.agpia.livre.ui.SectionAdapter
import com.agpia.livre.ui.SettingsActivity
import com.agpia.livre.utils.PreferencesManager
import com.agpia.livre.utils.applySystemBarInsets
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

/**
 * Écran de lecture d'une heure : en-tête illustré, parties repliables (français, arabe ou bilingue),
 * tiroir latéral listant les heures (comme l'application Agpia d'origine et Coptic Reader).
 */
class ReadingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReadingBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var sectionAdapter: SectionAdapter
    private lateinit var headerAdapter: ChapterHeaderAdapter
    private var currentChapterNumber: Int = 1
    private var totalChapters: Int = 0
    private var expandAllButton: MaterialButton? = null

    companion object {
        const val EXTRA_CHAPTER_NUMBER = "chapter_number"
        const val EXTRA_VERSE_NUMBER = "verse_number"
        private const val STATE_CHAPTER = "state_chapter"
        private const val STATE_EXPANDED = "state_expanded"
        private const val STATE_COLLAPSED_GROUPS = "state_collapsed_groups"

        /** Nombre d'éléments précédant les parties dans le RecyclerView (en-tête d'heure). */
        private const val HEADER_COUNT = 1

        // Menu rapide des langues
        private const val GROUP_PRIMARY = 1
        private const val GROUP_SECONDARY = 2
        private const val ID_SECONDARY_SUBMENU = 1000
        private const val ID_SECONDARY_NONE = 1001
    }

    /** Génération du contenu affiché : si le dépôt a été rechargé (mise à jour), l'heure est relue. */
    private var loadedGeneration = -1

    override fun onResume() {
        super.onResume()
        if (loadedGeneration != -1 && loadedGeneration != BookRepository.generation) {
            populateDrawerMenu()
            loadChapter(currentChapterNumber, resetExpanded = false)
        }
    }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // La taille de police ou la langue a pu changer : on rafraîchit sans recréer l'écran
                // (un changement de langue recrée de toute façon l'activité via la locale).
                sectionAdapter.fontSizeSp = prefsManager.getFontSize()
                sectionAdapter.titleCentered = prefsManager.isSectionTitleCentered()
                headerAdapter.titleCentered = prefsManager.isSectionTitleCentered()
                populateDrawerMenu()
                loadChapter(currentChapterNumber, resetExpanded = false)
            }
        }

    private val navigateLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let {
                    val chapterNum = it.getIntExtra(EXTRA_CHAPTER_NUMBER, -1)
                    val verseNum = it.getIntExtra(EXTRA_VERSE_NUMBER, -1)
                    if (chapterNum != -1) loadChapter(chapterNum, verseNum)
                }
            }
            // Les marque-pages ont pu changer pendant l'écran enfant.
            sectionAdapter.notifyDataSetChanged()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefsManager = PreferencesManager(this)

        binding = ActivityReadingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonNavigation.applySystemBarInsets(bottom = true)

        totalChapters = BookRepository.getTotalChapters()

        setupToolbar()
        setupDrawer()
        setupRecyclerView()
        setupNavigationButtons()
        setupBackHandling()

        val startingChapter = savedInstanceState?.getInt(STATE_CHAPTER)
            ?: intent.getIntExtra(EXTRA_CHAPTER_NUMBER, prefsManager.getLastChapter())
        val startingVerse = if (savedInstanceState == null) intent.getIntExtra(EXTRA_VERSE_NUMBER, -1) else -1
        savedInstanceState?.getIntArray(STATE_EXPANDED)?.let {
            sectionAdapter.restoreState(it, savedInstanceState.getIntArray(STATE_COLLAPSED_GROUPS))
        }
        loadChapter(startingChapter, startingVerse, resetExpanded = savedInstanceState == null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_CHAPTER, currentChapterNumber)
        outState.putIntArray(STATE_EXPANDED, sectionAdapter.expandedIds())
        outState.putIntArray(STATE_COLLAPSED_GROUPS, sectionAdapter.collapsedGroupIds())
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // L'en-tête du tiroir passe sous la barre d'état transparente : on lui ajoute l'inset haut.
        binding.navigationView.getHeaderView(0)?.applySystemBarInsets(top = true, right = false)

        populateDrawerMenu()

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            loadChapter(menuItem.itemId)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    /** Le menu du tiroir est construit à partir du contenu (dans la langue choisie) : une entrée par heure. */
    private fun populateDrawerMenu() {
        val menu = binding.navigationView.menu
        menu.clear()
        BookRepository.getChapters().forEach { chapter ->
            menu.add(Menu.NONE, chapter.number, chapter.number, chapter.shortTitle).isCheckable = true
        }
        menu.setGroupCheckable(Menu.NONE, true, true)
        binding.navigationView.setCheckedItem(currentChapterNumber)
    }

    private fun setupBackHandling() {
        // Retour : ferme d'abord le tiroir s'il est ouvert.
        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupNavigationButtons() {
        binding.buttonPrevious.setOnClickListener {
            if (currentChapterNumber > 1) loadChapter(currentChapterNumber - 1)
        }
        binding.buttonNext.setOnClickListener {
            if (currentChapterNumber < totalChapters) loadChapter(currentChapterNumber + 1)
        }
        binding.buttonHome.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        headerAdapter = ChapterHeaderAdapter().apply {
            titleCentered = prefsManager.isSectionTitleCentered()
        }
        sectionAdapter = SectionAdapter(
            chapterNumber = currentChapterNumber,
            prefsManager = prefsManager,
            onShare = { section -> shareSection(section) },
            onBookmarkChanged = { added ->
                Snackbar.make(
                    binding.recyclerViewSections,
                    if (added) R.string.bookmark_added else R.string.bookmark_removed,
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(binding.buttonNavigation).show()
            },
            onExpandedStateChanged = { updateExpandAllAction() }
        )
        binding.recyclerViewSections.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSections.adapter = ConcatAdapter(headerAdapter, sectionAdapter)
        // Pas de fondu enchaîné à la mise à jour d'une partie (marque-page, repli) : le chevron
        // est animé directement dans le support de vue.
        (binding.recyclerViewSections.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    private fun loadChapter(chapterNumber: Int, sectionToScroll: Int = -1, resetExpanded: Boolean = true) {
        val view: ChapterView = BookRepository.getChapterView(chapterNumber) ?: return
        val chapter: Chapter = view.chapter
        currentChapterNumber = chapter.number
        prefsManager.setLastChapter(chapter.number)
        // En bilingue, le titre court ne tient pas sur une ligne : l'arabe passe en sous-titre.
        val titleParts = chapter.shortTitle.split(BILINGUAL_SEPARATOR)
        supportActionBar?.title = titleParts[0]
        supportActionBar?.subtitle = titleParts.getOrNull(1)
        binding.navigationView.setCheckedItem(chapter.number)
        headerAdapter.setChapter(chapter)

        sectionAdapter.chapterNumber = chapter.number
        sectionAdapter.setLayout(
            primaryLanguage = chapter.language,
            rtl = view.isRtl,
            secondaryLanguage = if (view.bilingual) BookRepository.secondaryLanguage else null,
            secondaryRtl = view.secondaryRtl
        )
        loadedGeneration = BookRepository.generation
        // À l'ouverture d'une heure, les parties suivent le réglage « Tout agrandir » mémorisé
        // (sinon : psaumes repliés, reste déplié).
        if (resetExpanded) sectionAdapter.resetExpanded(prefsManager.isExpandAll(), view.sections)
        sectionAdapter.setSections(view.sections) {
            updateExpandAllAction()
            if (sectionToScroll > 0) {
                // Déplie la partie (et son service s'il est replié), puis défile vers sa position
                // dans la liste affichée. Après un changement d'heure, on laisse la liste se mettre
                // en page (mises à jour du diff) avant de positionner, sinon le défilement est décalé.
                sectionAdapter.reveal(sectionToScroll) { position ->
                    if (position != -1) {
                        binding.recyclerViewSections.post {
                            (binding.recyclerViewSections.layoutManager as? LinearLayoutManager)
                                ?.scrollToPositionWithOffset(position + HEADER_COUNT, 0)
                        }
                    }
                }
            } else if (resetExpanded) {
                binding.recyclerViewSections.scrollToPosition(0)
            }
        }

        binding.buttonPrevious.isEnabled = chapter.number > 1
        binding.buttonNext.isEnabled = chapter.number < totalChapters
    }

    /** Partage le texte affiché : une langue, ou les deux textes l'un sous l'autre en mode bilingue. */
    private fun shareSection(section: SectionPair) {
        val chapterTitle = BookRepository.getChapter(currentChapterNumber)?.title ?: ""
        val text = buildString {
            append(chapterTitle).append(" — ").append(section.title).append("\n\n")
            // Une partie sans corps (titre seul) se partage avec son titre uniquement.
            listOfNotNull(section.primary, section.secondary)
                .filter { it.content.isNotBlank() }
                .forEach { part -> append(part.content.trim()).append("\n\n") }
            append(getString(R.string.app_name))
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "$chapterTitle — ${section.title}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_via)))
    }

    private fun changeFontSize(delta: Float) {
        val newSize = (prefsManager.getFontSize() + delta)
            .coerceIn(PreferencesManager.MIN_FONT_SIZE, PreferencesManager.MAX_FONT_SIZE)
        prefsManager.setFontSize(newSize)
        sectionAdapter.fontSizeSp = newSize
    }

    // ---- Tout agrandir / tout réduire ----

    private fun toggleExpandAll() {
        val expandAll = !sectionAdapter.areAllExpanded()
        prefsManager.setExpandAll(expandAll)
        if (expandAll) sectionAdapter.expandAll() else sectionAdapter.collapseAll()
    }

    /** Met le bouton de la barre en phase avec l'état réel des parties de l'heure. */
    private fun updateExpandAllAction() {
        val button = expandAllButton ?: return
        val allExpanded = sectionAdapter.areAllExpanded()
        button.isEnabled = sectionAdapter.hasCollapsible()
        button.setText(if (allExpanded) R.string.collapse_all else R.string.expand_all)
        button.setIconResource(if (allExpanded) R.drawable.ic_unfold_less else R.drawable.ic_unfold_more)
    }

    // ---- Langues du contenu ----

    /**
     * Menu rapide des langues : la langue principale (une par entrée, construite depuis le manifeste)
     * et un sous-menu « Seconde langue » (Aucune / autres langues).
     */
    private fun showLanguageMenu() {
        val anchor = binding.toolbar.findViewById<View>(R.id.action_language) ?: binding.toolbar
        val languages = BookRepository.languages
        val primary = BookRepository.primaryLanguage
        val secondary = BookRepository.secondaryLanguage
        PopupMenu(this, anchor).apply {
            languages.forEachIndexed { index, lang ->
                menu.add(GROUP_PRIMARY, index, index, lang.nativeName).apply {
                    isCheckable = true
                    isChecked = lang.code == primary
                }
            }
            menu.setGroupCheckable(GROUP_PRIMARY, true, true)
            val sub = menu.addSubMenu(Menu.NONE, ID_SECONDARY_SUBMENU, languages.size, R.string.secondary_language)
            sub.add(GROUP_SECONDARY, ID_SECONDARY_NONE, 0, R.string.secondary_none).apply {
                isCheckable = true
                isChecked = secondary == null
            }
            languages.forEachIndexed { index, lang ->
                sub.add(GROUP_SECONDARY, index, index + 1, lang.nativeName).apply {
                    isCheckable = true
                    isChecked = lang.code == secondary
                    isEnabled = lang.code != primary
                }
            }
            sub.setGroupCheckable(GROUP_SECONDARY, true, true)
            setOnMenuItemClickListener { item ->
                when (item.groupId) {
                    GROUP_PRIMARY -> {
                        selectPrimaryLanguage(languages[item.itemId].code); true
                    }
                    GROUP_SECONDARY -> {
                        selectSecondaryLanguage(if (item.itemId == ID_SECONDARY_NONE) null else languages[item.itemId].code)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun selectPrimaryLanguage(code: String) {
        if (code == BookRepository.primaryLanguage) return
        prefsManager.setPrimaryLanguage(code)
        // Si la locale de l'interface change, AppCompat recrée l'écran (l'heure courante est restaurée
        // via l'état sauvegardé) ; sinon on recharge simplement le contenu dans le nouveau mode.
        if (!prefsManager.applyAppLocale(BookRepository.uiLocaleFor(code))) refreshAfterLanguageChange()
    }

    private fun selectSecondaryLanguage(code: String?) {
        if (code == BookRepository.secondaryLanguage) return
        prefsManager.setSecondaryLanguage(code)
        refreshAfterLanguageChange()
    }

    private fun refreshAfterLanguageChange() {
        populateDrawerMenu()
        loadChapter(currentChapterNumber, resetExpanded = false)
        val label = listOfNotNull(BookRepository.primaryLanguage, BookRepository.secondaryLanguage)
            .joinToString(" + ") { BookRepository.languageInfo(it)?.nativeName ?: it }
        Snackbar.make(
            binding.recyclerViewSections,
            getString(R.string.content_language_set, label),
            Snackbar.LENGTH_SHORT
        ).setAnchorView(binding.buttonNavigation).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.reading_menu, menu)
        val item = menu.findItem(R.id.action_expand_all)
        expandAllButton = (item.actionView as? MaterialButton)?.also { button ->
            button.setOnClickListener { toggleExpandAll() }
        }
        if (::sectionAdapter.isInitialized) updateExpandAllAction()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_expand_all -> {
                toggleExpandAll()
                true
            }
            R.id.action_language -> {
                showLanguageMenu()
                true
            }
            R.id.action_search -> {
                navigateLauncher.launch(Intent(this, SearchActivity::class.java))
                true
            }
            R.id.action_bookmarks -> {
                navigateLauncher.launch(Intent(this, BookmarksActivity::class.java))
                true
            }
            R.id.action_text_larger -> {
                changeFontSize(2 * PreferencesManager.FONT_SIZE_STEP)
                true
            }
            R.id.action_text_smaller -> {
                changeFontSize(-2 * PreferencesManager.FONT_SIZE_STEP)
                true
            }
            R.id.action_settings -> {
                settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
