package com.shiyuan.fastclaw.feature.interview

import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemInterviewUserBinding

class InterviewUserViewHolder(private val binding: ItemInterviewUserBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: InterviewMessageInfo) {
        bindContent(item.content)
    }

    fun bindContent(content: String?) {
        binding.tvContent.text = content
    }
}
