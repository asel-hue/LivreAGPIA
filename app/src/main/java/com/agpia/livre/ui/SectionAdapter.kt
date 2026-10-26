package com.agpia.livre.ui

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agpia.livre.R
import com.agpia.livre.data.SectionGroups
import com.agpia.livre.data.SectionPair
import com.agpia.livre.databinding.ItemSectionBinding
import com.agpia.livre.utils.CopticText
import com.agpia.livre.utils.PreferencesManager
import com.google.android.material.color.MaterialColors

/**
 * Affiche les parties d'une heure. Toutes les parties se replient/déplient d'un tap sur leur
 * ligne de titre (chevron animé) ; les psaumes sont repliés par défaut, comme dans l'application
 * d'origine. « Tout agrandir / Tout réduire » agit sur l'ensemble des parties de l'heure.
 * En mode bilingue, chaque partie affiche le français et l'arabe sur deux colonnes.
 *
 * Niveau de groupe : une partie d'en-tête de service (voir [SectionGroups]) est affichée en bandeau ;
 * un tap replie/déplie l'ensemble des parties du service, qui sortent alors de la liste affichée
 * (leur état individuel est conservé) et sont légèrement indentées quand elles sont visibles.
 */
class SectionAdapter(
    var chapterNumber: Int,
    private val prefsManager: PreferencesManager,
    private val onShare: (SectionPair) -> Unit,
    private val onBookmarkChanged: (added: Boolean) -> Unit,
    private val onExpandedStateChanged: () -> Unit
) : ListAdapter<SectionPair, SectionAdapter.SectionViewHolder>(SectionDiffCallback()) {

    private companion object {
        /** Taille des titres de parties par rapport au corps de texte (sp), identique pour tous. */
        const val TITLE_EXTRA_SP = 2f

        /** Retrait de début des parties d'un service (dp), en plus du retrait normal. */
        const val GROUP_INDENT_DP = 12
        const val ROOT_PADDING_START_DP = 16
    }

    /** Toutes les parties de l'heure (la liste affichée en exclut celles des services repliés). */
    private var allSections: List<SectionPair> = emptyList()
    private var grouping: SectionGroups.Grouping = SectionGroups.Grouping.EMPTY

    /** Identifiants des parties actuellement dépliées (état d'affichage, pas de donnée). */
    private val expandedIds = mutableSetOf<Int>()

    /** Identifiants des en-têtes de service dont le groupe est replié. */
    private val collapsedGroupIds = mutableSetOf<Int>()

    /** Sens de lecture de la langue principale (pilote la mise en page), fixé par l'heure chargée. */
    var isRtl: Boolean = false
        private set

    /** Deux colonnes : principale puis secondaire. */
    var isBilingual: Boolean = false
        private set

    /** Sens de lecture de la langue secondaire (seconde colonne). */
    private var secondaryRtl: Boolean = false

    /** Codes des langues affichées (règles de taille propres à certaines écritures). */
    private var primaryLanguage: String = PreferencesManager.LANG_FR
    private var secondaryLanguage: String? = null

    /** Fixe la présentation de l'heure à afficher (à appeler avant [setSections]). */
    fun setLayout(primaryLanguage: String, rtl: Boolean, secondaryLanguage: String?, secondaryRtl: Boolean) {
        val bilingual = secondaryLanguage != null
        if (isRtl == rtl && isBilingual == bilingual && this.secondaryRtl == secondaryRtl &&
            this.primaryLanguage == primaryLanguage && this.secondaryLanguage == secondaryLanguage
        ) return
        isRtl = rtl
        isBilingual = bilingual
        this.secondaryRtl = secondaryRtl
        this.primaryLanguage = primaryLanguage
        this.secondaryLanguage = secondaryLanguage
        notifyDataSetChanged()
    }

    var fontSizeSp: Float = prefsManager.getFontSize()
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    /**
     * Titres de parties centrés (réglage « Titres : Centrés »). Le centrage est absolu dans la ligne :
     * un espace symétrique du chevron + ⋮ est inséré au début, le titre se centre entre les deux.
     */
    var titleCentered: Boolean = prefsManager.isSectionTitleCentered()
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    // ---- Liste affichée ----

    /** Remplace les parties de l'heure ; [commit] est appelé une fois la liste affichée. */
    fun setSections(sections: List<SectionPair>, commit: () -> Unit) {
        allSections = sections
        grouping = SectionGroups.compute(sections)
        collapsedGroupIds.retainAll(grouping.headerIds)
        submitList(visibleSections(), commit)
    }

    /** Parties à afficher : toutes sauf celles d'un service replié (l'en-tête reste visible). */
    private fun visibleSections(): List<SectionPair> =
        allSections.filter { grouping.parentOf(it.id)?.let { parent -> parent !in collapsedGroupIds } ?: true }

    /** Position dans la liste affichée, ou -1 si la partie est masquée ou absente. */
    fun positionOf(sectionId: Int): Int = currentList.indexOfFirst { it.id == sectionId }

    // ---- État déplié / replié ----

    /** Parties repliables individuellement : celles qui ont un corps, hors en-têtes de service. */
    private val collapsible: List<SectionPair>
        get() = allSections.filter { it.hasBody && !grouping.isHeader(it.id) }

    fun collapseAll() {
        expandedIds.clear()
        collapsedGroupIds.addAll(grouping.headerIds)
        refreshAll()
    }

    fun expandAll() {
        collapsible.forEach { expandedIds.add(it.id) }
        collapsedGroupIds.clear()
        refreshAll()
    }

    private fun refreshAll() {
        submitList(visibleSections()) {
            notifyDataSetChanged()
            onExpandedStateChanged()
        }
    }

    /** Vrai si l'heure a des parties ou groupes repliables et qu'ils sont tous dépliés. */
    fun areAllExpanded(): Boolean =
        hasCollapsible() && collapsible.all { it.id in expandedIds } && collapsedGroupIds.isEmpty()

    fun hasCollapsible(): Boolean = collapsible.isNotEmpty() || !grouping.isEmpty()

    /**
     * Rend une partie visible et dépliée (ouverture d'un marque-page ou d'un résultat de recherche) :
     * déplie d'abord son service s'il est replié, puis fournit sa position dans la liste affichée.
     */
    fun reveal(sectionId: Int, onReady: (position: Int) -> Unit) {
        val section = allSections.find { it.id == sectionId }
        if (section == null) {
            onReady(-1)
            return
        }
        if (grouping.isHeader(sectionId)) {
            collapsedGroupIds.remove(sectionId)
        } else if (section.hasBody) {
            expandedIds.add(sectionId)
        }
        val parent = grouping.parentOf(sectionId)
        val groupOpened = parent != null && collapsedGroupIds.remove(parent)
        if (groupOpened || grouping.isHeader(sectionId)) {
            submitList(visibleSections()) {
                notifyDataSetChanged()
                onExpandedStateChanged()
                onReady(positionOf(sectionId))
            }
        } else {
            refreshSection(sectionId)
            onExpandedStateChanged()
            onReady(positionOf(sectionId))
        }
    }

    fun expandedIds(): IntArray = expandedIds.toIntArray()
    fun collapsedGroupIds(): IntArray = collapsedGroupIds.toIntArray()

    /** Restaure l'état sauvegardé (avant [setSections]). */
    fun restoreState(expanded: IntArray, collapsedGroups: IntArray?) {
        expandedIds.clear()
        expandedIds.addAll(expanded.toList())
        collapsedGroupIds.clear()
        collapsedGroups?.let { collapsedGroupIds.addAll(it.toList()) }
    }

    /**
     * Remplace l'état déplié sans notifier (à appeler avant [setSections]) :
     * tout déplié si [expandAll], sinon psaumes repliés et autres parties dépliées ;
     * les services sont toujours dépliés à l'ouverture d'une heure.
     */
    fun resetExpanded(expandAll: Boolean, sections: List<SectionPair>) {
        expandedIds.clear()
        collapsedGroupIds.clear()
        sections.filter { it.hasBody && (expandAll || !it.isPsalm) }.forEach { expandedIds.add(it.id) }
    }

    fun refreshSection(sectionId: Int) {
        val position = positionOf(sectionId)
        if (position != -1) notifyItemChanged(position)
    }

    private fun isExpanded(section: SectionPair): Boolean =
        if (grouping.isHeader(section.id)) section.id !in collapsedGroupIds
        else section.hasBody && section.id in expandedIds

    // ---- Tailles de texte ----

    /** Corps de base : en bilingue portrait, un peu plus petit pour tenir sur deux colonnes. */
    private fun baseSizeSp(portrait: Boolean): Float =
        if (isBilingual && portrait) fontSizeSp - 1f else fontSizeSp

    /** Taille du corps pour une langue : l'arabe (naskh) gagne à être un peu plus grand. */
    private fun sizeSpFor(language: String?, portrait: Boolean): Float =
        baseSizeSp(portrait) + if (language == PreferencesManager.LANG_AR) 2f else 0f

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SectionViewHolder(
        private val binding: ItemSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /** Couleur de titre du style, pour la rétablir après un en-tête de service (vue recyclée). */
        private val defaultTitleColor: ColorStateList = binding.textViewSectionTitle.textColors
        private val groupBackground = MaterialColors.getColor(
            binding.root, com.google.android.material.R.attr.colorSurfaceContainer
        )
        private val groupTitleColor = MaterialColors.getColor(
            binding.root, androidx.appcompat.R.attr.colorPrimary
        )
        private val density = binding.root.resources.displayMetrics.density

        init {
            binding.headerRow.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                val section = getItem(position)
                if (grouping.isHeader(section.id)) {
                    toggleGroup(section)
                    return@setOnClickListener
                }
                if (!section.hasBody) return@setOnClickListener
                val expanded = if (expandedIds.remove(section.id)) false else {
                    expandedIds.add(section.id); true
                }
                // Mise à jour directe du support (pas de rebind) pour animer le chevron.
                applyExpanded(section, expanded, animate = true)
                onExpandedStateChanged()
            }
            binding.buttonMore.setOnClickListener { showActions(it) }
        }

        /** Replie/déplie tout un service : ses parties quittent ou rejoignent la liste affichée. */
        private fun toggleGroup(header: SectionPair) {
            val expanded = if (collapsedGroupIds.remove(header.id)) true else {
                collapsedGroupIds.add(header.id); false
            }
            applyExpanded(header, expanded, animate = true)
            submitList(visibleSections()) { onExpandedStateChanged() }
        }

        fun bind(section: SectionPair) {
            val context = binding.root.context
            val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            val primarySize = sizeSpFor(primaryLanguage, portrait)
            val secondarySize = sizeSpFor(secondaryLanguage, portrait)
            val isHeader = grouping.isHeader(section.id)
            val inGroup = grouping.parentOf(section.id) != null

            // Sens de lecture selon la langue réelle du texte (et non celle de l'interface),
            // pour que le repli français reste lisible même sous une interface arabe.
            binding.sectionRoot.layoutDirection =
                if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            val textDirection = if (isRtl) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
            binding.textViewSectionTitle.textDirection = textDirection
            binding.textViewSectionContent.textDirection = textDirection
            // Seconde colonne : sens propre à la langue secondaire.
            binding.textViewSectionContentSecondary.textDirection =
                if (secondaryRtl) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR

            // Hiérarchie : bandeau pour l'en-tête de service, retrait pour les parties du service.
            binding.sectionRoot.setBackgroundColor(if (isHeader) groupBackground else Color.TRANSPARENT)
            binding.sectionRoot.updatePaddingRelative(
                start = ((ROOT_PADDING_START_DP + if (inGroup) GROUP_INDENT_DP else 0) * density).toInt()
            )
            binding.textViewSectionTitle.setTextColor(if (isHeader) ColorStateList.valueOf(groupTitleColor) else defaultTitleColor)

            // Même taille et même style pour tous les titres de parties (psaume ou non, replié ou non).
            binding.textViewSectionTitle.text = CopticText.style(context, section.title)
            binding.textViewSectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, primarySize + TITLE_EXTRA_SP)
            // Alignement : au début (= à droite en arabe seul, via layoutDirection) ou centré.
            binding.textViewSectionTitle.textAlignment =
                if (titleCentered) View.TEXT_ALIGNMENT_CENTER else View.TEXT_ALIGNMENT_VIEW_START
            binding.spaceTitleStart.isVisible = titleCentered

            binding.textViewSectionContent.text = CopticText.style(context, section.primary.content.trim())
            binding.textViewSectionContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, primarySize)
            if (isBilingual) {
                // Section absente dans la langue secondaire : colonne vide.
                binding.textViewSectionContentSecondary.text =
                    CopticText.style(context, section.secondary?.content?.trim().orEmpty())
                binding.textViewSectionContentSecondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, secondarySize)
            }
            binding.textViewSectionContentSecondary.isVisible = isBilingual
            binding.columnDivider.isVisible = isBilingual

            applyExpanded(section, isExpanded(section), animate = false)
            binding.imageBookmarked.isVisible = prefsManager.isBookmarked(chapterNumber, section.id)
        }

        /** Corps visible ou non, chevron orienté, état annoncé à l'accessibilité. */
        private fun applyExpanded(section: SectionPair, expanded: Boolean, animate: Boolean) {
            val context = binding.root.context
            binding.headerRow.contentDescription = section.title
            val isHeader = grouping.isHeader(section.id)

            if (!isHeader && !section.hasBody) {
                // Titre seul (ex. « Kyrie Eleison (41 fois) ») : ni corps, ni chevron, ni repli.
                binding.bodyRow.isVisible = false
                binding.imageChevron.animate().cancel()
                binding.imageChevron.visibility = View.INVISIBLE
                binding.headerRow.isClickable = false
                ViewCompat.setStateDescription(binding.headerRow, null)
                ViewCompat.replaceAccessibilityAction(
                    binding.headerRow,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK, null, null
                )
                return
            }

            binding.imageChevron.visibility = View.VISIBLE
            binding.headerRow.isClickable = true
            // L'intro d'un service (son propre texte) suit l'état du groupe.
            binding.bodyRow.isVisible = expanded && section.hasBody

            if (isHeader) {
                // Chevron de groupe : icône « déplier / replier tout » plutôt qu'une flèche tournante.
                binding.imageChevron.animate().cancel()
                binding.imageChevron.rotation = 0f
                binding.imageChevron.setImageResource(
                    if (expanded) R.drawable.ic_unfold_less else R.drawable.ic_unfold_more
                )
            } else {
                binding.imageChevron.setImageResource(R.drawable.ic_expand_more)
                val rotation = if (expanded) 180f else 0f
                if (animate) {
                    binding.imageChevron.animate().rotation(rotation).setDuration(200).start()
                } else {
                    binding.imageChevron.animate().cancel()
                    binding.imageChevron.rotation = rotation
                }
            }

            ViewCompat.setStateDescription(
                binding.headerRow,
                context.getString(if (expanded) R.string.state_expanded else R.string.state_collapsed)
            )
            val actionLabel = when {
                isHeader && expanded -> R.string.collapse_group
                isHeader -> R.string.expand_group
                expanded -> R.string.collapse_section
                else -> R.string.expand_section
            }
            ViewCompat.replaceAccessibilityAction(
                binding.headerRow,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                context.getString(actionLabel),
                null
            )
        }

        private fun showActions(anchor: View) {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return
            val section = getItem(position)
            val context = anchor.context
            val bookmarked = prefsManager.isBookmarked(chapterNumber, section.id)

            PopupMenu(context, anchor).apply {
                inflate(R.menu.section_actions)
                menu.findItem(R.id.action_bookmark).setTitle(
                    if (bookmarked) R.string.remove_bookmark else R.string.add_bookmark
                )
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_bookmark -> {
                            if (bookmarked) prefsManager.removeBookmark(chapterNumber, section.id)
                            else prefsManager.addBookmark(chapterNumber, section.id)
                            notifyItemChanged(position)
                            onBookmarkChanged(!bookmarked)
                            true
                        }
                        R.id.action_share -> {
                            onShare(section)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }
    }

    class SectionDiffCallback : DiffUtil.ItemCallback<SectionPair>() {
        override fun areItemsTheSame(oldItem: SectionPair, newItem: SectionPair): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SectionPair, newItem: SectionPair): Boolean =
            oldItem == newItem
    }
}
