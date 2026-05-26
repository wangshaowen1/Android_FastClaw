package com.shiyuan.fastclaw.feature.mine

import FieldItems
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.common.ext.toJsonString
import com.shiyuan.fastclaw.databinding.ActivityShrimpUnderstandsBinding
import com.shiyuan.fastclaw.databinding.ItemJobPreference1Binding
import com.shiyuan.fastclaw.databinding.ItemJobPreference1SonBinding
import com.shiyuan.fastclaw.databinding.ItemJobPreference2Binding
import com.shiyuan.fastclaw.databinding.ItemJobPreference2SonBinding
import com.shiyuan.fastclaw.databinding.ItemJobPreference3SonBinding
import com.shiyuan.fastclaw.feature.mine.viewModel.UnderstandsViewModel
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration

/**
 * @Introduce: 小虾理解
 */

// 在 ShrimpUnderstandsActivity 类外或内部定义（但需在调用前）
fun FieldItems.maxDepth(): Int = 1 + (fieldItems.maxOfOrNull { it.maxDepth() } ?: 0)
class ShrimpUnderstandsActivity :
    BaseViewModelActivity<ActivityShrimpUnderstandsBinding, UnderstandsViewModel>() {

    companion object {
        private const val EXTRA_PROGRESS = "extra_progress"
        fun start(activity: Activity?, progress: Int = 0) {
            activity ?: return
            val intent = Intent(activity, ShrimpUnderstandsActivity::class.java).apply {
                putExtra(EXTRA_PROGRESS, progress)
            }
            activity.startActivity(intent)
        }
    }

    private val progress: Int by lazy { intent.getIntExtra(EXTRA_PROGRESS, 0) }
    private val mAdapter2 by lazy {
        SingleBindingAdapter<ItemJobPreference1Binding, FieldItems>(
            R.layout.item_job_preference1,
            emptyList(),
            onBind = { binding, position, item ->
                binding.tvTile.text = item.fieldName ?: ""
                when (item.fieldType) {
                    "JOB_EXPECTATION" -> {
                        binding.tvConnect.visibility = View.GONE
                        binding.rcConnect.visibility = View.VISIBLE
                        val subAdapter =
                            SingleBindingAdapter<ItemJobPreference1SonBinding, FieldItems>(
                                R.layout.item_job_preference1_son,
                                item.fieldItems,
                                onBind = { b, _, sub -> b.tvTag.text = sub.fieldValue ?: "" })
                        binding.rcConnect.apply {
                            val layoutManager =
                                FlexboxLayoutManager(this@ShrimpUnderstandsActivity).apply {
                                    flexDirection = FlexDirection.ROW           // 水平方向排列
                                    flexWrap = FlexWrap.WRAP                    // 自动换行
                                    justifyContent =
                                        JustifyContent.FLEX_START  // 起始对齐（可根据需要改为 CENTER / SPACE_BETWEEN）
                                    alignItems =
                                        AlignItems.STRETCH             // 拉伸以保持行高一致（或改为 FLEX_START）
                                }
                            addItemDecoration(
                                SpaceDecoration(
                                    10.dp,
                                    SpaceDecoration.Direction.MIDDLE_HORIZONTAL
                                )
                            )
                            this.layoutManager = layoutManager
                            adapter = subAdapter
                        }
                    }

                    else -> {
                        binding.rcConnect.visibility = View.GONE
                        binding.tvConnect.visibility = View.VISIBLE
                        binding.tvConnect.text = item.fieldValue ?: ""
                    }
                }
            }
        )
    }

    private val mAdapter1 by lazy {
        SingleBindingAdapter<ItemJobPreference2Binding, FieldItems>(
            R.layout.item_job_preference2,
            emptyList(),
            onBind = { binding, position, item ->
                binding.tvTile.text = item.fieldName ?: ""
                when (item.fieldType) {
                    "EDUCATION_INFO", "PROJECT_EXPERIENCE" ,"WORK_EXPERIENCE","CERTIFICATE_INFO"-> {
                        binding.tvConnect.visibility = View.GONE
                        binding.rcConnect.visibility = View.VISIBLE
                        val subAdapter = SingleBindingAdapter<ItemJobPreference2SonBinding, FieldItems>(
                            R.layout.item_job_preference2_son,
                            item.fieldItems,
                            onBind = { binding, _, item ->
                                binding.tvTile.text = item.fieldValue ?: ""
                                val subAdapter = SingleBindingAdapter<ItemJobPreference3SonBinding, FieldItems>(
                                    R.layout.item_job_preference3_son,
                                    item.fieldItems,
                                    onBind = { binding, _, item ->
                                        binding.tvTile.text = item.fieldName ?: ""
                                        binding.tvConnect.text = item.fieldValue ?: ""
                                    })
                                binding.rcConnect.apply {
                                    layoutManager = LinearLayoutManager(this@ShrimpUnderstandsActivity, RecyclerView.VERTICAL, false)
                                    addItemDecoration(SpaceDecoration(16.dp, SpaceDecoration.Direction.MIDDLE))
                                    adapter = subAdapter
                                }
                            })
                        binding.rcConnect.apply {
                            layoutManager = LinearLayoutManager(this@ShrimpUnderstandsActivity, RecyclerView.VERTICAL, false)
                            addItemDecoration(SpaceDecoration(16.dp, SpaceDecoration.Direction.MIDDLE))
                            adapter = subAdapter
                        }
                    }
                    else -> {
                        binding.rcConnect.visibility = View.GONE
                        binding.tvConnect.visibility = View.VISIBLE
                        binding.tvConnect.text =item.fieldItems.firstOrNull()?.fieldValue ?: item.fieldValue ?: ""

                    }
                }
            }
        )
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObservers()
        mRealVM.understandingDetail()
        // mRealVM.setMockData()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        binding.rvUnderstands1.apply {
            layoutManager =
                LinearLayoutManager(this@ShrimpUnderstandsActivity, RecyclerView.VERTICAL, false)
            addItemDecoration(SpaceDecoration(20.dp, SpaceDecoration.Direction.MIDDLE))
            binding.rvUnderstands1.adapter = mAdapter1
        }

        binding.rvUnderstands2.apply {
            layoutManager =
                LinearLayoutManager(this@ShrimpUnderstandsActivity, RecyclerView.VERTICAL, false)
            addItemDecoration(SpaceDecoration(16.dp, SpaceDecoration.Direction.MIDDLE))
            binding.rvUnderstands2.adapter = mAdapter2
        }
        binding.tvTable1.setOnSingleClickListener { mRealVM.tableIndex.postValue(0) }
        binding.tvTable2.setOnSingleClickListener { mRealVM.tableIndex.postValue(1) }
    }

    fun setConnectVisibility(){
        if(mRealVM.tableIndex.value == 0){
            binding.flUnderstands2.visibility = View.GONE
            if( mRealVM.resumeInfo.value.isNullOrEmpty()){
                binding.viewUnderstands.root.visibility = View.VISIBLE
                binding.flUnderstands1.visibility = View.GONE
            }else{
                binding.viewUnderstands.root.visibility = View.GONE
                binding.flUnderstands1.visibility = View.VISIBLE
            }
            mAdapter1.submitList(mRealVM.resumeInfo.value)
        }else{
            binding.flUnderstands1.visibility = View.GONE
            if( mRealVM.jobPreference.value.isNullOrEmpty()){
                binding.viewUnderstands.root.visibility = View.VISIBLE
                binding.flUnderstands2.visibility = View.GONE
            }else{
                binding.viewUnderstands.root.visibility = View.GONE
                binding.flUnderstands2.visibility = View.VISIBLE
            }
            mAdapter2.submitList(mRealVM.resumeInfo.value)
        }
    }
    private fun initObservers() {
        mRealVM.tableIndex.observe(this) {
            if (it == 0) {
                binding.tvTable1.setBackgroundResource(R.drawable.gradient_top_11c4cb_bottom_efcdaf)
                binding.tvTable1.setTextColor(resources.getColor(R.color.white))
                binding.ivTable1.visibility = View.VISIBLE

                binding.tvTable2.background = null
                binding.tvTable2.setTextColor(resources.getColor(R.color.color_666666))
                binding.ivTable2.visibility = View.GONE

                setConnectVisibility()
            } else {
                binding.tvTable1.background = null
                binding.tvTable1.setTextColor(resources.getColor(R.color.color_666666))
                binding.ivTable1.visibility = View.GONE

                binding.tvTable2.setBackgroundResource(R.drawable.gradient_top_11c4cb_bottom_efcdaf)
                binding.tvTable2.setTextColor(resources.getColor(R.color.white))
                binding.ivTable2.visibility = View.VISIBLE

                setConnectVisibility()
            }
        }


        mRealVM.resumeInfo.observe(this) { data ->
            setConnectVisibility()
        }
        mRealVM.jobPreference.observe(this) { data ->
            setConnectVisibility()
        }
        mRealVM.titleInfo.observe(this) { data ->
            data ?: return@observe
            binding.tvName.text = data.getOrNull(0)?.fieldValue ?: "暂无"
            binding.tvTip.text = data.getOrNull(1)?.fieldValue ?: "暂无"
            binding.waveView.progress =progress
            binding.waveView.text ="${progress}%"
        }
    }

}
