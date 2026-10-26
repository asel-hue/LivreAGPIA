package com.agpia.livre.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agpia.livre.data.BILINGUAL_SEPARATOR
import com.agpia.livre.data.Chapter
import com.agpia.livre.databinding.ItemChapterBinding
import com.agpia.livre.utils.hourImageRes
import java.util.Locale

class ChapterAdapter(
    private val onChapterClick: (Chapter) -> Unit
) : ListAdapter<Chapter, ChapterAdapter.ChapterViewHolder>(ChapterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val binding = ItemChapterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChapterViewHolder(
        private val binding: ItemChapterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onChapterClick(getItem(position))
            }
        }

        fun bind(chapter: Chapter) {
            // Titre bilingue : l'arabe sur sa propre ligne (évite une coupure au milieu de l'arabe).
            binding.textViewChapterTitle.text = chapter.title.replace(BILINGUAL_SEPARATOR, "\n")

            // Vignette si l'illustration de l'heure existe, sinon numéro dans un rond.
            val imageRes = binding.root.context.hourImageRes(chapter.number)
            binding.cardHourImage.isVisible = imageRes != 0
            binding.textViewChapterNumber.isVisible = imageRes == 0
            if (imageRes != 0) {
                binding.imageHour.setImageResource(imageRes)
            } else {
                binding.textViewChapterNumber.text = String.format(Locale.getDefault(), "%d", chapter.number)
            }
        }
    }

    class ChapterDiffCallback : DiffUtil.ItemCallback<Chapter>() {
        override fun areItemsTheSame(oldItem: Chapter, newItem: Chapter): Boolean =
            oldItem.number == newItem.number

        override fun areContentsTheSame(oldItem: Chapter, newItem: Chapter): Boolean =
            oldItem == newItem
    }
}
