package com.shiyuan.fastclaw.feature.invite

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_INVITE_NOW
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_INVITE_RULES
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_SHARE
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.databinding.ActivityInviteFriendsBinding
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity

/**
 * @Introduce: 邀请好友
 */
class InviteFriendsActivity :
    BaseViewModelActivity<ActivityInviteFriendsBinding, InviteFriendsViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            activity.startActivity(Intent(activity, InviteFriendsActivity::class.java))
        }
    }

    private val recordAdapter by lazy {
        QuickAdapter<ItemInviteRecordVM>(R.layout.view_invite_record_item, emptyList())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        mRealVM.mToolbarVM.menuAction = {
            EventManager.onAppTrack(KLX_CLICK_SHARE )
            showInviteShareDialog()
        }
        binding.rvInviteRecords.apply {
            layoutManager = LinearLayoutManager(this@InviteFriendsActivity)
            isNestedScrollingEnabled = false
            adapter = recordAdapter
        }
        mRealVM.inviteRecordItems.observe(this) { items ->
            recordAdapter.submitList(items)
        }
        mRealVM.loadInviteStats()
        mRealVM.loadInviteRecords()
        binding.tvInviteNow.setOnSingleClickListener {
            EventManager.onAppTrack(KLX_CLICK_INVITE_NOW)
            showInviteShareDialog()
        }
        binding.tvInviteRule.setOnSingleClickListener {
            EventManager.onAppTrack(KLX_CLICK_INVITE_RULES)
            SimpleBrowserActivity.launch(this, Constants.INVITEFRIEND_RULES_URL)
        }
        setupToolbarBackgroundAnimation()
    }

    private fun showInviteShareDialog() {
        val inviteCode = User.get().inviteCode
        if (inviteCode != null) {
            InviteShareDialog.show(supportFragmentManager, inviteCode)
            return
        }
        mRealVM.queryInviteShareInfo { code ->
            InviteShareDialog.show(supportFragmentManager, code)
        }
    }

    private fun setupToolbarBackgroundAnimation() {
      /*  binding.appbarLayout.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                val totalRange = appBarLayout.totalScrollRange.takeIf { it > 0 }
                    ?: return@OnOffsetChangedListener
                val progress = (-verticalOffset / totalRange.toFloat()).coerceIn(0f, 1f)
                val alpha = (progress * 255).toInt().coerceIn(0, 255)
               // window.statusBarColor = Color.argb(alpha, 255, 255, 255)
              //  mRealVM.mToolbarVM.mBackground.set(Color.argb(alpha, 255, 255, 255))
            }
        )*/
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }
}
