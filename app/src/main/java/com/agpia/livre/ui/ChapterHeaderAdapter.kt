package com.agpia.livre.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.agpia.livre.data.BILINGUAL_SEPARATOR
import com.agpia.livre.data.Chapter
import com.agpia.livre.databinding.ItemChapterHeaderBinding
import com.agpia.livre.utils.hourImageRes

/**
 * En-tête unique en haut de l'écran de lecture (via ConcatAdapter) : image de l'heure
 * (`hour_N` dans drawable-nodpi, masquée si absente) et titre complet.
 */
class ChapterHeaderAdapter : RecyclerView.Adapter<ChapterHeaderAdapter.HeaderViewHolder>() {

    private var chapter: Chapter? = null

    fun setChapter(chapter: Chapter?) {
        this.chapter = chapter
        notifyItemChanged(0)
    }

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val binding = ItemChapterHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(chapter)
    }

    class HeaderViewHolder(
        private val binding: ItemChapterHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chapter: Chapter?) {
            if (chapter == null) {
                binding.root.isVisible = false
                return
            }
            binding.root.isVisible = true
            binding.headerRoot.layoutDirection =
                if (chapter.isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            binding.textViewChapterTitle.textDirection =
                if (chapter.isRtl) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
            // Titre bilingue : l'arabe sur sa propre ligne plutôt qu'un séparateur en bout de ligne.
            binding.textViewChapterTitle.text = chapter.title.replace(BILINGUAL_SEPARATOR, "\n")

            val imageRes = binding.root.context.hourImageRes(chapter.number)
            binding.cardHourImage.isVisible = imageRes != 0
            if (imageRes != 0) binding.imageHour.setImageResource(imageRes)
        }
    }
}
