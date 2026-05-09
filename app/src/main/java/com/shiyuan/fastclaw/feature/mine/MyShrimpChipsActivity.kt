package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.Observable
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.LoadState
import com.shiyuan.fastclaw.databinding.ActivityMyHistoryReportBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.databinding.ActivityMyShrimpChipsBinding
import com.shiyuan.fastclaw.feature.payment.ItemPaymentMethodVm
import com.shiyuan.fastclaw.feature.report.ReportViewModel

/**
 * @Introduce: 我的虾条
 */
class MyShrimpChipsActivity : BaseViewModelActivity<ActivityMyShrimpChipsBinding, MineViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, MyShrimpChipsActivity::class.java)
            activity.startActivity(intent)
        }
    }


    private val adapter by lazy {
        QuickAdapter<ItemPaymentMethodVm>(R.layout.item_shrimp_chips, emptyList())
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
        binding.recyclerView.apply {
            layoutManager = ReportViewModel.buildLayoutManager(context)
            adapter = adapter
        }

    }

    private fun initObservers() {

    }
}
