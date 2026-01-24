package com.kgmedia.fastfilm.feature.creator.generate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.core.network.apis.request.DramaGenerateBody
import com.kgmedia.fastfilm.databinding.ActivityStoryEditorBinding
import com.kgmedia.fastfilm.feature.creator.character.CharacterFragment
import com.nhsd.basic.BaseViewModelActivity

/**
 * 故事编辑设定界面（分镜生成，角色确认）
 */
class StoryEditorActivity : BaseViewModelActivity<ActivityStoryEditorBinding, StoryEditorViewModel>() {

    companion object {
        const val KEY_STEP = "step"


        /**
         * 从头开始生成
         * @param content 故事内容
         * @param styleId 故事风格ID
         * @param videoRatio 视频比例
         * @param model 故事生成方式
         * @param otherExtraId 续写(剧集id)，同款（剧本id）
         */
        @JvmStatic
        fun start(
            context: Context?,
            content: String?,
            styleId: String? = null,
            videoRatio: Int? = null,
            model: Int? = DramaGenerateBody.GEN_WAY_INSPIRATION,
            otherExtraId: String? = null,
        ) {
            val context = context ?: return
            val starter = Intent(context, StoryEditorActivity::class.java)
            starter.putExtra(Constants.Extra.EXTRA_ID, otherExtraId)
            starter.putExtra(Constants.Extra.MODE, model)
            starter.putExtra(Constants.Extra.CONTENT, content)
            starter.putExtra(Constants.Extra.STYLE_ID, styleId)
            starter.putExtra(Constants.Extra.VIDEO_RATIO, videoRatio)
            context.startActivity(starter)
        }

        /**
         * @param dramaId 创作ID
         * @param title 故事标题，直接进步骤2要传
         * @param scriptContent 故事大纲 步骤1给
         * @param step 步骤几  1，恢复生成  2，跳到角色列表界面
         */
        @JvmStatic
        fun start(context: Context?, step: Int? = 2, dramaId: String?, title: String? = null, scriptContent: String? = null) {
            val context = context ?: return
            val starter = Intent(context, StoryEditorActivity::class.java)
            starter.putExtra(Constants.Extra.ID, dramaId)
            if (title != null) {
                starter.putExtra(Constants.Extra.TITLE, title)
            }
            starter.putExtra(Constants.Extra.CONTENT, scriptContent)
            starter.putExtra(KEY_STEP, step)
            context.startActivity(starter)
        }
    }


    override fun onBackPressed() {
        if (mRealVM.step.value == 1 && mRealVM.backWarning) {//因为步骤1流式输出的时候 不会保存生成的内容
            showConfirmPrompt("退出后将无法保留故事内容，请谨慎操作") {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.title.observe(this) {
            mRealVM.mToolbarVM.mTitle.set(it)
        }
        mRealVM.step.observe(this) {
            if (it == 2) {
                supportFragmentManager.beginTransaction().replace(R.id.container, CharacterFragment.newInstance(mRealVM.dramaId))
                    .commitAllowingStateLoss()
            } else {
                supportFragmentManager.beginTransaction().replace(R.id.container, StoryBoardFragment.newInstance())
                    .commitAllowingStateLoss()
            }
        }
    }

}