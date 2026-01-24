package com.kgmedia.fastfilm.feature.wallet.withdraw

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.ext.formatDecimal
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.common.ext.showConfirmDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.manager.ShareManager
import com.kgmedia.fastfilm.core.network.apis.entity.WithdrawAccountInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WithdrawInfo
import com.kgmedia.fastfilm.core.network.apis.request.BindWithdrawAccountBody
import com.kgmedia.fastfilm.core.network.apis.request.BindZfbBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.wallet.auth.Alipay
import com.kgmedia.fastfilm.feature.wallet.auth.AuthListener
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.showToast
import com.umeng.socialize.bean.SHARE_MEDIA


/**
 * 收益提现
 */
class WithdrawViewModel : AppRepositoryViewModel<WithdrawRepository>() {

    val toolbar by lazy {
        ToolbarViewModel(
            title = "收益提现",
            titleColor = R.color.color_292929.getResColor(),
            navigationDrawable = R.drawable.ic_nav_back_black.getResDrawable(),
            navigationAction = { back() }
        )
    }

    val loadState by lazy { ObservableInt() }

    /**
     * 提现方式
     */
    val method by lazy { ObservableInt(WithdrawAccountInfo.ACCOUNT_TYPE_ZFB) }
    val summary by lazy { ObservableField<WithdrawInfo>() }
    val accountInfo by lazy { ObservableField<WithdrawAccountInfo>() }
    val withdrawAmount by lazy { ObservableField<String>() }
    val committable by lazy { ObservableBoolean(false) }

    override fun onResume() {
        super.onResume()
        refreshSummary()
    }

    fun refreshSummary() {
        val summary = summary.get()
        if (summary == null) loadState.set(LoadStateViewHolder.LOADING)
        launchWithLoading(
            showLoading = summary != null,
            block = {
                response(
                    bean = mRepo.withdrawInfo(),
                    result = {
                        data?.let {
                            this@WithdrawViewModel.summary.set(it)
                            checkCommittable()
                        }
                        updateLoadState()
                        val data = data
                        if (data != null && accountInfo.get() == null) {
                            val account = data.withdrawalAccountList?.firstOrNull { it.accountType == method.get() } ?: data.withdrawalAccountList?.firstOrNull()
                            account?.let {
                                accountInfo.set(it)
                                it.accountType?.let { type -> method.set(type) }
                                checkCommittable()
                            }
                        }
                    },
                    error = { updateLoadState() }
                )
            },
            onError = {
                updateLoadState()
            })
    }

    private fun updateLoadState() {
        if (summary.get() == null) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    /**
     * 设置提现方式
     */
    fun setWithdrawMethod(method: Int) {
        if (method != this.method.get()) {
            this.method.set(method)
            summary.get()?.withdrawalAccountList?.firstOrNull { it.accountType == method }.let {
                accountInfo.set(it)
                checkCommittable()
            }
        }
    }

    /**
     * 《收益提现协议》
     */
    fun viewPolicy() {
        Config.get().appConfig.shortDramasIntegralExchangeProfitProtocol?.let { SimpleBrowserActivity.launch(topActivity, it) }
    }

    fun bindAccount() {
        if (accountInfo.get() == null) {
            when (method.get()) {
                WithdrawAccountInfo.ACCOUNT_TYPE_WX -> {
                    wxAuth()
                }

                WithdrawAccountInfo.ACCOUNT_TYPE_ZFB -> {
                    zfbAuth()
                }
            }
        }
    }

    private fun wxAuth() {
        val activity = topActivity ?: return
        ShareManager(activity)
            .login(SHARE_MEDIA.WEIXIN, object : ShareManager.UmengLoginListener {
                override fun onStart(platform: SHARE_MEDIA?) {
                }

                override fun onAuthed(platform: SHARE_MEDIA?, data: MutableMap<String, String>?) {
                    wxBind(data)
                }

                override fun onError(e: Exception?, platform: SHARE_MEDIA?, onCancel: Boolean) {
                    bindFailed()
                }
            })
    }

    private fun wxBind(data: Map<String, String>?) {
        launchWithLoading {
            val body = BindWithdrawAccountBody(
                accountType = WithdrawAccountInfo.ACCOUNT_TYPE_WX,
                accountId = data?.get("openid"),
                accountAvatar = data?.get("iconurl"),
                accountName = data?.get("name")
            )
            response(mRepo.bindWithdrawAccount(body)) { refreshSummary() }
        }
    }

    private fun zfbAuth() {
        Alipay.auth(summary.get()?.aliPayStr, object : AuthListener {
            override fun onFailed(reason: String) {
                reason.showToast()
            }

            override fun onSuccess(authCode: String) {
                zfbBind(authCode)
            }
        })
    }

    private fun zfbBind(authCode: String) {
        launchWithLoading { response(mRepo.bindZfb(BindZfbBody(authCode))) { refreshSummary() } }
    }

    private fun bindFailed() {
        "绑定失败".showToast()
    }

    fun onInput(input: String?) {
        withdrawAmount.set(input)
        checkCommittable()
    }

    /**
     * 检查是否可以提交
     */
    private fun checkCommittable() {
        val amount = withdrawAmount.get()?.toFloatOrNull() ?: 0f
        if (amount <= 0) {
            committable.set(false)
            return
        }
        val maxBalance = summary.get()?.balance ?: 0f
        if (amount > maxBalance) {
            committable.set(false)
            return
        }
        val min = summary.get()?.minWithdrawalAmount ?: 0f
        val max = summary.get()?.maxWithdrawalAmount ?: 0f
        if (amount < min || amount > max) {
            committable.set(false)
            return
        }
        val account = accountInfo.get()
        if (account == null) {
            committable.set(false)
            return
        }
        committable.set(true)
    }

    /**
     * 确定提现
     */
    fun confirm() {
        val amount = withdrawAmount.get()?.toFloatOrNull() ?: 0f
        if (amount <= 0) {
            "请输入提现金额".showToast()
            return
        }
        val maxBalance = summary.get()?.balance ?: 0f
        if (amount > maxBalance) {
            "请输入${maxBalance.formatMoney()}元一下".showToast()
            return
        }
        val min = summary.get()?.minWithdrawalAmount ?: 0f
        val max = summary.get()?.maxWithdrawalAmount ?: 0f
        if (amount < min){
            "单笔提现金额最低${min.formatMoney()}元".showToast()
            return
        }
        if (amount > max){
            "单笔提现金额最高${max.formatMoney()}元".showToast()
            return
        }
        val account = accountInfo.get()
        if (account == null) {
            "请绑定提现账号".showToast()
            return
        }
        WithdrawConfirmDialog.show(amount.formatDecimal(), null, account) {
            withdraw(amount, account.accountType ?: WithdrawAccountInfo.ACCOUNT_TYPE_ZFB)
        }
    }

    private fun withdraw(balance: Float, accountType: Int) {
        launchWithLoading {
            response(mRepo.withdraw(balance, accountType)) {
                refreshSummary()
                "提现申请已提交，到账后将通过系统消息通知您。".showToast()
                //topActivity?.showConfirmDialog(
                //    title = "已发起提现申请",
                //    tips = "提现金额1-7个工作日内到帐",
                //    positiveButtonText = "我知道了"
                //)
            }
        }
    }
}