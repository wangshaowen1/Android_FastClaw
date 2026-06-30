package com.shiyuan.fastclaw.feature.interview.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemInterviewUserBinding
import com.shiyuan.fastclaw.feature.interview.bean.InterviewMessageInfo

class InterviewUserViewHolder(private val binding: ItemInterviewUserBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: InterviewMessageInfo) {
        bindContent(item.content)
    }

    fun bindContent(content: String?) {
        binding.tvContent.text = content
    }
}
