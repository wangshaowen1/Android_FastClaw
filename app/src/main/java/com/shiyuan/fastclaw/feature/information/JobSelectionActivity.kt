package com.shiyuan.fastclaw.feature.information

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.toJsonString
import com.shiyuan.fastclaw.common.utils.KeyboardUtils.hideKeyboard
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.databinding.ActivityIndustrySelectionBinding
import com.shiyuan.fastclaw.databinding.ActivityJobSelectionBinding
import com.shiyuan.fastclaw.feature.information.adapter.IyPnAdapter
import com.shiyuan.fastclaw.feature.information.adapter.IyPnJobSonAdapter
import com.shiyuan.fastclaw.feature.information.adapter.IyPnSonAdapter
import com.shiyuan.fastclaw.feature.information.adapter.SearchAdapter
import com.shiyuan.fastclaw.feature.information.adapter.SelectedIyPnAdapter
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import kotlin.getValue

/**
 * 职位选择页：左侧一级职位分类、右侧二级职位和三级职位列表，
 * 支持单选（搜索 / 列表点击直接回传）和多选（底部已选 + 完成回调）。
 */
class JobSelectionActivity :
    BaseViewModelActivity<ActivityJobSelectionBinding, InformationViewModel>() {
    companion object {
        // 是否多选模式参数 key
        private const val EXTRA_IS_MULTIPLE = "extra_is_multiple"

        // 会话 ID / 对话 ID 参数 key，用于服务端反显
        private const val SESSIONID = "SESSIONID"
        private const val CONVERSATIONID = "CONVERSATIONID"

        /**
         * 启动职位选择页。
         *
         * @param activity 启动源 Activity。
         * @param isMultiple 是否多选模式，多选时显示顶部/底部已选区域。
         * @param sessionId 会话 ID。
         * @param conversationId 对话 ID。
         */
        fun start(activity: Activity?, isMultiple: Boolean = false, sessionId: String? = "", conversationId: String? = "") {
            activity ?: return
            val intent = Intent(activity, JobSelectionActivity::class.java).apply {
                putExtra(EXTRA_IS_MULTIPLE, isMultiple)
                putExtra(JobSelectionActivity.Companion.SESSIONID, sessionId)
                putExtra(JobSelectionActivity.Companion.CONVERSATIONID, conversationId)
            }
            activity.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化视图、点击事件
        initView()
        // 监听 ViewModel 状态变化
        observeEvents()
        // 首次进入请求职位字典
        mRealVM.queryDictPositionQuery(sessionId,conversationId)
    }


    // 是否多选模式
    private val isMultiple: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_MULTIPLE, false) }

    // 当前会话 ID / 对话 ID
    private val sessionId: String by lazy { intent.getStringExtra(JobSelectionActivity.Companion.SESSIONID) ?: "" }
    private val conversationId: String by lazy { intent.getStringExtra(JobSelectionActivity.Companion.CONVERSATIONID)  ?: ""}
    // 左侧一级职位分类列表适配器
    private val cyIyPnAdapter: IyPnAdapter by lazy {
        IyPnAdapter( { item, position ->
            // 单选样式：重置其他项的选中态
            mRealVM.allPositioData.value?.forEach { it.isSelected = false }
            item.isSelected = true
            cyIyPnAdapter.notifyDataSetChanged()
            // 切换右侧二级职位
            cyIyJobPnSonAdapter.submitList(mRealVM.allPositioData.value?.get(position)?.secondCategories)
            mRealVM.lastSelectedPosition2.value = position
        },1)
    }

    // 右侧二级职位下的具体职位网格适配器
    private val cyIyJobPnSonAdapter: IyPnJobSonAdapter by lazy {
        IyPnJobSonAdapter { item, _ ->
            if (isMultiple) {
                // 多选：维护已选集合，最多 3 个
                val selectedList = mRealVM.selectPositionData.value?.toMutableList() ?: mutableListOf()
                if (item.isSelected) {
                    item.isSelected = false
                    mRealVM.selectPositionData.value = selectedList.filterNot { it.positionCode == item.positionCode }
                } else {
                    if (selectedList.size >= 3) {
                        "最多选择三个".showToast()
                        return@IyPnJobSonAdapter
                    }
                    item.isSelected = true
                    mRealVM.selectPositionData.value = (selectedList + item).distinctBy { it.positionCode }
                }
            } else {
                // 单选：回传选中项并关闭页面
                item.fieldCode = "position"
                FlowEventBus.postSticky(Events.CommonEnumItemEvent(item));
                finish()
            }
            cyIyJobPnSonAdapter.notifyDataSetChanged()
        }
    }

    // 底部已选职位横滑标签适配器
    private val selectCyIyPnAdapter: SelectedIyPnAdapter by lazy {
        SelectedIyPnAdapter( { item ->
            // 从已选集合中移除
            val newList = mRealVM.selectPositionData.value?.filterNot { it.positionCode == item.positionCode }
            mRealVM.selectPositionData.value = newList

            // 同步清除右侧列表中的选中态
            mRealVM.allPositioData.value?.forEach { parent ->
                parent.secondCategories?.forEach { secondCategory ->
                    secondCategory.positions?.firstOrNull { child -> child.positionCode == item.positionCode }?.isSelected = false
                }
            }
            cyIyJobPnSonAdapter.notifyDataSetChanged()
        },1)
    }

    // 搜索结果列表适配器
    private val searchAdapter: SearchAdapter by lazy {
        SearchAdapter( { item, position ->
            if (isMultiple) {
                // 多选：直接追加到已选集合（最多 3 个）
                val selectedList = mRealVM.selectPositionData.value?.toMutableList() ?: mutableListOf()
                if (selectedList.size >= 3) {
                    "最多选择三个".showToast()
                    return@SearchAdapter
                }
                item.isSelected = true
                mRealVM.selectPositionData.value = (selectedList + item).distinctBy { it.positionCode }

                // 同步右侧列表中的对应项为选中
                mRealVM.allPositioData.value?.forEach { parent ->
                    parent.secondCategories?.forEach { secondCategory ->
                        secondCategory.positions?.firstOrNull { child -> child.positionCode == item.positionCode }?.isSelected =
                            true
                    }
                }
                cyIyJobPnSonAdapter.notifyDataSetChanged()
                // 关闭搜索面板 + 收起键盘
                binding.rlSc.visibility = View.GONE
                hideKeyboard(binding.et)
            } else {
                // 单选：回传并关闭
                FlowEventBus.postSticky(Events.CommonEnumItemEvent(item));
                finish()
            }

        }, 1) { binding.et.text?.toString().orEmpty() }
    }

    private fun initView() {
        // 多选模式：显示顶部/底部已选区域，并配置横滑列表
        if (isMultiple) {
            binding.tvTitle.text = getString(R.string.choose_desired_position)
            binding.llMultipleTop.visibility = View.VISIBLE
            binding.llMultipleBot.visibility = View.VISIBLE
            binding.rcSelected.apply {
                layoutManager = LinearLayoutManager(
                    this@JobSelectionActivity,
                    RecyclerView.HORIZONTAL,
                    false
                )
                isNestedScrollingEnabled = false
                adapter = selectCyIyPnAdapter
            }
        } else {
            // 单选模式：隐藏多选相关 UI
            binding.tvTitle.text = getString(R.string.choose_desired_position)
            binding.llMultipleTop.visibility = View.GONE
            binding.llMultipleBot.visibility = View.GONE
        }

        // 左侧一级分类列表
        binding.rcLeft.apply {
            layoutManager = LinearLayoutManager(
                this@JobSelectionActivity,
                RecyclerView.VERTICAL,
                false
            )
            adapter = cyIyPnAdapter
        }

        // 右侧二级职位 + 嵌入式三级职位网格
        binding.rcRight.apply {
            layoutManager = LinearLayoutManager(
                this@JobSelectionActivity,
                RecyclerView.VERTICAL,
                false
            )
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
            itemAnimator = null
            adapter = cyIyJobPnSonAdapter
        }

        // 搜索结果列表
        binding.rcSc.apply {
            layoutManager = LinearLayoutManager(
                this@JobSelectionActivity,
                RecyclerView.VERTICAL,
                false
            )
            adapter = searchAdapter
        }
        // 搜索框输入变化：触发搜索 + 控制搜索面板显隐
        binding.et.doAfterTextChanged { editable ->
            val keyword = editable?.toString().orEmpty()
            val showIcon = keyword.isNotEmpty()
            if (showIcon) {
                // 有输入：发起搜索，显示搜索结果面板
                mRealVM.queryDictPositionQuery(keyword,sessionId,conversationId)
                binding.ivClear.visibility = View.VISIBLE
                binding.rlSc.visibility = View.VISIBLE
            } else {
                // 无输入：清空搜索面板
                binding.rlSc.visibility = View.GONE
                binding.ivClear.visibility = View.GONE
            }
        }
        // 搜索框清空按钮
        binding.ivClear.setOnSingleClickListener {
            binding.rlSc.visibility = View.GONE
            binding.et.text.clear()
        }
        // 返回
        binding.ivBack.setOnSingleClickListener { finish() }

        // 底部完成按钮：把已选职位以简化结构回传给上游，并关闭页面
        binding.layoutPrimary.setOnSingleClickListener {
            val selectedList = mRealVM.selectPositionData.value?.toMutableList() ?: mutableListOf()
            if (selectedList.isEmpty()) {
                "最少选择一个".showToast()
                return@setOnSingleClickListener
            }
            mRealVM.selectPositionData.value?.toJsonString()?.let { msg -> Log.d("layoutPrimary", msg) }
            val categoryList = mRealVM.selectPositionData.value?.map {
                mapOf("code" to it.positionCode, "name" to it.positionName)
            }
            categoryList?.toJsonString()?.let { msg -> Log.d("layoutPrimary", msg) }
            FlowEventBus.postSticky(Events.ClawAgentEvent(categoryList?.toJsonString(), AppEvent.KLX_C_EXPECTED_JOB_COMPLETED))
            finish()
        }
    }



    private fun observeEvents() {
        // 已选集合变化：刷新已选列表、计数和完成按钮
        mRealVM.selectPositionData.observe(this) {
            binding.tvWordCount.text = it.size.toString()
            selectCyIyPnAdapter.submitList(it)
            cyIyJobPnSonAdapter.notifyDataSetChanged()
        }

        // 搜索结果变化：空态切换 + 刷新列表
        mRealVM.searchPositionData.observe(this) {
            if(it.isNullOrEmpty()){
                // 无结果：显示空态文案
              binding.tvEm.visibility = View.VISIBLE
              binding.rcSc.visibility = View.GONE
            }else{
                binding.tvEm.visibility = View.GONE
                binding.rcSc.visibility = View.VISIBLE
            }
            searchAdapter.submitList(it)
        }
        // 一级职位数据变化：刷新左侧分类，并默认选中第二个分类
        mRealVM.allPositioData.observe(this){
            cyIyPnAdapter.submitList(it)
            Log.d("node.allPositioData", it?.toJsonString().toString())

            //默认第 2个
            mRealVM.allPositioData.value?.getOrNull(mRealVM.lastSelectedPosition2.value ?: 1)?.also { node ->
                node.isSelected = true
                Log.d("node.secondCategories", node.secondCategories?.toJsonString().toString())
                cyIyJobPnSonAdapter.submitList(node.secondCategories ?: emptyList())
            } ?: cyIyJobPnSonAdapter.submitList(emptyList())
        }
        observeEvents {

        }
    }

    override fun setTranslucentStatus() {
        // 设置沉浸式状态栏
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

}
