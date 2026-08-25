package com.shiyuan.fastclaw.feature.opportunity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.BR
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ActivityCompanyDetailBinding
import com.shiyuan.fastclaw.feature.opportunity.vm.CompanyDetailViewModel

/** 公司主页，展示机会详情接口关联企业的公开信息和福利标签。 */
class CompanyDetailActivity :
    BaseViewModelActivity<ActivityCompanyDetailBinding, CompanyDetailViewModel>() {

    companion object {
        private const val EXTRA_ENTERPRISE_ID = "extra_enterprise_id"

        /**
         * 打开公司主页。
         *
         * @param enterpriseId 企业业务 ID；为空、非数字或非正数时不跳转。
         */
        fun start(activity: Activity?, enterpriseId: String) {
            val currentEnterpriseId = enterpriseId.toLongOrNull() ?: return
            if (activity == null || currentEnterpriseId <= 0) return
            activity.startActivity(
                Intent(activity, CompanyDetailActivity::class.java)
                    .putExtra(EXTRA_ENTERPRISE_ID, currentEnterpriseId)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.toolbar.setVariable(BR.toolbar, mRealVM.toolbar)
        val enterpriseId = intent.getLongExtra(EXTRA_ENTERPRISE_ID, 0L)
        if (enterpriseId <= 0) {
            finish()
            return
        }
        mRealVM.companyDetail.observe(this) { detail ->
            // OpportunityCompanyDetailData 的文本字段由 DataBinding 展示，标签在此动态创建。
            renderWelfareTags(detail.welfareTags)
        }
        mRealVM.queryCompanyDetail(enterpriseId)
    }

    /** 将接口返回的非空福利标签渲染到流式布局。 */
    private fun renderWelfareTags(tags: List<String>) {
        binding.flexWelfare.removeAllViews()
        tags.filter(String::isNotBlank).forEach { tag ->
            binding.flexWelfare.addView(
                TextView(this).apply {
                    text = tag
                    setTextAppearance(R.style.Text12)
                    setTextColor(getColor(R.color.color_ffffff))
                    setBackgroundResource(R.drawable.bg_15_ffffff_r7)
                    setPadding(9.dp, 4.dp, 9.dp, 4.dp)
                    layoutParams = FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        marginEnd = 8.dp
                        bottomMargin = 8.dp
                    }
                }
            )
        }
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(false)
            .navigationBarColor(R.color.company_detail_background)
            .navigationBarDarkIcon(false)
            .transparentStatusBar()
            .init()
    }
}
