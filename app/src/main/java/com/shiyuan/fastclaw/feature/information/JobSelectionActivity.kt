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

class JobSelectionActivity :
    BaseViewModelActivity<ActivityJobSelectionBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_MULTIPLE = "extra_is_multiple"

        private const val SESSIONID = "SESSIONID"
        private const val CONVERSATIONID = "CONVERSATIONID"

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
        initView()
        observeEvents()
        mRealVM.queryDictPositionQuery(sessionId,conversationId)
    }


    private val isMultiple: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_MULTIPLE, false) }

    private val sessionId: String by lazy { intent.getStringExtra(JobSelectionActivity.Companion.SESSIONID) ?: "" }
    private val conversationId: String by lazy { intent.getStringExtra(JobSelectionActivity.Companion.CONVERSATIONID)  ?: ""}
    private val cyIyPnAdapter: IyPnAdapter by lazy {
        IyPnAdapter( { item, position ->
            mRealVM.allPositioData.value?.forEach { it.isSelected = false }
            item.isSelected = true
            cyIyPnAdapter.notifyDataSetChanged()
            cyIyJobPnSonAdapter.submitList(mRealVM.allPositioData.value?.get(position)?.secondCategories)
            mRealVM.lastSelectedPosition2.value = position
        },1)
    }

    private val cyIyJobPnSonAdapter: IyPnJobSonAdapter by lazy {
        IyPnJobSonAdapter { item, _ ->
            if (isMultiple) {
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
                item.fieldCode = "position"
                FlowEventBus.postSticky(Events.CommonEnumItemEvent(item));
                finish()
            }
            cyIyJobPnSonAdapter.notifyDataSetChanged()
        }
    }

    private val selectCyIyPnAdapter: SelectedIyPnAdapter by lazy {
        SelectedIyPnAdapter( { item ->
            val newList = mRealVM.selectPositionData.value?.filterNot { it.positionCode == item.positionCode }
            mRealVM.selectPositionData.value = newList

            mRealVM.allPositioData.value?.forEach { parent ->
                parent.secondCategories?.forEach { secondCategory ->
                    secondCategory.positions?.firstOrNull { child -> child.positionCode == item.positionCode }?.isSelected = false
                }
            }
            cyIyJobPnSonAdapter.notifyDataSetChanged()
        },1)
    }

    private val searchAdapter: SearchAdapter by lazy {
        SearchAdapter( { item, position ->
            if (isMultiple) {
                val selectedList = mRealVM.selectPositionData.value?.toMutableList() ?: mutableListOf()
                if (selectedList.size >= 3) {
                    "最多选择三个".showToast()
                    return@SearchAdapter
                }
                item.isSelected = true
                mRealVM.selectPositionData.value = (selectedList + item).distinctBy { it.positionCode }

                mRealVM.allPositioData.value?.forEach { parent ->
                    parent.secondCategories?.forEach { secondCategory ->
                        secondCategory.positions?.firstOrNull { child -> child.positionCode == item.positionCode }?.isSelected =
                            true
                    }
                }
                cyIyJobPnSonAdapter.notifyDataSetChanged()
                binding.rlSc.visibility = View.GONE
                hideKeyboard(binding.et)
            } else {

                FlowEventBus.postSticky(Events.CommonEnumItemEvent(item));
                finish()
            }

        }, 1) { binding.et.text?.toString().orEmpty() }
    }

    private fun initView() {
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
            binding.tvTitle.text = getString(R.string.choose_desired_position)
            binding.llMultipleTop.visibility = View.GONE
            binding.llMultipleBot.visibility = View.GONE
        }

        binding.rcLeft.apply {
            layoutManager = LinearLayoutManager(
                this@JobSelectionActivity,
                RecyclerView.VERTICAL,
                false
            )
            adapter = cyIyPnAdapter
        }

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

        binding.rcSc.apply {
            layoutManager = LinearLayoutManager(
                this@JobSelectionActivity,
                RecyclerView.VERTICAL,
                false
            )
            adapter = searchAdapter
        }
        binding.et.doAfterTextChanged { editable ->
            val keyword = editable?.toString().orEmpty()
            val showIcon = keyword.isNotEmpty()
            if (showIcon) {
                mRealVM.queryDictPositionQuery(keyword,sessionId,conversationId)
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
            mRealVM.selectPositionData.value?.toJsonString()?.let { msg -> Log.d("layoutPrimary", msg) }
            val categoryList = mRealVM.selectPositionData.value?.map {
                mapOf("code" to it.positionCode, "name" to it.positionName)
            }
            categoryList?.toJsonString()?.let { msg -> Log.d("layoutPrimary", msg) }
            FlowEventBus.postSticky(Events.ClawAgentEvent(categoryList?.toJsonString(), AppEvent.KLX_C_EXPECTED_JOB_COMPLETED))
            finish()
        }
    }

    private fun updateCompleteButtonState(selectedList: List<com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem>) {
        val hasSelection = selectedList.isNotEmpty()
        binding.layoutPrimary.isEnabled = hasSelection
        binding.layoutPrimary.alpha = if (hasSelection) 1f else 0.3f
    }

    private fun observeEvents() {
        mRealVM.selectPositionData.observe(this) {
            binding.tvWordCount.text = it.size.toString()
            selectCyIyPnAdapter.submitList(it)
            cyIyJobPnSonAdapter.notifyDataSetChanged()
            updateCompleteButtonState(it)
        }

        mRealVM.searchPositionData.observe(this) {
            if(it.isNullOrEmpty()){
              binding.tvEm.visibility = View.VISIBLE
              binding.rcSc.visibility = View.GONE
            }else{
                binding.tvEm.visibility = View.GONE
                binding.rcSc.visibility = View.VISIBLE
            }
            searchAdapter.submitList(it)
        }
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
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

}