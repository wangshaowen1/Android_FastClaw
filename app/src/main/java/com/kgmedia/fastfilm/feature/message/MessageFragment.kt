package com.kgmedia.fastfilm.feature.message

import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.network.apis.entity.BannerInfo
import com.kgmedia.fastfilm.databinding.FragmentMessageBinding
import com.kgmedia.fastfilm.feature.main.MainViewModel
import com.kgmedia.fastfilm.router.AppRouter
import com.kgmedia.fastfilm.widget.CropShapeImageView
import com.kuaige.imageloader.GlideImageLoader
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getParentViewModel
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.setOnSingleClickListener
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhpan.indicator.enums.IndicatorStyle

/**
 * 首页-消息
 */
class MessageFragment : BaseViewModelFragment<FragmentMessageBinding, MessageViewModel>() {

    private lateinit var bannerAdapter: BaseBannerAdapter<BannerInfo>

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        dataUpdate()
    }

    private fun initView() {
        bannerAdapter = object : BaseBannerAdapter<BannerInfo>() {
            override fun bindData(
                holder: BaseViewHolder<BannerInfo>?, data: BannerInfo, position: Int, pageSize: Int
            ) {
                holder?.apply {
                    val image = findViewById<CropShapeImageView>(R.id.image)
                    GlideImageLoader.with(image).load(data.jumpCode)
                        .into(image)
                    image.setOnSingleClickListener {
                        AppRouter.jump(data.jumpCode)
                    }
                }
            }

            override fun getLayoutId(viewType: Int): Int {
                return R.layout.item_message_banner
            }
        }
        binding.bvp.setAdapter(bannerAdapter)
            .setIndicatorSliderRadius(2f.dp)
            .setIndicatorSliderGap(4.dp)
            .setIndicatorHeight(4.dp)
            .setIndicatorSliderWidth(4.dp, 8.dp)
            .setIndicatorSliderColor(
                R.color.color_50_ffffff.getResColor(), R.color.white.getResColor()
            ).setIndicatorStyle(IndicatorStyle.ROUND_RECT)
            .setIndicatorMargin(0, 0, 0, 4.dp)
            .setIndicatorView(binding.ivIndicator)
            .setIndicatorVisibility(View.VISIBLE)
            .create()
    }

    private fun dataUpdate() {
        observeEvents {
            on<Events.MainTabDoubleClickEvent> { event ->
                if (event.tabType == Events.MainTabDoubleClickEvent.TAB_MESSAGE) {
                    if (isActive) {
                        mRealVM.refreshMessages()
                    }
                }
            }
        }
        mRealVM.bannerList.observe(this) {
            binding.bvp.create(it)
        }
    }
}