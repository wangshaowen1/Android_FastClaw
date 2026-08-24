package com.shiyuan.fastclaw.feature.message.chat.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ItemChatJobCardBinding
import com.shiyuan.fastclaw.feature.message.MessageFormatter
import com.shiyuan.fastclaw.feature.message.data.JobChatItem

/**
 * 职位卡片消息 ViewHolder，负责渲染当前会话关联的岗位信息。
 */
class JobChatViewHolder(private val binding: ItemChatJobCardBinding) : RecyclerView.ViewHolder(binding.root) {

    /**
     * 绑定职位卡片标题、薪资、公司信息和标签。
     */
    fun bind(item: JobChatItem) {
        val company = item.brandName?.takeIf { it.isNotBlank() } ?: item.companyName
        val stage = item.companyName?.takeIf { it.isNotBlank() && it != company }
        val tags = listOfNotNull(item.cityName, item.experienceText, item.educationText)
        binding.tvTime.text = item.timeText
        binding.tvTime.visibility = if (item.timeText.isBlank()) View.GONE else View.VISIBLE
        binding.tvTitle.text = item.jobName.orEmpty()
        binding.tvSalary.text = item.salaryText.orEmpty()
        binding.tvSalary.visibility = if (item.salaryText.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvCompany.text = company.orEmpty()
        binding.tvCompany.visibility = if (company.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvStage.text = stage.orEmpty()
        binding.tvStage.visibility = if (stage.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.layoutCompany.visibility = if (company.isNullOrBlank() && stage.isNullOrBlank()) View.GONE else View.VISIBLE
        bindTagViews(tags)
        bindActionText(item)
    }

    /**
     * 绑定职位标签，当前样式最多展示三个标签。
     */
    private fun bindTagViews(tags: List<String>) {
        val tagViews = listOf(binding.tvTagOne, binding.tvTagTwo, binding.tvTagThree)
        tagViews.forEachIndexed { index, view ->
            val tag = tags.getOrNull(index)
            view.text = tag.orEmpty()
            view.visibility = if (tag.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        binding.layoutTags.visibility = if (tags.any { it.isNotBlank() }) View.VISIBLE else View.GONE
    }

    /**
     * 按职业卡片操作方和是否切换职位展示底部动作文案。
     */
    private fun bindActionText(item: JobChatItem) {
        val context = binding.root.context
        val text = if (item.isJobChanged) {
            if (item.operatorRole == OPERATOR_ROLE_CANDIDATE) {
                context.getString(R.string.message_chat_job_changed_by_self)
            } else {
                context.getString(R.string.message_chat_job_changed_by_hr)
            }
        } else {
            val action = if (item.operatorRole == OPERATOR_ROLE_CANDIDATE) {
                context.getString(R.string.message_chat_job_started_by_self)
            } else {
                context.getString(R.string.message_chat_job_started_by_other)
            }
            context.getString(R.string.message_chat_job_started_format, MessageFormatter.format(item.time), action)
        }
        binding.tvActionTime.text = text
        binding.tvActionTime.visibility = View.VISIBLE
        binding.viewDivider.visibility = View.VISIBLE
    }

    companion object {
        // 求职者操作方枚举值。
        private const val OPERATOR_ROLE_CANDIDATE = "candidate"
    }
}
