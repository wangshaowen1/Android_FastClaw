package com.kgmedia.fastfilm.feature.creator.generate

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fluid.afm.markdown.widget.PrinterMarkDownTextView
import com.fluid.afm.styles.BlockQuoteStyle
import com.fluid.afm.styles.CodeStyle
import com.fluid.afm.styles.MarkdownStyles
import com.fluid.afm.styles.ParagraphStyle
import com.fluid.afm.styles.TableStyle
import com.fluid.afm.styles.TitleStyle
import com.fluid.afm.utils.ParseUtil
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.FragmentDramaGenerateStep1Binding
import com.kgmedia.fastfilm.widget.GeneratingDialog
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener


/**
 * 故事梗概生成
 */
class DramaGenerateStep1Fragment :
    BaseViewModelFragment<FragmentDramaGenerateStep1Binding, DramaGenerateStep1ViewModel>() {

    companion object {
        fun newInstance(): DramaGenerateStep1Fragment {
            val fragment = DramaGenerateStep1Fragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    val hostVm by lazy { getViewModelOfActivity<StoryEditorViewModel>() }
    val markDownTextView by lazy { PrinterMarkDownTextView(mActivity) }
    var dialog: GeneratingDialog? = null

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onAppTrack(AppEvent.JG_VIEW_GENERATE_STORYBOARD_PAGE)
        activity?.intent?.let {
            mRealVM.inspiration = it.getStringExtra(Constants.Extra.CONTENT)
            mRealVM.customEpisodeCount = it.getStringExtra(StoryEditorActivity.CUSTOM_EPISODE_COUNT)
            mRealVM.episodeRangeCode = it.getStringExtra(StoryEditorActivity.EPISODE_RANGE_CODE)
            mRealVM.videoStyleId = it.getStringExtra(Constants.Extra.STYLE_ID)
            mRealVM.videoRatio = it.getStringExtra(Constants.Extra.VIDEO_RATIO)
            mRealVM.sseGenerate()
            hostVm?.backWarning = true
        }
        mRealVM.promptGenerating.observe(this) {
            if (it.first) {
                dialog?.dismiss()
                dialog = activity?.let { it1 -> GeneratingDialog(it1, it.second) }
                dialog?.show()
            } else {
                dialog?.dismiss()
            }
        }

        val linearLayoutManager = LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.setLayoutManager(linearLayoutManager)

        linearLayoutManager.setStackFromEnd(true)
        binding.recyclerView.setAdapter(object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(markDownTextView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            }

            override fun getItemCount(): Int {
                return 1
            }
        })
        markDownTextView.init(getCustomUIStyles(), null)
        markDownTextView.setPrintParams(25, 1)

        mRealVM.mSSEContent.observe(this) {
            if (markDownTextView.isStarted) {
                markDownTextView.appendPrinting(it)
                return@observe
            }
            markDownTextView.startPrinting(it)
        }
        mRealVM.onSSECompleted = {
            hostVm?.backWarning = false
            markDownTextView.stopPrinting("")
        }
        mRealVM.mWorkName.observe(this) {
            if (hostVm?.title?.value != it) {
                hostVm?.title?.value = it
            }
        }
        hostVm?.title?.observe(this) {
            if (it != mRealVM.mWorkName.value) {
                mRealVM.mWorkName.value = it
            }
        }
        binding.btnRegenerate.setOnSingleClickListener {
            mRealVM.sseGenerate()
            hostVm?.backWarning = true
        }
        binding.btnNext.setOnSingleClickListener {
            mRealVM.confirmGenerate { workId, storyId ->
                hostVm?.nextStep(2, workId, storyId)
            }
        }
    }

    fun getCustomUIStyles(): MarkdownStyles {
        val styles = MarkdownStyles()

        // 1. 段落样式：调亮文字颜色（从暗灰改为接近白色），增加行高
        styles.paragraphStyle(
            ParagraphStyle.create()
                .fontSize(ParseUtil.parseDp("30rpx").toFloat())
                .fontColor(-2829100) // 约等于 #D4D4D4，更亮一些
                .lineHeight(ParseUtil.parseDp("52rpx"))
                .paragraphSpacing(ParseUtil.parseDp("12px"))
        )

        // 2. 引用/卡片样式：UI图中每一集看起来像卡片，可以用 BlockQuote 或自定义段落背景模拟
        // 这里将引用的竖线颜色调亮，文字调为亮灰
        styles.blockQuoteStyle(
            BlockQuoteStyle.create()
                .lineColor(-12303292) // 细线颜色 #444444
                .fontColor(-4473925) // 文字颜色 #BBBBBB
                .lineWidth(ParseUtil.parseDp("6rpx"))
                .lineMargin(ParseUtil.parseDp("20rpx"))
        )

        // 3. 表格样式（如果你的目录是用表格实现的）
        styles.tableStyle(
            TableStyle.create()
                .tableBlockRadius(ParseUtil.parseDp("16rpx"))
                .fontColor(-2829100)
                .titleFontColor(-1) // 纯白标题
                .titleBackgroundColor(-14540254) // 深灰背景 #222222
                .bodyBackgroundColor(-13619152) // 稍浅的背景 #303030
                .drawBorder(false)
        ) // UI图中通常不带生硬的边框

        // 4. 标题样式：加大加亮，突出“第1集”这种层级
        styles.setTitleStyle(0, TitleStyle.create(1.5F))
            .setTitleStyle(1, TitleStyle.create(1.3F))
            .setTitleStyle(2, TitleStyle.create(1.1F));

        // 5. 代码块（如果简介中有代码样式，调整背景为深色圆角块）
        styles.codeStyle(
            CodeStyle.create()
                .inlineFontColor(-2171170)
                .inlineCodeBackgroundColor(855638016) // 半透明深色
                .codeFontColor(-2829100)
                .codeFontSize(ParseUtil.parseDp("28rpx"))
                .drawBorder(false)
        )

        return styles
    }

    override fun onDestroy() {
        if (markDownTextView.isStarted) {
            markDownTextView.stopPrinting("")
        }
        super.onDestroy()
    }
}