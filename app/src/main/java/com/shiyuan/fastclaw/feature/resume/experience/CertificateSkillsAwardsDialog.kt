package com.shiyuan.fastclaw.feature.resume.experience

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldTree
import com.shiyuan.fastclaw.databinding.DialogCertificateSkillsAwardsBinding
import com.shiyuan.fastclaw.databinding.ItemCertificateSkillsAwardsBinding
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.base.BaseBottomDialogFragment
import com.shiyuan.fastclaw.base.BaseBottomDialogVmFragment
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItemFinder
import com.shiyuan.fastclaw.feature.resume.InformationViewModel

/**
 * 技能 / 证书 / 奖项通用列表底部弹窗。
 * 通过 type 区分当前展示的是哪一类数据，渲染不同字段项与点击跳转目标。
 */
class CertificateSkillsAwardsDialog : BaseBottomDialogVmFragment<DialogCertificateSkillsAwardsBinding, InformationViewModel>() {

    companion object {
        const val TAG = "CertificateSkillsAwardsDialog"
        // 弹窗类型：skills / certificates / awards
        private var onAddClick: ((String) -> Unit)? = null
        // 完成按钮回调
        private var onCompleteClick: (() -> Unit)? = null
        // 条目点击回调
        private var onFieldTreeItemClick: ((AttributeFieldTree, AttributeArrayQueryData) -> Unit)? = null
        // 弹窗类型变量：certificates / skills / awards
        private var type: String = "certificates"
        private var questionQueueId: String? = null
        private var attributeGroupCode: String? = null
        private var clientRequestId: String? = null
        private var sessionId: String? = null
        private var conversationId: String? = null

        // 字段编码：技能名称
        private const val FIELD_SKILL_NAME = "skillName"


        /**
         * 显示技能 / 证书 / 奖项弹窗。
         *
         * @param manager FragmentManager。
         * @param attributeData 字段数据。
         * @param type 弹窗类型：certificates / skills / awards。
         * @param onCompleteClick 完成按钮回调。
         * @param onAddClick 新增按钮回调。
         * @param onFieldTreeItemClick 条目点击回调。
         */
        fun show(
            manager: FragmentManager,
            type: String = "certificates",
            questionQueueId: String? = null,
            attributeGroupCode: String? = null,
            clientRequestId: String? = null,
            sessionId: String? = null,
            conversationId: String? = null,
            onCompleteClick: (() -> Unit)? = null,
            onAddClick: ((String) -> Unit)? = null,
            onFieldTreeItemClick: ((AttributeFieldTree, AttributeArrayQueryData) -> Unit)? = null,
        ): CertificateSkillsAwardsDialog {
            this.onAddClick = onAddClick
            this.onFieldTreeItemClick = onFieldTreeItemClick
            this.onCompleteClick = onCompleteClick
            this.type = type
            this.questionQueueId = questionQueueId
            this.attributeGroupCode = attributeGroupCode
            this.clientRequestId = clientRequestId
            this.sessionId = sessionId
            this.conversationId = conversationId
            return CertificateSkillsAwardsDialog().apply {
                show(manager)
            }
        }

        // 透传字段数据的参数 key
        private const val ARG_ATTRIBUTE_DATA = "arg_attribute_data"
        // 字段编码：未完成标识
        private const val INCOMPLETE = "INCOMPLETE"
        // 字段编码：证书名称
        private const val FIELD_CERTIFICATE_NAME = "certificateName"
        // 字段编码：奖项名称
        private const val FIELD_AWARD_NAME = "awardName"

    }

    // 透传进来的字段数据
    private var attributeData: AttributeArrayQueryData? = null

