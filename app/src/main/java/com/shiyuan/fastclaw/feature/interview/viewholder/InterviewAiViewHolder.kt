package com.shiyuan.fastclaw.feature.interview.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemInterviewAiBinding
import com.shiyuan.fastclaw.feature.interview.bean.InterviewMessageInfo

class InterviewAiViewHolder(private val binding: ItemInterviewAiBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: InterviewMessageInfo) {
        bindContent(item.content)
    }

    fun bindContent(content: String?) {
        binding.tvContent.text = content
    }
}
