package com.shiyuan.fastclaw.feature.report.report

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.shiyuan.fastclaw.R
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder

/**
 * @Introduce: 轮播图适配器（支持本地图片资源）
 */
class ImageBannerAdapter : BaseBannerAdapter<Any>() {

    override fun getLayoutId(viewType: Int): Int {
        return R.layout.item_banner_image
    }

    override fun createViewHolder(parent: ViewGroup, itemView: View?, viewType: Int): BaseViewHolder<Any> {
        return ImageBannerViewHolder(itemView!!)
    }

    override fun bindData(holder: BaseViewHolder<Any>?, data: Any, position: Int, pageSize: Int) {
        (holder as? ImageBannerViewHolder)?.bind(data as Int)
    }
}

class ImageBannerViewHolder(itemView: View) : BaseViewHolder<Any>(itemView) {

    private val imageView: ImageView = itemView.findViewById(R.id.image)

    fun bind(imageRes: Int) {
        imageView.setImageResource(imageRes)
    }
}
