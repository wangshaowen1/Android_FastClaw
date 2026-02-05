package com.kgmedia.fastfilm.feature.creator.generate

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants

class StoryEditorViewModel : AppRepositoryViewModel<AppRepository>() {

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "未命名标题",
        background = Color.TRANSPARENT
    )

    var workId: String? = null//作品id
    var storyId: String? = null//当前正在创作的剧集id
    //作品标题
    val title = MutableLiveData(mBundle.getString(Constants.Extra.TITLE, "未命名标题"))
    val step = MutableLiveData<Int>()//当前处于的步骤序号1-5
    val startFrom = MutableLiveData<Int>()//从1还是从2开始，从2开始就是4步创作流程
    val model by lazy { mBundle.getInt(Constants.Extra.MODE, DramaGenerateConstants.MODEL_INSPIRATION) }
    var backWarning = false//子界面在流式输出的时候 用户要退出的时候需要弹出提示

    override fun onCreate() {
        super.onCreate()
        workId = mBundle.getString(Constants.Extra.ID)

        when (model) {
            DramaGenerateConstants.MODEL_INSPIRATION -> {
                step.value = 1
                startFrom.value = 1
            }

            DramaGenerateConstants.MODEL_STORY -> {
                step.value = 2
                startFrom.value = 2
            }

            DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION -> {
                step.value = 2
                startFrom.value = 2
            }

            DramaGenerateConstants.MODEL_CONTINUE_STORY -> {
                step.value = 2
                startFrom.value = 2
            }

            else -> step.value = mBundle.getInt(StoryEditorActivity.KEY_STEP, 1)//中途恢复界面走这里
        }
    }

    fun nextStep(step: Int, workId: String? = null, storyId: String? = null) {
        workId?.let {
            this.workId = it
        }
        storyId?.let {
            this.storyId = it
        }
        this.step.value = step
    }
}