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
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumTreeUtil
import com.shiyuan.fastclaw.databinding.ActivityIndustrySelectionBinding
import com.shiyuan.fastclaw.feature.information.adapter.IyPnAdapter
import com.shiyuan.fastclaw.feature.information.adapter.IyPnSonAdapter
import com.shiyuan.fastclaw.feature.information.adapter.SearchAdapter
import com.shiyuan.fastclaw.feature.information.adapter.SelectedIyPnAdapter
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import kotlin.collections.distinctBy
import kotlin.collections.plus


/**
 * 行业选择页：左侧一级行业、右侧二级行业网格，
 * 支持单选（搜索 / 列表点击直接回传）和多选（底部已选 + 完成回调）。
 */
class IndustrySelectionActivity :
    BaseViewModelActivity<ActivityIndustrySelectionBinding, InformationViewModel>() {
    companion object {
        // 是否多选模式参数 key
        private const val EXTRA_IS_MULTIPLE = "extra_is_multiple"
        // 会话 ID / 对话 ID
        private const val SESSIONID = "SESSIONID"
        private const val CONVERSATIONID = "CONVERSATIONID"

        /**
         * 启动行业选择页。
         */
        fun start(activity: Activity?, isMultiple: Boolean = false, sessionId: String? = "", conversationId: String? = "") {
            activity ?: return
            val intent = Intent(activity, IndustrySelectionActivity::class.java).apply {
                putExtra(EXTRA_IS_MULTIPLE, isMultiple)
                putExtra(SESSIONID, sessionId)
                putExtra(CONVERSATIONID, conversationId)
            }
            activity.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化视图与点击事件
        initView()
        // 监听 ViewModel 状态变化
        observeEvents()
        // 首次进入请求行业字典
        mRealVM.queryDictIndustryQuery(sessionId,conversationId)
    }


    // 是否多选模式
    private val isMultiple: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_MULTIPLE, false) }
    // 会话 ID / 对话 ID
    private val sessionId: String by lazy { intent.getStringExtra(SESSIONID) ?: "" }
    private val conversationId: String by lazy { intent.getStringExtra(CONVERSATIONID)  ?: ""}
    // 左侧一级行业列表适配器
    private val cyIyPnAdapter: IyPnAdapter by lazy {
        IyPnAdapter( { item, position ->
            // 单选样式：重置其他项的选中态
            mRealVM.allData.value?.forEach { it.isSelected = false }
            item.isSelected = true
            cyIyPnAdapter.notifyDataSetChanged()
            // 切换右侧二级行业
            cyIyPnSonAdapter.submitList(mRealVM.allData.value?.get(position)?.children)
            mRealVM.lastSelectedPosition.value  = position
        },0)
    }
    // 右侧二级行业网格适配器
    private val cyIyPnSonAdapter: IyPnSonAdapter by lazy {
        IyPnSonAdapter( { item, _ ->
            if (isMultiple) {
                // 多选：维护已选集合，最多 3 个
                val selectedList = mRealVM.selectData.value?.toMutableList() ?: mutableListOf()
                if(item.isSelected){
                    item.isSelected  =false
                    mRealVM.selectData.value = selectedList.filterNot { it.code == item.code }
                }else{
                    if (selectedList.size >= 3) {
                        "最多选择三个".showToast()
                        return@IyPnSonAdapter
                    }
                    item.isSelected = true
                    mRealVM.selectData.value = (selectedList + item).distinctBy { it.code }
                }
            } else {
                // 单选：回传并关闭
                item.fieldCode = "industry"
                FlowEventBus.postSticky(Events.CommonEnumItemEvent(item));
                finish()
            }
            cyIyPnSonAdapter.notifyDataSetChanged()
        },0)
    }

    // 底部已选行业横滑标签适配器
    private val selectCyIyPnAdapter: SelectedIyPnAdapter by lazy {
        SelectedIyPnAdapter( { item ->
            // 从已选集合中移除
            val newList = mRealVM.selectData.value?.filterNot { it.code == item.code }
            mRealVM.selectData.value = newList

            // 同步清除右侧列表中的选中态
            mRealVM.allData.value?.forEach { parent ->
                parent.children?.firstOrNull { child -> child.code == item.code }?.isSelected = false
            }
            cyIyPnSonAdapter.notifyDataSetChanged()
        },0)
    }

    // 搜索结果列表适配器
    private val searchAdapter: SearchAdapter by lazy {
        SearchAdapter( { item, position ->
            if (isMultiple) {
                // 多选：直接追加到已选集合（最多 3 个）
                val selectedList = mRealVM.selectData.value?.toMutableList() ?: mutableListOf()
                if (selectedList.size >= 3) {
                    "最多选择三个".showToast()
                    return@SearchAdapter
                }
                item.isSelected = true
                mRealVM.selectData.value = (selectedList + item).distinctBy { it.code }

                // 同步右侧列表中的对应项为选中
                mRealVM.allData.value?.forEach { parent ->
                    parent.children?.firstOrNull { child -> child.code == item.code }?.isSelected = true
                }
                cyIyPnSonAdapter.notifyDataSetChanged()
                // 关闭搜索面板 + 收起键盘
                binding.rlSc.visibility = View.GONE
                hideKeyboard(binding.et)
            } else {
                // 单选：回传并关闭
                FlowEventBus.postSticky(Events.CommonEnumItemEvent(item));
                finish()
            }

        },0){ binding.et.text?.toString().orEmpty() }
    }

    private fun initView() {
        // 多选模式：显示顶部/底部已选区域，并配置横滑列表
        if (isMultiple) {
            binding.tvTitle.text = getString(R.string.choose_expected_industry)
            binding.llMultipleTop.visibility = View.VISIBLE
            binding.llMultipleBot.visibility = View.VISIBLE
            binding.rcSelected.apply {
                layoutManager = LinearLayoutManager(
                    this@IndustrySelectionActivity,
                    RecyclerView.HORIZONTAL,
                    false
                )
                isNestedScrollingEnabled = false
                adapter = selectCyIyPnAdapter
            }
        } else {
            // 单选模式：隐藏多选相关 UI
            binding.tvTitle.text = getString(R.string.industry)
            binding.llMultipleTop.visibility = View.GONE
            binding.llMultipleBot.visibility = View.GONE
        }

        // 左侧一级行业列表
        binding.rcLeft.apply {
            layoutManager = LinearLayoutManager(
                this@IndustrySelectionActivity,
                RecyclerView.VERTICAL,
                false
            )
            adapter = cyIyPnAdapter
        }

        // 右侧二级行业网格（两列）
        binding.rcRight.apply {
            layoutManager = GridLayoutManager(
                this@IndustrySelectionActivity,
                2,
                RecyclerView.VERTICAL,
                false
            )
            adapter = cyIyPnSonAdapter
        }

        // 搜索结果列表
        binding.rcSc.apply {
            layoutManager = LinearLayoutManager(
                this@IndustrySelectionActivity,
                RecyclerView.VERTICAL,
                false
            )
            adapter = searchAdapter
        }
        // 搜索框输入变化：触发搜索 + 控制搜索面板显隐
        binding.et.doAfterTextChanged { editable ->
            val showIcon = !editable.isNullOrEmpty()
            if (showIcon) {
                // 有输入：发起搜索，显示搜索结果面板
                mRealVM.queryDictIndustryQuery(editable.toString(),sessionId,conversationId)
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

        // 底部完成按钮：未选时提示，否则回传并关闭
        binding.layoutPrimary.setOnSingleClickListener {
            val selectedList = mRealVM.selectData.value?.toMutableList() ?: mutableListOf()
            if (selectedList.isEmpty()) {
                "最少选择一个行业".showToast()
                return@setOnSingleClickListener
            }
            val msgGson = CommonEnumTreeUtil.toJson(selectedList)
            FlowEventBus.postSticky(Events.ClawAgentEvent(msgGson))
            finish()
        }
    }

    private fun observeEvents() {
        // 已选集合变化：刷新已选列表、计数和完成按钮
        mRealVM.selectData.observe(this) {
            binding.tvWordCount.text = it.size.toString()
            selectCyIyPnAdapter.submitList(it)
        }

        // 搜索结果变化：空态切换 + 刷新列表
        mRealVM.searchData.observe(this) {
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
        // 一级行业数据变化：刷新左侧分类，并默认选中第二个分类
        mRealVM.allData.observe(this){
            cyIyPnAdapter.submitList(it)

            //默认第 2个
            mRealVM.allData.value?.getOrNull(mRealVM.lastSelectedPosition.value ?: 1)?.also { node ->
                node.isSelected = true
                cyIyPnSonAdapter.submitList(node.children ?: emptyList())
            } ?: cyIyPnSonAdapter.submitList(emptyList())
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
