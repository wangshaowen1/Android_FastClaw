package com.shiyuan.fastclaw.feature.opportunity

import android.app.Activity
import android.content.Intent
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ActivityOpportunityDetailBinding
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityViewModel
import io.noties.markwon.Markwon

/**
 * 机会详情。
 */
class OpportunityDetailActivity :
    BaseViewModelActivity<ActivityOpportunityDetailBinding, OpportunityViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            activity.startActivity(Intent(activity, OpportunityDetailActivity::class.java))
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

@BindingAdapter("opportunityMarkdown")
fun setOpportunityMarkdown(view: TextView, markdown: String?) {
    Markwon.create(view.context).setMarkdown(view, markdown.orEmpty())
}