    // 列表适配器
    private val fieldTreeAdapter by lazy {
        SingleBindingAdapter<ItemCertificateSkillsAwardsBinding, AttributeFieldTree>(
            R.layout.item_certificate_skills_awards,
            emptyList()
        ) { binding, position, item ->
            // 未完成态时显示缺失提示
            binding.layoutMissing.visibility = if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE
            // 根据 type 选择展示的字段：技能 / 证书 / 奖项
            when (type) {
                "skills" -> {
                    val  projectName = AttributeFieldItemFinder.findFirst(item.fieldItems,FIELD_SKILL_NAME)
                    binding.tvName.text = projectName?.displayValue ?:  "暂无"
                }

                "certificates" -> {
                    val  projectName = AttributeFieldItemFinder.findFirst(item.fieldItems,FIELD_CERTIFICATE_NAME)
                    binding.tvName.text = projectName?.displayValue ?:  "暂无"
                }
                else -> {
                    val  projectName = AttributeFieldItemFinder.findFirst(item.fieldItems,FIELD_AWARD_NAME)
                    binding.tvName.text = projectName?.displayValue ?:  "暂无"
                }
            }
            // 点击根据 type 跳到对应编辑页
            binding.root.setOnClickListener {
                attributeData?.let { attr ->
                    when (type) {
                        "skills" -> {
                            SkillActivity.start(requireActivity(), true, attr, position,clientRequestId)
                        }
                        "certificates" -> {
                            CertificateActivity.start(requireActivity(), true, attr, position,clientRequestId)
                        }
                        else -> {
                            AwardActivity.start(requireActivity(), true, attr, position,clientRequestId)
                        }
                    }
                }
            }
        }
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initRecyclerView()
        initView()
        queryAttributeArray()
    }

    /**
     * 初始化列表布局和间距装饰。
     */
    private fun initRecyclerView() {
        binding.rvExperience.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        // 避免重复添加
        if (binding.rvExperience.itemDecorationCount == 0) {
            binding.rvExperience.addItemDecoration(SpaceDecoration(12.dp))
        }
    }

    private fun initView() {
        // 根据 type 切换标题
        when (type) {
            "skills" -> {
                binding.tvTitle.text = "技能"
            }
            "certificates" -> {
                binding.tvTitle.text = "证书"
            }
            else -> {
                binding.tvTitle.text = "奖项"
            }
        }
        // 关闭按钮
        binding.ivClose.setOnSingleClickListener { dismiss() }
        // 新增：复制首条记录清空字段值，按 type 跳到对应编辑页
        binding.clAddExperience.setOnSingleClickListener {
            attributeData?.fieldTree?.let { treeList ->
                // 1. 取出第0个元素（如果存在）作为模板
                val original = treeList.getOrNull(0) ?: return@let

                // 2. 深拷贝 original（通过序列化方式最安全，此处展示手动递归拷贝）
                val copy = original.copy(
                    fieldItems = original.fieldItems?.map { item ->
                        item.copy(
                            fieldValue = null,      // 直接置 null
                            displayValue = null
                        )
                    }?.toMutableList()
                )
                // 3. 将副本插入到列表第0个位置
                treeList.add(0, copy)
            }
            when (type) {
                "skills" -> {
                    SkillActivity.start(requireActivity(), false, attributeData,0,clientRequestId)
                }
                "certificates" -> {
                    CertificateActivity.start(requireActivity(), false, attributeData,0,clientRequestId)
                }

                else -> {
                    AwardActivity.start(requireActivity(), false, attributeData,0,clientRequestId)
                }
            }
        }
        // 完成：关闭弹窗并通知调用方
        binding.clWcExperience.setOnSingleClickListener {
            if (attributeData?.fieldTree?.filter { it.statusCode == INCOMPLETE }.isNullOrEmpty()) {
                dismiss()
                onCompleteClick?.invoke()
            } else {
                SupplementDialog.show(
                    manager = childFragmentManager,
                    content = "还有${getSupplementTitle()}未补充完整",
                    onLater = {
                        dismiss()
                    }
                )
            }
        }
        // 监听子页面保存事件，刷新列表
        observeEvents {
            on<Events.AttributeFieldSuccessfulEvent> {
                try {
                    queryAttributeArray()
                }catch (e: Exception){

                }
            }
        }
    }

    private fun queryAttributeArray() {
        if (questionQueueId == null && attributeGroupCode == null && sessionId == null && conversationId == null) {
            bindData()
            return
        }
        mRealVM.queryAttributeArray(
            questionQueueId,
            attributeGroupCode,
            sessionId,
            conversationId,
            clientRequestId
        ) {
            attributeData = it ?: attributeData
            bindData()
        }
    }

    private fun getSupplementTitle(): String {
        return when (type) {
            "skills" -> "技能"
            "certificates" -> "证书"
            else -> "奖项"
        }
    }

    /**
     * 绑定列表数据到适配器。
     */
    private fun bindData() {
        val fieldTree = attributeData?.fieldTree ?: emptyList()
        binding.rvExperience.adapter = fieldTreeAdapter
        @SuppressLint("NotifyDataSetChanged")
        fieldTreeAdapter.submitList(fieldTree)
    }


    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
    }
}
