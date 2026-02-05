package com.kgmedia.fastfilm.feature.play.more

import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.EditDramaBody
import com.kgmedia.fastfilm.core.response
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast

/**
 * 短剧播放
 */
class ShortInfoEditorViewModel : AppRepositoryViewModel<AppRepository>() {

    fun editShotInfo(dramaId: String?, dramaTitle: String, dramaIntro: String) {
        launchWithLoading {
            response(mRepo.api.editShotDramaInfo(EditDramaBody(dramaId, dramaTitle, dramaIntro))) {
                "修改成功".showToast()
                dismiss()
                FlowEventBus.postSync(Events.ShortDramasEditInfoEvent(dramaId, dramaTitle, dramaIntro))
            }
        }
    }
}