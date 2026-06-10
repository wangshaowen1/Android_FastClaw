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
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumTreeUtil
import com.shiyuan.fastclaw.databinding.ActivityIndustrySelectionBinding
import com.shiyuan.fastclaw.feature.information.adapter.IyPnAdapter
import com.shiyuan.fastclaw.feature.information.adapter.IyPnSonAdapter
import com.shiyuan.fastclaw.feature.information.adapter.SelectedIyPnAdapter
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel


class IndustrySelectionActivity :
    BaseViewModelActivity<ActivityIndustrySelectionBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_MULTIPLE = "extra_is_multiple"

        fun start(activity: Activity?, isMultiple: Boolean = false) {
            activity ?: return
            val intent = Intent(activity, IndustrySelectionActivity::class.java).apply {
                putExtra(EXTRA_IS_MULTIPLE, isMultiple)
            }
            activity.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeEvents()
        mRealVM.queryDictIndustryQuery()
    }


    private val isMultiple: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_MULTIPLE, false) }
    private val cyIyPnAdapter: IyPnAdapter by lazy {
        IyPnAdapter { item, position ->
            mRealVM.levelData.value?.forEach { it.isSelected = false }
            item.isSelected = true
            cyIyPnAdapter.notifyDataSetChanged()
            mRealVM.lastSelectedPosition.value = position
        }
    }
    private val cyIyPnSonAdapter: IyPnSonAdapter by lazy {
        IyPnSonAdapter() { item, _ ->
            val currentList = mRealVM.levelDataSong.value?.toMutableList() ?: mutableListOf()
            val targetItem = currentList.firstOrNull { it.code == item.code } ?: item
            if (isMultiple) {

                /*val selectedList = mRealVM.selectData.value?.toMutableList() ?: mutableListOf()
                if (targetItem.isSelected) {
                    targetItem.isSelected = false
                    mRealVM.selectData.value = selectedList.filterNot { it.code == targetItem.code }
                } else {
                    if (selectedList.size >= 3) {
                        "最多选择三个".showToast()
                        return@IyPnSonAdapter
                    }
                    targetItem.isSelected = true
                    mRealVM.selectData.value = (selectedList + targetItem).distinctBy { it.code }
                }*/

            } else {
                FlowEventBus.postSticky(Events.CommonEnumItemEvent(item));
                finish()
            }
           // mRealVM.levelDataSong.value = currentList
            cyIyPnSonAdapter.notifyDataSetChanged()
        }
    }

    private val selectCyIyPnAdapter: SelectedIyPnAdapter by lazy {
        SelectedIyPnAdapter { item ->
            val newList = mRealVM.selectData.value?.filterNot { it.code == item.code }
            mRealVM.selectData.value = newList
        }
    }

    private fun initView() {
        if (isMultiple) {
            binding.tvTitle.text = getString(R.string.choose_desired_position)
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
            binding.tvTitle.text = getString(R.string.select_job_title)
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
        binding.et.doAfterTextChanged { editable ->
            val showIcon = !editable.isNullOrEmpty()
            if (showIcon) {
                mRealVM.queryDictIndustryQuery(editable.toString())
                binding.ivClear.visibility = View.VISIBLE
            } else {
                binding.ivClear.visibility = View.GONE
            }
        }
        binding.ivClear.setOnSingleClickListener {
            binding.et.text.clear()
        }
        binding.ivBack.setOnSingleClickListener { finish() }
    }

    private fun observeEvents() {
        mRealVM.selectData.observe(this) {
            binding.tvWordCount.text = it.size.toString()
            selectCyIyPnAdapter.submitList(it)
        }
        mRealVM.levelData.observe(this) {
            Log.d("levelData", it.toJsonString())
            cyIyPnAdapter.submitList(it)
        }
        mRealVM.levelDataSong.observe(this) {
            Log.d("levelDataSong", it.toJsonString())
            cyIyPnSonAdapter.submitList(it)
        }
        mRealVM.lastSelectedPosition.observe(this) {
            mRealVM.levelDataSong.value = CommonEnumTreeUtil.getChildrenByParentCode(mRealVM.allData.value?: emptyList() , mRealVM.allData.value?.get(it)?.code.toString())
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
