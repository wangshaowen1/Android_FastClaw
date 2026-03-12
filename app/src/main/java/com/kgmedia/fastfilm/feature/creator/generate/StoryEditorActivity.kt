package com.kgmedia.fastfilm.feature.creator.generate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.databinding.ActivityStoryEditorBinding
import com.kgmedia.fastfilm.feature.creator.assets.AssetManagementFragment
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.kgmedia.fastfilm.feature.creator.storyboard.StoryboardDesignFragment
import com.kgmedia.fastfilm.feature.editor.info.DramaTitleEditDialog
import com.nhsd.basic.BaseViewModelActivity

/**
 * 作品生成根界面（灵感模式有5步，小说模式只有4步，从剧本生成开始）
 * 灵感模式，小说续创也是4步 从剧本生成开始
 */
class StoryEditorActivity :
    BaseViewModelActivity<ActivityStoryEditorBinding, StoryEditorViewModel>() {

    companion object {
        const val KEY_STEP = "step"

        //集数范围
        const val EPISODE_RANGE_CODE = "episodeRangeCode"

        //自定义集数
        const val CUSTOM_EPISODE_COUNT = "customEpisodeCount"

        /**
         * 从头开始生成
         * @param workId 作品id（首次生成==》目前仅小说文档上传的情况下有值）
         * @param content 故事内容
         * @param name 故事名称
         * @param styleId 故事风格ID
         * @param videoRatio 视频比例
         * @param model 故事生成方式
         * @param otherExtraId 续写(剧集id)，同款（剧本id）
         */
        @JvmStatic
        fun start(
            context: Context?,
            workId: String?,
            content: String?,
            name: String?,
            episodeRangeCode: String? = null,
            customEpisodeCount: String? = null,
            videoRatio: String? = null,
            styleId: String? = null,
            model: Int? = DramaGenerateConstants.MODEL_INSPIRATION,
            otherExtraId: String? = null,
        ) {
            val context = context ?: return
            val starter = Intent(context, StoryEditorActivity::class.java)
            starter.putExtra(Constants.Extra.EXTRA_ID, otherExtraId)
            starter.putExtra(Constants.Extra.MODE, model)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.CONTENT, content)
            starter.putExtra(Constants.Extra.TITLE, name)
            starter.putExtra(EPISODE_RANGE_CODE, episodeRangeCode)
            starter.putExtra(CUSTOM_EPISODE_COUNT, customEpisodeCount)
            starter.putExtra(Constants.Extra.STYLE_ID, styleId)
            starter.putExtra(Constants.Extra.VIDEO_RATIO, videoRatio)
            context.startActivity(starter)
        }


        //续写
        @JvmStatic
        fun startContinue(
            context: Context?,
            workId: String? = null,
            workTitle: String? = null,
            //剧集id
            storyId: String? = null,
            //上一集的剧集id
            previousStoryId: String? = null,
            //当前章节的序号
            chapterIndex: Int? = 1,
            //剧集内容，续写小说的时候需要
            dramaContent: String? = null,
            model: Int? = DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION,
            videoRatio: String? = null,
        ) {
            val context = context ?: return
            val starter = Intent(context, StoryEditorActivity::class.java)
            starter.putExtra(Constants.Extra.MODE, model)
            starter.putExtra(Constants.Extra.TITLE, workTitle)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.STORY_ID, storyId)
            starter.putExtra(Constants.Extra.PREVIOUS_STORY_ID, previousStoryId)
            starter.putExtra(Constants.Extra.CHAPTER_INDEX, chapterIndex)
            starter.putExtra(Constants.Extra.CONTENT, dramaContent)
            starter.putExtra(Constants.Extra.VIDEO_RATIO, videoRatio)
            context.startActivity(starter)
        }

        /**
         * @param step 步骤几  1 故事梗概 2 故事剧本 3 资产管理 4 分镜设计
         */
        @JvmStatic
        fun restore(
            context: Context?,
            step: Int,
            workId: String?,
            storyId: String?,
            taskId: String? = null,
            workTitle: String? = null,
            //当前章节的序号
            chapterIndex: Int? = 1,
            model: Int? = DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION,
            videoRatio: String? = null,
        ) {
            val context = context ?: return
            val starter = Intent(context, StoryEditorActivity::class.java)
            starter.putExtra(KEY_STEP, step)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.STORY_ID, storyId)
            starter.putExtra(Constants.Extra.TASK_ID, taskId)
            starter.putExtra(Constants.Extra.TITLE, workTitle)
            starter.putExtra(Constants.Extra.RESTORE, true)
            starter.putExtra(Constants.Extra.CHAPTER_INDEX, chapterIndex)
            starter.putExtra(Constants.Extra.MODE, model)
            starter.putExtra(Constants.Extra.VIDEO_RATIO, videoRatio)
            context.startActivity(starter)
        }
    }

    override fun onBackPressed() {
        if (mRealVM.step.value == 1 && mRealVM.backWarning) {//因为步骤1流式输出的时候 不会保存生成的内容
            showConfirmPrompt("退出后将无法保留生成的内容，请谨慎操作") {
                super.onBackPressed()
            }
        } else {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
            if (currentFragment is OnBackPressedHandler) {
                val isHandled = currentFragment.handleBackPressed()
                if (isHandled) return // 如果 Fragment 返回 true，表示它拦截了退出，Activity 直接 return
            }
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.title.observe(this) {
            mRealVM.mToolbarVM.mTitle.set(it)
        }
        mRealVM.step.observe(this) {
            when (it) {
                1 -> {
                    if (mRealVM.startFrom.value == 2) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container, DramaGenerateStep2Fragment.newInstance())
                            .commitAllowingStateLoss()
                    } else {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container, DramaGenerateStep1Fragment.newInstance())
                            .commitAllowingStateLoss()
                    }
                }

                2 -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, DramaGenerateStep2Fragment.newInstance())
                        .commitAllowingStateLoss()
                }

                3 -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, AssetManagementFragment.newInstance())
                        .commitAllowingStateLoss()
                }

                4 -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, StoryboardDesignFragment.newInstance())
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.tvWorkName, R.id.btnModify -> {
                val isEdit =
                    (mRealVM.step.value == 1 || (mRealVM.step.value == 2 && mRealVM.model == DramaGenerateConstants.MODEL_STORY))
                if (!isEdit) return
                val dialog = DramaTitleEditDialog.newInstance(mRealVM.title.value)
                dialog.onSave = {
                    mRealVM.title.value = it
                }
                dialog.show(supportFragmentManager)
            }
        }
    }
}