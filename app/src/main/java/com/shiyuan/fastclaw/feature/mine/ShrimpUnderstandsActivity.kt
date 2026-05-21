package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.item.QuickViewHolder
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.databinding.ActivityShrimpUnderstandsBinding
import com.shiyuan.fastclaw.feature.report.ItemReportVM

/**
 * @Introduce: 小虾理解
 */
class ShrimpUnderstandsActivity :
    BaseViewModelActivity<ActivityShrimpUnderstandsBinding, MineViewModel>() {

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

    }

    private fun initObservers() {

    }


}
