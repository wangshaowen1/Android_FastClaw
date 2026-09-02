package com.shiyuan.fastclaw.feature.message.chat

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ItemChatJobCardBinding
import com.shiyuan.fastclaw.feature.message.data.JobChatItem
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 职位卡片消息 ViewHolder，负责渲染当前会话关联的岗位信息。
 */
class JobChatViewHolder(private val binding: ItemChatJobCardBinding) : RecyclerView.ViewHolder(binding.root) {

    // 职位卡片点击回调。
    var onCardClickListener: (JobChatItem) -> Unit = {}

    /**
     * 绑定职位卡片标题、薪资、公司信息和标签。
     */
    fun bind(item: JobChatItem) {
        val company = item.brandName?.takeIf { it.isNotBlank() } ?: item.companyName
        val stage = item.financingStage?.takeIf { it.isNotBlank() }
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
        bindActionState(item)
        binding.layoutCard.setOnSingleClickListener { onCardClickListener(item) }
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
     * 根据是否切换职位控制卡片底部发起时间和系统提示展示。
     */
    private fun bindActionState(item: JobChatItem) {
        val context = binding.root.context
        binding.tvJobChange.visibility = if (item.isJobChanged) View.VISIBLE else View.GONE
        binding.tvActionTime.visibility = if (item.isJobChanged) View.GONE else View.VISIBLE
        binding.viewDivider.visibility = if (item.isJobChanged) View.GONE else View.VISIBLE
        if (item.isJobChanged) {
            binding.tvJobChange.text = when (item.operatorRole) {
                OPERATOR_ROLE_CANDIDATE -> context.getString(R.string.message_chat_job_changed_by_self)
                OPERATOR_ROLE_HR -> context.getString(R.string.message_chat_job_changed_by_hr)
                else -> context.getString(R.string.message_chat_job_changed_by_hr)
            }
        } else {
            val action = when (item.operatorRole) {
                OPERATOR_ROLE_CANDIDATE -> context.getString(R.string.message_chat_job_started_by_self)
                OPERATOR_ROLE_HR -> context.getString(R.string.message_chat_job_started_by_other)
                else -> context.getString(R.string.message_chat_job_started_by_other)
            }
            binding.tvActionTime.text = context.getString(R.string.message_chat_job_started_format,
                formatActionTime(item.time),
                action
            )
        }
    }

    /**
     * 格式化职位卡片底部发起时间，固定显示月日和时分。
     */
    private fun formatActionTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return SimpleDateFormat("M月d日 H:mm", Locale.getDefault()).format(timestamp)
    }

    companion object {
        // 求职者操作方枚举值。
        private const val OPERATOR_ROLE_CANDIDATE = "candidate"

        // HR 操作方枚举值。
        private const val OPERATOR_ROLE_HR = "hr"
    }
}
