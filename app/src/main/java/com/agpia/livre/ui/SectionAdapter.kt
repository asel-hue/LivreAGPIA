package com.agpia.livre.ui

import android.content.res.Configuration
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agpia.livre.R
import com.agpia.livre.data.SectionPair
import com.agpia.livre.databinding.ItemSectionBinding
import com.agpia.livre.utils.CopticText
import com.agpia.livre.utils.PreferencesManager

/**
 * Affiche les parties d'une heure. Toutes les parties se replient/déplient d'un tap sur leur
 * ligne de titre (chevron animé) ; les psaumes sont repliés par défaut, comme dans l'application
 * d'origine. « Tout agrandir / Tout réduire » agit sur l'ensemble des parties de l'heure.
 * En mode bilingue, chaque partie affiche le français et l'arabe sur deux colonnes.
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
    }

    /** Identifiants des parties actuellement dépliées (état d'affichage, pas de donnée). */
    private val expandedIds = mutableSetOf<Int>()

    /** Sens de lecture du texte affiché (arabe seul = droite à gauche), fixé par l'heure chargée. */
    var isRtl: Boolean = false
        private set

    /** Deux colonnes français / arabe. */
    var isBilingual: Boolean = false
        private set

    /** Fixe la présentation de l'heure à afficher (à appeler avant [submitList]). */
    fun setLayout(rtl: Boolean, bilingual: Boolean) {
        if (isRtl == rtl && isBilingual == bilingual) return
        isRtl = rtl
        isBilingual = bilingual
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

    // ---- État déplié / replié ----

    fun collapseAll() {
        expandedIds.clear()
        notifyDataSetChanged()
        onExpandedStateChanged()
    }

    /** Parties repliables : celles qui ont un corps (les titres seuls sont ignorés). */
    private val collapsible: List<SectionPair>
        get() = currentList.filter { it.hasBody }

    fun expandAll() {
        collapsible.forEach { expandedIds.add(it.id) }
        notifyDataSetChanged()
        onExpandedStateChanged()
    }

    /** Vrai si l'heure a des parties repliables et qu'elles sont toutes dépliées. */
    fun areAllExpanded(): Boolean =
        collapsible.let { it.isNotEmpty() && it.all { s -> s.id in expandedIds } }

    fun hasCollapsible(): Boolean = collapsible.isNotEmpty()

    fun expand(sectionId: Int) {
        if (expandedIds.add(sectionId)) {
            refreshSection(sectionId)
            onExpandedStateChanged()
        }
    }

    fun expandedIds(): IntArray = expandedIds.toIntArray()

    fun restoreExpanded(ids: IntArray) {
        expandedIds.clear()
        expandedIds.addAll(ids.toList())
    }

    /**
     * Remplace l'état déplié sans notifier (à appeler avant [submitList]) :
     * tout déplié si [expandAll], sinon psaumes repliés et autres parties dépliées.
     */
    fun resetExpanded(expandAll: Boolean, sections: List<SectionPair>) {
        expandedIds.clear()
        sections.filter { it.hasBody && (expandAll || !it.isPsalm) }.forEach { expandedIds.add(it.id) }
    }

    fun refreshSection(sectionId: Int) {
        val position = currentList.indexOfFirst { it.id == sectionId }
        if (position != -1) notifyItemChanged(position)
    }

    private fun isExpanded(section: SectionPair): Boolean = section.hasBody && section.id in expandedIds

    // ---- Tailles de texte ----

    /** Corps français : en bilingue portrait, un peu plus petit pour tenir sur deux colonnes. */
    private fun frenchSizeSp(portrait: Boolean): Float =
        if (isBilingual && portrait) fontSizeSp - 1f else fontSizeSp

    /** Corps arabe (naskh) : gagne à être un peu plus grand que le français. */
    private fun arabicSizeSp(portrait: Boolean): Float = frenchSizeSp(portrait) + 2f

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

        init {
            binding.headerRow.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                val section = getItem(position)
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

        fun bind(section: SectionPair) {
            val context = binding.root.context
            val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            val frenchSize = frenchSizeSp(portrait)
            val arabicSize = arabicSizeSp(portrait)

            // Sens de lecture selon la langue réelle du texte (et non celle de l'interface),
            // pour que le repli français reste lisible même sous une interface arabe.
            binding.sectionRoot.layoutDirection =
                if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            val textDirection = if (isRtl) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
            binding.textViewSectionTitle.textDirection = textDirection
            binding.textViewSectionContent.textDirection = textDirection

            // Même taille et même style pour tous les titres de parties (psaume ou non, replié ou non).
            val titleBase = if (isRtl) arabicSize else frenchSize
            binding.textViewSectionTitle.text = CopticText.style(context, section.title)
            binding.textViewSectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleBase + TITLE_EXTRA_SP)
            // Alignement : au début (= à droite en arabe seul, via layoutDirection) ou centré.
            binding.textViewSectionTitle.textAlignment =
                if (titleCentered) View.TEXT_ALIGNMENT_CENTER else View.TEXT_ALIGNMENT_VIEW_START
            binding.spaceTitleStart.isVisible = titleCentered

            if (isBilingual) {
                binding.textViewSectionContent.text =
                    CopticText.style(context, section.french?.content?.trim().orEmpty())
                binding.textViewSectionContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, frenchSize)
                binding.textViewSectionContentArabic.text =
                    CopticText.style(context, section.arabic?.content?.trim().orEmpty())
                binding.textViewSectionContentArabic.setTextSize(TypedValue.COMPLEX_UNIT_SP, arabicSize)
            } else {
                val body = section.primary.content.trim()
                binding.textViewSectionContent.text = CopticText.style(context, body)
                binding.textViewSectionContent.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP, if (isRtl) arabicSize else frenchSize
                )
            }
            binding.textViewSectionContentArabic.isVisible = isBilingual
            binding.columnDivider.isVisible = isBilingual

            applyExpanded(section, isExpanded(section), animate = false)
            binding.imageBookmarked.isVisible = prefsManager.isBookmarked(chapterNumber, section.id)
        }

        /** Corps visible ou non, chevron orienté, état annoncé à l'accessibilité. */
        private fun applyExpanded(section: SectionPair, expanded: Boolean, animate: Boolean) {
            val context = binding.root.context
            binding.headerRow.contentDescription = section.title

            if (!section.hasBody) {
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
            binding.bodyRow.isVisible = expanded
            val rotation = if (expanded) 180f else 0f
            if (animate) {
                binding.imageChevron.animate().rotation(rotation).setDuration(200).start()
            } else {
                binding.imageChevron.animate().cancel()
                binding.imageChevron.rotation = rotation
            }

            ViewCompat.setStateDescription(
                binding.headerRow,
                context.getString(if (expanded) R.string.state_expanded else R.string.state_collapsed)
            )
            ViewCompat.replaceAccessibilityAction(
                binding.headerRow,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                context.getString(if (expanded) R.string.collapse_section else R.string.expand_section),
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
