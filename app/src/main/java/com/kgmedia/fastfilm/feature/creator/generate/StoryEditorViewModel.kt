package com.kgmedia.fastfilm.feature.creator.generate

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.request.DramaGenerateBody

class StoryEditorViewModel : AppRepositoryViewModel<AppRepository>() {

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "未命名标题",
        background = Color.TRANSPARENT
    )

    var dramaId: String? = null
    val title = MutableLiveData(mBundle.getString(Constants.Extra.TITLE, "未命名标题"))
    val step = MutableLiveData<Int>()
    val model by lazy { mBundle.getInt(Constants.Extra.MODE, DramaGenerateBody.GEN_WAY_INSPIRATION) }
    var backWarning = false//子界面在流式输出的时候 用户要退出的时候需要弹出提示

    override fun onCreate() {
        super.onCreate()
        dramaId = mBundle.getString(Constants.Extra.ID)
        step.value = mBundle.getInt(StoryEditorActivity.KEY_STEP, 1)
    }


}