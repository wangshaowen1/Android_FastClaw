package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.QuickViewHolder
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.databinding.ActivityShrimpUnderstandsBinding
import com.shiyuan.fastclaw.feature.mine.viewModel.UnderstandsViewModel
import com.shiyuan.fastclaw.feature.report.ItemReportVM

/**
 * @Introduce: 小虾理解
 */
class ShrimpUnderstandsActivity :
    BaseViewModelActivity<ActivityShrimpUnderstandsBinding, UnderstandsViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, ShrimpUnderstandsActivity::class.java)
            activity.startActivity(intent)
        }
    }

    private val mAdapter by lazy {
        object : QuickAdapter<ItemReportVM>(R.layout.item_report, emptyList()) {
            override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: ItemReportVM?) {
                //val binding = DataBindingUtil.bind<ItemReportBinding>(holder.itemView)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
        initObservers()
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
            layoutManager = LinearLayoutManager(this@ShrimpUnderstandsActivity, RecyclerView.VERTICAL, false)
            adapter = mAdapter
        }
        binding.tvTable1.setOnSingleClickListener {  mRealVM.tableIndex.postValue(0) }
        binding.tvTable2.setOnSingleClickListener { mRealVM.tableIndex.postValue(1) }
    }

    private fun initObservers() {
        mRealVM.tableIndex.observe(this){
            if(it == 0){
                binding.tvTable1.setBackgroundResource(R.drawable.gradient_top_11c4cb_bottom_efcdaf)
                binding.tvTable1.setTextColor(resources.getColor(R.color.white))
                binding.ivTable1.visibility = View.VISIBLE

                binding.tvTable2.background = null
                binding.tvTable2.setTextColor(resources.getColor(R.color.color_666666))
                binding.ivTable2.visibility = View.GONE
            }else{
                binding.tvTable1.background = null
                binding.tvTable1.setTextColor(resources.getColor(R.color.color_666666))
                binding.ivTable1.visibility = View.GONE

                binding.tvTable2.setBackgroundResource(R.drawable.gradient_top_11c4cb_bottom_efcdaf)
                binding.tvTable2.setTextColor(resources.getColor(R.color.white))
                binding.ivTable2.visibility = View.VISIBLE
            }
        }
    }


}
