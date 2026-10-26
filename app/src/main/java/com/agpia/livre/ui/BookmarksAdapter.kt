package com.agpia.livre.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agpia.livre.R
import com.agpia.livre.databinding.ItemBookmarkBinding

class BookmarksAdapter(
    private val onItemClick: (BookmarkItem) -> Unit,
    private val onDeleteClick: (BookmarkItem) -> Unit
) : ListAdapter<BookmarkItem, BookmarksAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemBookmarkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onItemClick(getItem(position))
            }
            binding.buttonDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onDeleteClick(getItem(position))
            }
        }

        fun bind(bookmark: BookmarkItem) {
            binding.textViewChapterTitle.text = binding.root.context.getString(
                R.string.title_with_section, bookmark.chapterTitle, bookmark.sectionTitle
            )
            binding.textViewVersePreview.text = bookmark.preview
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BookmarkItem>() {
        override fun areItemsTheSame(oldItem: BookmarkItem, newItem: BookmarkItem): Boolean =
            oldItem.chapterNumber == newItem.chapterNumber && oldItem.sectionId == newItem.sectionId

        override fun areContentsTheSame(oldItem: BookmarkItem, newItem: BookmarkItem): Boolean =
            oldItem == newItem
    }
}
