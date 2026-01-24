package com.kgmedia.fastfilm.feature.message

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.BannerInfo
import com.kgmedia.fastfilm.core.network.apis.request.BannerBody
import com.kgmedia.fastfilm.core.network.apis.request.MessagesTypeBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.message.likes.SystemMessageLikesActivity
import com.kgmedia.fastfilm.feature.message.systemnotice.SystemNoticeActivity
import com.kgmedia.fastfilm.feature.message.watching.SystemMessageWatchingActivity
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * 首页-消息
 */
class MessageViewModel : AppRepositoryViewModel<MessagesRepository>() {
    val loadState by lazy { ObservableInt() }
    val adapter by lazy { QuickAdapter<ItemMessageVM>(R.layout.item_message_type, emptyList()) }
    val eventVM by lazy { ItemEventsViewModel<Int>() }
    val unreadNum by lazy { MutableLiveData<Int?>() }

    //banner数据
    val bannerList = MutableLiveData<List<BannerInfo>?>()

    //用户显示轮播图
    val isShowCarousel = ObservableField<Boolean>()

    override fun onCreate() {
        super.onCreate()
        //判断是否显示轮播图
        displayCarouselImage()
        //获取banner信息
        getAppBannerInfo()
        observeEvents()
    }

    override fun onResume() {
        super.onResume()
        //判断是否显示轮播图
        displayCarouselImage()
        refreshMessages()
    }

    private fun observeEvents() {
        launch {
            eventVM.events.collect { event ->
                markMessagesAsRead(event)
                when (event) {
                    MessagesTypeBody.TYPE_WATCHING -> {
                        startActivity(SystemMessageWatchingActivity::class.java)
                    }

                    MessagesTypeBody.TYPE_LIKED -> {
                        startActivity(SystemMessageLikesActivity::class.java)
                    }

                    MessagesTypeBody.TYPE_SYSTEM_NOTICE -> {
                        startActivity(SystemNoticeActivity::class.java)
                    }
                }
            }
        }
    }

    fun refreshMessages() {
        if (adapter.itemCount == 0) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                response(
                    bean = mRepo.messagesSummary(),
                    result = {
                        unreadNum.value = data?.unreadNumTotal
                        data?.apply {
                            adapter.submitList(null)
                            watchingMessage?.let {
                                adapter.add(
                                    ItemMessageVM(
                                        eventVM,
                                        MessagesTypeBody.TYPE_WATCHING,
                                        R.drawable.ic_message_watching,
                                        "追更",
                                        it
                                    )
                                )
                            }
                            likesMessage?.let {
                                adapter.add(
                                    ItemMessageVM(
                                        eventVM,
                                        MessagesTypeBody.TYPE_LIKED,
                                        R.drawable.ic_message_liked,
                                        "喜欢",
                                        it
                                    )
                                )
                            }
                            systemMessages?.let {
                                adapter.add(
                                    ItemMessageVM(
                                        eventVM,
                                        MessagesTypeBody.TYPE_SYSTEM_NOTICE,
                                        R.drawable.ic_message_system_notice,
                                        "系统通知",
                                        it
                                    )
                                )
                            }
                        }
                        updateLoadState()
                    },
                    error = {
                        updateLoadState()
                    }
                )
            },
            onError = {
                updateLoadState()
            })
    }

    /**
     * 消息全部设置成已读
     * @param type 消息类型[MessagesTypeBody]
     */
    private fun markMessagesAsRead(type: Int) {
        launch {
            response(mRepo.messagesMarkRead(type)) {
                refreshMessages()
            }
        }
    }

    private fun updateLoadState() {
        if (adapter.itemCount == 0) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    /**
     * 获取banner信息
     */
    fun getAppBannerInfo() {
        launch {
            response(
                bean = mRepo.appBanner(BannerBody(BannerBody.LOCATION_MESSAGE)),
                result = {
                    bannerList.value = data?.items
                    //判断是否显示轮播图
                    displayCarouselImage()
                })
        }
    }

    /**
     * 判断是否显示轮播图
     */
    private fun displayCarouselImage() {
        val isVip = User.get().isVip
        if (isVip) {
            isShowCarousel.set(false)
            return
        }
        if ((bannerList.value?.size ?: 0) <= 0) {
            isShowCarousel.set(false)
            return
        }
        isShowCarousel.set(true)
    }
}