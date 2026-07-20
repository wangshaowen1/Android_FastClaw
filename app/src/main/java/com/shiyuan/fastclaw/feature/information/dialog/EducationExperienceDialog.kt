package com.shiyuan.fastclaw.feature.information.dialog

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
import com.shiyuan.fastclaw.databinding.DialogEducationExperienceBinding
import com.shiyuan.fastclaw.databinding.ItemEducationExperienceBinding
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.isNull
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.base.BaseBottomDialogFragment
import com.shiyuan.fastclaw.base.BaseBottomDialogVmFragment
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItemFinder
import com.shiyuan.fastclaw.feature.information.EducationalActivity
import com.shiyuan.fastclaw.feature.information.ProjectActivity
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

/**
 * 教育经历列表底部弹窗。
 * 展示已有教育经历，点击条目进入编辑页，可新增或完成提交。
 */
class EducationExperienceDialog : BaseBottomDialogVmFragment<DialogEducationExperienceBinding, InformationViewModel>() {

    companion object {
        const val TAG = "EducationExperienceDialog"
        // 新增按钮回调（保留字段，新增逻辑内联在 initView）
        private var onAddClick: (() -> Unit)? = null
        // 条目点击回调
        private var onFieldTreeItemClick: ((AttributeFieldTree, AttributeArrayQueryData) -> Unit)? = null
        // 完成按钮回调
        private var onCompleteClick: (() -> Unit)? = null
        private var questionQueueId: String? = null
        private var attributeGroupCode: String? = null
        private var clientRequestId: String? = null
        private var sessionId: String? = null
        private var conversationId: String? = null


        /**
         * 显示教育经历弹窗。
         */
        fun show(
            manager: FragmentManager,
            questionQueueId: String? = null,
            attributeGroupCode: String? = null,
            clientRequestId: String? = null,
            sessionId: String? = null,
            conversationId: String? = null,
            onAddClick: (() -> Unit)? = null,
            onFieldTreeItemClick: ((AttributeFieldTree, AttributeArrayQueryData) -> Unit)? = null,
            onCompleteClick: (() -> Unit)? = null,
        ): EducationExperienceDialog {
            this.onAddClick = onAddClick
            this.questionQueueId = questionQueueId
            this.attributeGroupCode = attributeGroupCode
            this.clientRequestId = clientRequestId
            this.sessionId = sessionId
            this.conversationId = conversationId
            this.onFieldTreeItemClick = onFieldTreeItemClick
            this.onCompleteClick = onCompleteClick
            return EducationExperienceDialog().apply {
                show(manager)
            }
        }

        // 透传字段数据的参数 key
        private const val ARG_ATTRIBUTE_DATA = "arg_attribute_data"
        // 字段编码：未完成标识
        private const val INCOMPLETE = "INCOMPLETE"
        // 字段编码：起止时间、专业
        private const val START_DATE = "startDate"
        private const val END_DATE = "endDate"
        private const val MAJOR = "major"

        // 字段编码：学校名称
        private const val FIELD_SCHOOL_NAME = "schoolName"

    }

    // 透传进来的教育经历数据
    private var attributeData: AttributeArrayQueryData? = null

    // 教育经历列表适配器
    private val fieldTreeAdapter by lazy {
        SingleBindingAdapter<ItemEducationExperienceBinding, AttributeFieldTree>(
            R.layout.item_education_experience,
            emptyList()
        ) { binding, position2, item ->
            // 学校名称
            val  projectName = AttributeFieldItemFinder.findFirst(item.fieldItems,FIELD_SCHOOL_NAME)
            binding.tvCompany.text = projectName?.displayValue ?:  "暂无"
            // 未完成态时显示缺失提示
            binding.layoutMissing.visibility = if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE
            // 起止时间、专业
            val startDate = AttributeFieldItemFinder.findFirst(item.fieldItems, START_DATE)
            val endDate = AttributeFieldItemFinder.findFirst(item.fieldItems, END_DATE)
            val major = AttributeFieldItemFinder.findFirst(item.fieldItems, MAJOR)
            binding.tvTime.text = (startDate?.displayValue ?: "") + " - " + (endDate?.displayValue ?: "")
            binding.tvMajor.text = major?.displayValue ?: ""

            // 时间为空时整行隐藏
            if (binding.tvTime.text == " - ") {
                binding.tvTime.visibility = View.GONE
                binding.v.visibility = View.GONE
            } else {
                binding.tvTime.visibility = View.VISIBLE
            }

            // 专业为空时整行隐藏
            if (binding.tvMajor.text.isNullOrEmpty()) {
                binding.v.visibility = View.GONE
                binding.tvMajor.visibility = View.GONE
            } else {
                binding.tvMajor.visibility = View.VISIBLE
            }

            // 点击进入编辑页
            binding.root.setOnClickListener {
                attributeData?.let { attr ->
                    EducationalActivity.start(requireActivity(), true, attr, position2, clientRequestId)
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
        // 关闭按钮
        binding.ivClose.setOnSingleClickListener { dismiss() }
        // 新增：复制首条记录清空字段值，并打开新增页
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
            EducationalActivity.start(requireActivity(), false, attributeData,0, clientRequestId)
        }
        // 完成：关闭弹窗并通知调用方
        binding.clWcExperience.setOnSingleClickListener {
            if (attributeData?.fieldTree?.filter { it.statusCode == INCOMPLETE }.isNullOrEmpty()) {
                dismiss()
                onCompleteClick?.invoke()
            } else {
                SupplementDialog.show(
                    manager = childFragmentManager,
                    content = "还有教育经历未补充完整",
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
