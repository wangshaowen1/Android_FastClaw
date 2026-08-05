package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.UserFilePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgUserFileBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import java.util.Locale

/** 用户侧文件卡片（FILE_CARD）。 */
class UserFileViewHolder(private val binding: ItemMsgUserFileBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, UserFilePayloadBean::class.java) }.getOrNull()
        }
        val fileName = payload?.fileName?.takeIf { it.isNotBlank() }
            ?: payload?.resumeName?.takeIf { it.isNotBlank() }
        val fileType = payload?.fileType.orEmpty().uppercase(Locale.ROOT)
        binding.tvFileName.text = fileName
        binding.tvFileType.text = fileType
        binding.ivFileType.setImageResource(resolveFileTypeIcon(fileType))
    }

    private fun resolveFileTypeIcon(fileType: String): Int {
        return when (fileType) {
            "PDF" -> R.drawable.ic_resume
            "DOC",
            "DOCX" -> R.drawable.ic_resume_attachment
            else -> R.drawable.ic_folder
        }
    }
}
