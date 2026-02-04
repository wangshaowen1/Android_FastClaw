package com.kgmedia.fastfilm.feature.works

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.db.entity.WorkInformationInfo
import com.kgmedia.fastfilm.databinding.FragmentMyWorksBinding
import com.kgmedia.fastfilm.feature.main.MainViewModel
import com.kgmedia.fastfilm.feature.series.SeriesActivity
import com.kgmedia.fastfilm.widget.decoration.BottomSpaceDecoration
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfActivity
import kotlinx.coroutines.launch

class MyWorksFragment : BaseViewModelFragment<FragmentMyWorksBinding, MyWorksViewModel>() {


    private val myWorksAdapter by lazy {
        MyWorksAdapter(eventVm = eventVm)
    }
    private val eventVm by lazy {
        ItemMultipleChoiceEventsViewModel<MyWorksEvent, String>()
    }


    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }


    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(12.dp))
            addItemDecoration(BottomSpaceDecoration(80.dp))
            itemAnimator = null
            val myMultiStatePagingFooterAdapter = MyMultiStatePagingFooterAdapter(
                myWorksAdapter,
                noData = "暂无作品记录，快去创作你的作品吧",
            ) {
                myWorksAdapter.retry()
            }
            adapter = myWorksAdapter.withLoadStateFooter(myMultiStatePagingFooterAdapter)
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = myWorksAdapter,
            loadStateHolder = binding.lsv,
        )
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.getWorkInformation().collect {
                myWorksAdapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is MyWorksEvent.ViewDetails -> {
                        //查看详情
                        SeriesActivity.start(workId = event.workInfo?.workId)
                    }

                    is MyWorksEvent.ComposeState -> {
                        //编辑下选择
                        mRealVM.selectedCount.value = eventVm.getSelectedItems().size
                    }
                }
            }
        }
        getViewModelOfActivity<MainViewModel>()?.setIsEditing = { status ->
            mRealVM.isView.value = true
        }
        mRealVM.isView.observe(this) {
            eventVm.switchStatus(it)
            getViewModelOfActivity<MainViewModel>()?.isEditing?.set(!it)
            if (it) {
                //是否启用下拉刷新
                binding.srl.setEnableRefresh(true)
                binding.groupView.visibility = View.VISIBLE
                binding.groudEdit.visibility = View.GONE
                eventVm.clearAllSelection()
                getViewModelOfActivity<MainViewModel>()?.showBottomBar?.set(true)
                return@observe
            }
            binding.srl.setEnableRefresh(false)
            getViewModelOfActivity<MainViewModel>()?.showBottomBar?.set(false)
            binding.groupView.visibility = View.GONE
            binding.groudEdit.visibility = View.VISIBLE
        }

        mRealVM.selectedCount.observe(this) {
            if (it <= 0) {
                binding.tvSelectCount.text = "已选择 0 个"
                return@observe
            }
            val spannableString = SpannableString("已选择${it}个")
            spannableString.setSpan(
                ForegroundColorSpan(
                    Color.WHITE
                ), 3, spannableString.length - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.tvSelectCount.text = spannableString
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnEdit -> {
                //编辑
                mRealVM.isView.value = false
            }

            R.id.btnSelectAll -> {
                //全选
                val ids = hashSetOf<String>()
                val items: List<WorkInformationInfo> = myWorksAdapter.snapshot().items
                items.forEach { w -> ids.add(w.work.workId ?: "") }
                eventVm.setSelectedItems(ids)
                mRealVM.selectedCount.value = ids.size
            }

            R.id.btnDelete -> {
                //删除
                if (eventVm.getSelectedItems().isEmpty()) {
                    mRealVM.isView.value = true
                    return
                }
                topActivity?.showConfirmPrompt(
                    title = "是否删除该作品",
                    cancelText = "取消",
                    confirmText = "确定"
                ) {
                    mRealVM.delete(eventVm.getSelectedItems().toList())
                }
            }

            R.id.btnComplete -> {
                //完成
                mRealVM.isView.value = true
            }
        }
    }

}