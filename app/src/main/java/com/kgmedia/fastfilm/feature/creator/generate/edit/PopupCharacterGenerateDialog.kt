package com.kgmedia.fastfilm.feature.creator.generate.edit

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.SizeUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.core.network.apis.entity.RoleTagInfo
import com.kgmedia.fastfilm.databinding.PopupCharacterGenerateBinding
import com.zhpan.bannerview.BaseViewHolder

class PopupCharacterGenerateDialog(context: Context?) : PopupWindow(context) {
    var onPopViewClickedListener: ((RoleTagInfo?) -> Unit)? = null
    private val binding: PopupCharacterGenerateBinding = PopupCharacterGenerateBinding.inflate(LayoutInflater.from(context))
    var list = listOf<RoleTagInfo>()
    private val adapter by lazy {
        object : BaseQuickAdapter<RoleTagInfo, BaseViewHolder<RoleTagInfo>>(list) {
            override fun onBindViewHolder(holder: BaseViewHolder<RoleTagInfo>, position: Int, item: RoleTagInfo?) {
                holder.setText(R.id.tvName, item?.roleTagName)
                holder.setBackgroundResource(
                    R.id.tvName,
                    when (position % 2) {
                        0 -> R.color.color_f5f5f5  // 偶数位置显示 f5f5f5
                        else -> R.color.white      // 奇数位置显示白色
                    }
                )
                holder.setOnClickListener(R.id.tvName) {
                    onPopViewClickedListener?.invoke(item)
                    dismiss()
                }
            }

            override fun onCreateViewHolder(
                context: Context,
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<RoleTagInfo> {
                return BaseViewHolder<RoleTagInfo>(LayoutInflater.from(context).inflate(R.layout.item_character_generate, parent, false))
            }
        }
    }

    init {
        binding.recyclerView.setLayoutManager(LinearLayoutManager(context))
        binding.recyclerView.adapter = adapter
        setContentView(binding.getRoot())
        animationStyle = R.style.NoDimAnimBottomGrow
    }


    fun show(list: List<RoleTagInfo>, anchor: View) {
        this.list = list
        adapter.submitList(list)

        // 1. 获取锚点位置（相对屏幕）
        val location = IntArray(2)
        anchor.getLocationInWindow(location)
        val anchorX = location[0]
        val anchorY = location[1]

        // 2. 测量 Popup 宽高（确保高度准确，避免测量为 0）
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = contentView.measuredWidth.takeIf { it > 0 } ?: measureWidth
        val popupHeight = contentView.measuredHeight.takeIf { it > 0 } ?: measureHeight

        // 3. 计算 X 坐标，确保弹窗水平居中
        val x = (anchorX + anchor.width / 2f - popupWidth / 2f).toInt()

        // 4. 计算 Y 坐标，确保弹窗贴在目标 View 上方
        var y = anchorY - popupHeight

        if (y < 0) {
            y = anchorY + anchor.height // 若空间不足，显示在下方
        }

        // 6. 配置 PopupWindow 属性
        isOutsideTouchable = true
        isFocusable = true
        setBackgroundDrawable(BitmapDrawable())

        // 7. 显示 PopupWindow
        super.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
    }



    private val measureHeight: Int
        get() = SizeUtils.dp2px(122f)

    private val measureWidth: Int
        get() = SizeUtils.dp2px(86f)

    private fun offsetY(): Int {
        return 0
    }
}
