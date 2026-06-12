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


class IndustrySelectionActivity :
    BaseViewModelActivity<ActivityIndustrySelectionBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_MULTIPLE = "extra_is_multiple"
        private const val SESSIONID = "SESSIONID"
        private const val CONVERSATIONID = "CONVERSATIONID"

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
        initView()
        observeEvents()
        mRealVM.queryDictIndustryQuery(sessionId,conversationId)
    }


    private val isMultiple: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_MULTIPLE, false) }
    private val sessionId: String by lazy { intent.getStringExtra(SESSIONID) ?: "" }
    private val conversationId: String by lazy { intent.getStringExtra(CONVERSATIONID)  ?: ""}
    private val cyIyPnAdapter: IyPnAdapter by lazy {
        IyPnAdapter( { item, position ->
            mRealVM.allData.value?.forEach { it.isSelected = false }
            item.isSelected = true
            cyIyPnAdapter.notifyDataSetChanged()
            cyIyPnSonAdapter.submitList(mRealVM.allData.value?.get(position)?.children)
            mRealVM.lastSelectedPosition.value  = position
        },0)
    }
    private val cyIyPnSonAdapter: IyPnSonAdapter by lazy {
        IyPnSonAdapter( { item, _ ->
            if (isMultiple) {
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
                item.fieldCode = "industry"
                FlowEventBus.postSticky(Events.CommonEnumItemEvent(item));
                finish()
            }
            cyIyPnSonAdapter.notifyDataSetChanged()
        },0)
    }

    private val selectCyIyPnAdapter: SelectedIyPnAdapter by lazy {
        SelectedIyPnAdapter( { item ->
            val newList = mRealVM.selectData.value?.filterNot { it.code == item.code }
            mRealVM.selectData.value = newList

            mRealVM.allData.value?.forEach { parent ->
                parent.children?.firstOrNull { child -> child.code == item.code }?.isSelected = false
            }
            cyIyPnSonAdapter.notifyDataSetChanged()
        },0)
    }

    private val searchAdapter: SearchAdapter by lazy {
        SearchAdapter( { item, position ->
            if (isMultiple) {
                val selectedList = mRealVM.selectData.value?.toMutableList() ?: mutableListOf()
                if (selectedList.size >= 3) {
                    "最多选择三个".showToast()
                    return@SearchAdapter
                }
                item.isSelected = true
                mRealVM.selectData.value = (selectedList + item).distinctBy { it.code }

                mRealVM.allData.value?.forEach { parent ->
                    parent.children?.firstOrNull { child -> child.code == item.code }?.isSelected = true
                }
                cyIyPnSonAdapter.notifyDataSetChanged()
                binding.rlSc.visibility = View.GONE
                hideKeyboard(binding.et)
            } else {
                FlowEventBus.postSticky(Events.CommonEnumItemEvent(item));
                finish()
            }

        },0){ binding.et.text?.toString().orEmpty() }
    }

    private fun initView() {
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
            binding.tvTitle.text = getString(R.string.choose_desired_position)
            binding.llMultipleTop.visibility = View.GONE
            binding.llMultipleBot.visibility = View.GONE
        }

        binding.rcLeft.apply {
            layoutManager = LinearLayoutManager(
                this@IndustrySelectionActivity,
                RecyclerView.VERTICAL,
                false
            )
            adapter = cyIyPnAdapter
        }

        binding.rcRight.apply {
            layoutManager = GridLayoutManager(
                this@IndustrySelectionActivity,
                2,
                RecyclerView.VERTICAL,
                false
            )
            adapter = cyIyPnSonAdapter
        }

        binding.rcSc.apply {
            layoutManager = LinearLayoutManager(
                this@IndustrySelectionActivity,
                RecyclerView.VERTICAL,
                false
            )
            adapter = searchAdapter
        }
        binding.et.doAfterTextChanged { editable ->
            val showIcon = !editable.isNullOrEmpty()
            if (showIcon) {
                mRealVM.queryDictIndustryQuery(sessionId,conversationId,editable.toString())
                binding.ivClear.visibility = View.VISIBLE
                binding.rlSc.visibility = View.VISIBLE
            } else {
                binding.rlSc.visibility = View.GONE
                binding.ivClear.visibility = View.GONE
            }
        }
        binding.ivClear.setOnSingleClickListener {
            binding.rlSc.visibility = View.GONE
            binding.et.text.clear()
        }
        binding.ivBack.setOnSingleClickListener { finish() }

        updateCompleteButtonState(emptyList())

        binding.layoutPrimary.setOnSingleClickListener {
            val selectedList = mRealVM.selectData.value?.toMutableList() ?: mutableListOf()
            if (selectedList.isEmpty()) {
                "最少选择一个".showToast()
                return@setOnSingleClickListener
            }
            val msgGson = CommonEnumTreeUtil.toJson(selectedList)
            FlowEventBus.postSticky(Events.ClawAgentEvent(msgGson))
            finish()
        }
    }

    private fun updateCompleteButtonState(selectedList: List<com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem>) {
        val hasSelection = selectedList.isNotEmpty()
        binding.layoutPrimary.isEnabled = hasSelection
        binding.layoutPrimary.alpha = if (hasSelection) 1f else 0.3f
    }

    private fun observeEvents() {
        mRealVM.selectData.observe(this) {
            binding.tvWordCount.text = it.size.toString()
            selectCyIyPnAdapter.submitList(it)
            updateCompleteButtonState(it)
        }

        mRealVM.searchData.observe(this) {
            if(it.isNullOrEmpty()){
              binding.tvEm.visibility = View.VISIBLE
              binding.rcSc.visibility = View.GONE
            }else{
                binding.tvEm.visibility = View.GONE
                binding.rcSc.visibility = View.VISIBLE
            }
            searchAdapter.submitList(it)
        }
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
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

}
