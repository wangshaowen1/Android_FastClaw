package com.kgmedia.fastfilm.feature.creator

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.StringUtils.getString
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.RecyclerViewVM
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.parseMessage
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.CreationStyleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.InspirationInfo
import com.kgmedia.fastfilm.core.network.apis.request.CreationGeneratePreBody
import com.kgmedia.fastfilm.core.network.apis.request.OptimizeContentBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.CreationFragment.Companion.KEY_MODEL
import com.kgmedia.fastfilm.feature.creator.CreationFragment.Companion.MODEL_INSPIRATION
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kgmedia.fastfilm.feature.creator.style.ItemCreationStyleVm
import com.kgmedia.fastfilm.feature.payment.vip.MembershipBenefitsActivity
import com.kgmedia.fastfilm.router.AppNavigator
import com.kgmedia.fastfilm.widget.decoration.GridSpacingItemDecoration
import com.kgmedia.fastfilm.widget.decoration.LinearLayoutManagerSpacingDecoration
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.showToast

class CreationViewModel : AppRepositoryViewModel<CreationRepository>() {
    val input by lazy { MutableLiveData<String?>() }
    val textLimitNum by lazy { Config.get().appConfig.shortDramas }
    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(1000)
    }
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    val isVip = ObservableBoolean(User.get().isVip)
    val hint = ObservableField<String>()
    val creationModel by lazy {
        mBundle.getInt(KEY_MODEL)
    }

    val inspirationPrompts by lazy { MutableLiveData<List<InspirationInfo>>() }
    var checkedRatio: ItemVideoRatioVm? = null
    val styleVms = arrayListOf<ItemCreationStyleVm>()
    val mAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_creation_style, styleVms)
            .setItemDecoration(
                LinearLayoutManagerSpacingDecoration(
                    10,
                    16,
                    LinearLayoutManagerSpacingDecoration.Orientation.Horizontal
                )
            ).build()

    val ratioVms = arrayListOf(
        initRatioVm(CreationGeneratePreBody.RATIO_9_16),
        initRatioVm(CreationGeneratePreBody.RATIO_16_9),
        initRatioVm(CreationGeneratePreBody.RATIO_3_4),
        initRatioVm(CreationGeneratePreBody.RATIO_4_3)
    )

    fun initRatioVm(ratio: Int): ItemVideoRatioVm {
        return ItemVideoRatioVm(ratio).apply {
            if (ratio == CreationGeneratePreBody.RATIO_9_16) {
                checked.set(true)
                checkedRatio = this
            }
            onItemClick.value = {
                if (checkedRatio != this) {
                    checkedRatio?.checked?.set(false)
                    this.checked.set(true)
                    checkedRatio = this
                }
            }
        }
    }

    val mRatioAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_video_ratio, ratioVms)
            .setItemDecoration(
                GridSpacingItemDecoration(
                    4,
                    8.dp,
                    0
                )
            ).build()


    fun inspirationModel(): Boolean {
        return creationModel == MODEL_INSPIRATION
    }

    override fun onCreate() {
        super.onCreate()
        input.value = if (inspirationModel()) Global.inspirationContent else Global.customizedContent
        creationStyles()
        if (inspirationModel()) {
            randomInspirations()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshConfig()
    }

    fun refreshConfig() {
        isVip.set(User.get().isVip)
        val maxLength =
            if (inspirationModel()) textLimitNum?.shortDramasInspirationWriteStoryLimitNum else (if (isVip.get()) textLimitNum?.shortDramasWriteStoryLimitNumForVip else textLimitNum?.shortDramasWriteStoryLimitNum)
        maxInputLength.set(maxLength ?: (if (isVip.get()) 5000 else 1000))
        hint.set(
            if (inspirationModel()) textLimitNum?.shortDramasInspirationStoryRemindText
                ?: getString(R.string.hint_inspiration_input) else textLimitNum?.shortDramasWriteStoryRemindText
                ?: "请输入你想写的故事。例如：穿成恶毒女配，我决定躺平摆烂，每天在心里直播吐槽。没想到那个阴鸷狠戾的反派大佬，居然能听见我的心声。当我骂他“疯批”时，他却低声轻笑：“骂得好，再骂几句。”"
        )
    }

    fun optimizePrompt() {
        promptGenerating.value = true to "正在优化中，请稍候..."
        launch {
            val promptResponse = mRepo.optimizeContent(OptimizeContentBody(input.value, if (inspirationModel()) 1 else 2))
            promptGenerating.value = false to ""
            if (promptResponse.isSuccess) {
                input.value = promptResponse.data?.content
            } else {
                promptResponse.msg?.showToast()
            }
        }
    }

    fun randomPrompt() {
        promptGenerating.value = true to "灵感生成中，请稍候..."
        launch {
            val promptResponse = mRepo.randomInspirationStory(OptimizeContentBody(input.value))
            promptGenerating.value = false to ""
            if (promptResponse.isSuccess) {
                input.value = promptResponse.data?.content
            } else {
                promptResponse.msg?.showToast()
            }
        }
    }

    fun randomInspirations(showLoading: Boolean = false) {
        requestWithLoading({ mRepo.getInspirationQuickList() }, showLoading = showLoading) {
            inspirationPrompts.value = data?.items

//            inspirationPrompts.value = listOf(
//                InspirationInfo("1", "1吉利古如吉利古如吉利古如吉利古如吉利古如吉利古如吉利古如吉利古如"),
//                InspirationInfo("2", "2吉利古如吉利古如吉利古如吉利古如吉利古如吉利古如吉利古如吉利古如"),
//            )
        }
    }

    fun nextStep(callback: (String?) -> Unit) {
        launchWithLoading(showLoading = true, {
            val promptResponse =
                mRepo.creationGeneratePre(
                    CreationGeneratePreBody(
                        if (inspirationModel()) CreationGeneratePreBody.GEN_WAY_INSPIRATION else CreationGeneratePreBody.GEN_WAY_CUSTOMIZED,
                        input.value,
                        checkedStyle?.data?.get()?.styleId,
                        checkedRatio?.data?.get(),
                    )
                )
            if (promptResponse.isSuccess) {
                callback.invoke(promptResponse.data?.dramasId)
            } else {
                promptResponse.msg?.showToast()
            }
        }, onError = {
            parseMessage(it)?.showToast()
        })
    }

    var checkedStyle: ItemCreationStyleVm? = null
    fun creationStyles() {
        launch {
            response(mRepo.creationStyleList()) {
                styleVms.clear()
                data?.items?.forEach { item ->
                    styleVms.add(initStyleVm(item))
                }
                mAdapterVm.notifyDataSetChanged()
            }
        }
    }

    fun addChooseStyle(style: CreationStyleInfo?) {
        val styleVm = styleVms.find { style?.styleId == it.data.get()?.styleId }
        if (styleVm == null) {
            if (style == null) return
            styleVms.add(0, initStyleVm(style).apply {
                checkedStyle?.checked?.set(false)
                checked.set(true)
                checkedStyle = this
            })
            mAdapterVm.notifyDataSetChanged()
        } else {
            checkedStyle?.checked?.set(false)
            styleVm.checked.set(true)
            checkedStyle = styleVm
        }
    }

    fun initStyleVm(item: CreationStyleInfo): ItemCreationStyleVm {
        return ItemCreationStyleVm(item).apply {
            if (checkedStyle == null) {
                checked.set(true)
                checkedStyle = this
            }
            onItemClick.value = {
//                            if (inspirationModel()) {
//                                EventManager.onAppEvent(EventConstants.INSPO_BOOK_CLICK_STYLE)
//                            } else {
//                                EventManager.onAppEvent(EventConstants.FREESTYLE_CLICK_STYLE)
//                            }
                if (!AppNavigator.checkLoginIntercepted(true)) {
                    if (item.isVip && !User.get().isVip) {
                        MembershipBenefitsActivity.start(
                            scene = if (inspirationModel()) {
                                MembershipBenefitsActivity.Scene.InspirationCreationStyle
                            } else {
                                MembershipBenefitsActivity.Scene.CustomCreationStyle
                            }
                        )
                    } else {
                        checkedStyle?.checked?.set(false)
                        this.checked.set(true)
                        checkedStyle = this
                    }
                }
            }
        }
    }
}