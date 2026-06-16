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
import com.shiyuan.fastclaw.databinding.DialogWorkExperienceBinding
import com.shiyuan.fastclaw.databinding.ItemWorkExperienceBinding
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.isNull
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItemFinder
import com.shiyuan.fastclaw.feature.information.EducationalActivity
import com.shiyuan.fastclaw.feature.information.WorkActivity
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

/**
 * 工作经验列表底部弹窗。
 * 展示已有工作经历，点击条目进入编辑页，可新增或完成提交。
 */
class WorkExperienceDialog :
    BaseBottomPushVMDialogFragment<DialogWorkExperienceBinding, InformationViewModel>() {

    companion object {
        const val TAG = "WorkExperienceDialog"

        // 新增按钮回调（保留字段，目前未直接使用，新增逻辑内联在 initView）
        private var onAddClick: (() -> Unit)? = null

        // 条目点击回调
        private var onFieldTreeItemClick: ((AttributeFieldTree, AttributeArrayQueryData) -> Unit)? =
            null

        // 完成按钮回调
        private var onCompleteClick: (() -> Unit)? = null


        /**
         * 显示工作经验弹窗。
         *
         * @param manager FragmentManager。
         * @param attributeData 工作经历字段数据。
         * @param onAddClick 新增按钮回调。
         * @param onFieldTreeItemClick 条目点击回调。
         * @param onCompleteClick 完成按钮回调。
         */
        fun show(
            manager: FragmentManager,
            attributeData: AttributeArrayQueryData? = null,
            onAddClick: (() -> Unit)? = null,
            onFieldTreeItemClick: ((AttributeFieldTree, AttributeArrayQueryData) -> Unit)? = null,
            onCompleteClick: (() -> Unit)? = null,
        ): WorkExperienceDialog {
            this.onAddClick = onAddClick
            this.onFieldTreeItemClick = onFieldTreeItemClick
            this.onCompleteClick = onCompleteClick
            return WorkExperienceDialog().apply {
                arguments = Bundle().apply {
                    attributeData?.let { putParcelable(ARG_ATTRIBUTE_DATA, it) }
                }
                show(manager)
            }
        }

        // 字段编码：未完成标识
        private const val INCOMPLETE = "INCOMPLETE"

        // 字段编码：起止时间、职位、工作内容
        private const val START_DATE = "startDate"
        private const val END_DATE = "endDate"
        private const val POSITION = "position"
        private const val WORK_CONTENT = "workContent"

        // 透传字段数据的参数 key
        private const val ARG_ATTRIBUTE_DATA = "arg_attribute_data"

        // 字段编码：公司名称
        private const val FIELD_COMPANY_NAME = "companyName"

    }

    // 透传进来的工作经历数据
    private var attributeData: AttributeArrayQueryData? = null

    // 工作经历列表适配器
    private val fieldTreeAdapter by lazy {
        SingleBindingAdapter<ItemWorkExperienceBinding, AttributeFieldTree>(
            R.layout.item_work_experience,
            emptyList()
        ) { binding, position2, item ->
            // 公司名称
            val projectName =
                AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_COMPANY_NAME)
            binding.tvCompany.text = projectName?.displayValue ?: "暂无"
            // 未完成态时显示缺失提示
            binding.layoutMissing.visibility =
                if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE
            // 起止时间
            val startDate = AttributeFieldItemFinder.findFirst(item.fieldItems, START_DATE)
            val endDate = AttributeFieldItemFinder.findFirst(item.fieldItems, END_DATE)
            // 职位、工作内容
            val position = AttributeFieldItemFinder.findFirst(item.fieldItems, POSITION)
            val workContent = AttributeFieldItemFinder.findFirst(item.fieldItems, WORK_CONTENT)
            binding.tvTime.text =
                (startDate?.displayValue ?: "") + " - " + (endDate?.displayValue ?: "")
            binding.tvJob.text = position?.displayValue ?: ""
            workContent?.displayValue?.let {
                binding.tvDesc.text = "内容:" + workContent?.displayValue ?: ""
            }

            // 时间为空时整行隐藏
            if (binding.tvTime.text == " - ") {
                binding.tvTime.visibility = View.GONE
                binding.v.visibility = View.GONE
            } else {
                binding.tvTime.visibility = View.VISIBLE
            }

            // 职位为空时整行隐藏
            if (binding.tvJob.text.isNullOrEmpty()) {
                binding.v.visibility = View.GONE
                binding.tvJob.visibility = View.GONE
            } else {
                binding.tvJob.visibility = View.VISIBLE
            }

            // 工作内容为空时隐藏
            if (workContent?.displayValue.isNullOrEmpty()) {
                binding.tvDesc.visibility = View.GONE
            } else {
                binding.tvDesc.visibility = View.VISIBLE
            }
            // 点击进入编辑页
            binding.root.setOnClickListener {
                attributeData?.let { attr ->
                    WorkActivity.start(requireActivity(), true, attr, position2)
                }
            }
        }
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        // 解析参数 → 初始化列表 → 初始化视图 → 绑定数据
        attributeData = getAttributeData()
        initRecyclerView()
        initView()
        bindData()
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
            WorkActivity.start(requireActivity(), false, attributeData, 0)
        }
        // 完成：关闭弹窗并通知调用方
        binding.clWcExperience.setOnSingleClickListener {
            dismiss()
            onCompleteClick?.invoke()
        }

        // 监听子页面保存事件，刷新列表
        observeEvents {
            on<Events.AttributeFieldSuccessfulEvent> {
                try {
                    attributeData = it.attributeData
                    bindData()
                } catch (e: Exception) {

                }
            }
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

    /**
     * 兼容不同 Android 版本的 Parcelable 读取方式。
     */
    @Suppress("DEPRECATION")
    private fun getAttributeData(): AttributeArrayQueryData? {
        val bundle = arguments ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(ARG_ATTRIBUTE_DATA, AttributeArrayQueryData::class.java)
        } else {
            bundle.getParcelable(ARG_ATTRIBUTE_DATA)
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
    }
}

