package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadSuccessBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent简历上传成功卡（UPLOAD_RESUME 的 PARSE_SUCCESS 态）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentUploadSuccessViewHolder(private val binding: ItemMsgAgentUploadSuccessBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, UploadResumePayloadBean::class.java) }.getOrNull()
        }
        // 标题：statusText（如「已上传」），兜底保留布局默认「简历上传成功」
//        payload?.statusText?.takeIf { it.isNotBlank() }?.let { binding.tvTitle.text = it }
        // TODO 对齐度数据（understandingScore / 旧值→新值）文档 UPLOAD_RESUME PARSE_SUCCESS 未定义，
        //  待与后端对齐字段后再绑定 wpvAlignProgress / tvProgressDesc，当前保留布局默认展示
    }

}