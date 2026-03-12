package com.kgmedia.fastfilm.router

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.permissions.requestRead2WritePermissions
import com.kgmedia.fastfilm.common.utils.IntentUtils
import com.kgmedia.fastfilm.common.utils.Utils
import com.kgmedia.fastfilm.core.manager.IRouter
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.core.network.apis.request.WorkBody
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.feature.editor.EditStoryboardVideoActivity
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity.Companion.launch
import com.kgmedia.fastfilm.widget.LoadingDialog2
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext

object AppRouter : IRouter {

    override fun openFromPush(context: Context, uri: String?) {
        uri?.let {
            AppLaunchJump.launchJump(uri.toUri())
        }
    }

    override fun navigate(context: Context, path: String?) {
        path?.let { jump(it) }
    }

    @JvmStatic
    fun jump(context: Context, schemeUri: String, openBrowser: Boolean) {
        if (openBrowser && schemeUri.startsWith("http")) {
            openBrowser(context, schemeUri)
        } else {
            jump(schemeUri)
        }
    }

    @JvmStatic
    fun jump(schemeUri: String?) {
        if (TextUtils.isEmpty(schemeUri)) {
            return
        }
        val uri = Uri.parse(schemeUri)
        if (uri != null) {
            var context: Context? = ActivityUtils.getTopActivity()
            if (context == null) {
                context = ActivityUtils.getTopActivity()
            }
            if (Constants.APP_SCHEME == uri.scheme) {
                jump(uri)
            } else if (schemeUri!!.endsWith(".apk")) {
                if (context is Activity) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || Utils.hasPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    ) {
                        IntentUtils.startDownload(context, schemeUri)
                    } else {
                        if (!Utils.isActivityFinishing(context)) {
                            val finalContext: Context? = context
                            (context as FragmentActivity).requestRead2WritePermissions(
                                context.getString(
                                    R.string.sdcard_permission_description
                                )
                            ) { allGranted: Boolean ->
                                if (allGranted && !Utils.isActivityFinishing(finalContext)) {
                                    IntentUtils.startDownload(finalContext, schemeUri)
                                }
                                null
                            }
                        }
                    }
                }
            } else if (schemeUri.startsWith("http")) {
                launch(context, schemeUri)
            }
        }
    }


    @JvmStatic
    fun jump(uri: Uri) {
        val host = uri.host
        Logger.i("host " + uri.host + ", uri=" + uri.toString())
        if (TextUtils.isEmpty(host)) {
            return
        }

        when (host) {
            "retoreCreator" -> {//fastfilm://retoreCreator?workId=%s&storyId=%s&workTitle=%s&workType=%s&totalEpisodeCnt=%s&workRatio=%s
                val workId = uri.getQueryParameter("workId")
                val storyId = uri.getQueryParameter("storyId")
                val workTitle = uri.getQueryParameter("workTitle")
                val workType = uri.getQueryParameter("workType")
                val workRatio = uri.getQueryParameter("workRatio")
                val totalEpisodeCnt = uri.getQueryParameter("totalEpisodeCnt")
                Logger.i("retoreCreator workId=$workId, storyId=$storyId, workTitle=$workTitle, workType=$workType, workRatio=$workRatio")
                startStory(workId, storyId, workTitle, workType?.toIntOrNull(), totalEpisodeCnt?.toIntOrNull(), workRatio)
            }

            "bookGenerated" -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                AppLaunchJump.launchJump(uri)
            }

            else -> {}
        }
    }

    fun openBrowser(context: Context, url: String?) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = url?.toUri()
        // 注意此处的判断intent.resolveActivity()可以返回显示该Intent的Activity对应的组件名
        // 官方解释 : Name of the component implementing an activity that can display the intent
        if (intent.resolveActivity(context.packageManager) != null) {
            val componentName = intent.resolveActivity(context.packageManager)
            context.startActivity(Intent.createChooser(intent, "请选择浏览器"))
        } else {
            // GlobalMethod.showToast(context, "链接错误或无浏览器");
        }
    }

    @JvmStatic
    fun startStory(work: WorkInfo?, storyId: String?) {
        work?.apply {
            startStory(workId, storyId, workTitle, workType, totalEpisodeCnt, workRatio)
        }
    }

    @JvmStatic
    fun startStory(
        workId: String?,
        storyId: String?,
        workTitle: String?,
        workType: Int?,
        totalEpisodeCnt:Int?,
        workRatio: String?,
    ) {
        if (workId.isNullOrEmpty() || storyId.isNullOrEmpty()) {
            return
        }
        val loading = (topActivity as? FragmentActivity)?.supportFragmentManager?.let {
            val dialog = LoadingDialog2()
            dialog.show(it)
            dialog
        }
        MainScope().launchNoCrash(
            block = {
                val res = withContext(Dispatchers.IO) {
                    ApiFactory.apiService.singleEpisodeDetails(
                        WorkBody(
                            workId,
                            storyId
                        )
                    )
                }
                if (!res.isSuccess) {
                    res.msg?.showToast()
                    loading?.dismissAllowingStateLoss()
                    return@launchNoCrash
                }
                jumpToTarget(
                    workId = workId,
                    workTitle = workTitle,
                    workType = workType,
                    totalEpisodeCnt = totalEpisodeCnt,
                    singleEpisodeDetailsInfo = res.data
                )
                loading?.dismissAllowingStateLoss()
            },
            onError = {
                loading?.dismissAllowingStateLoss()
            }
        )
    }

    /**
     *查看单集作品的剧集详情
     */
    @JvmStatic
    fun singleEpisodeSeriesDetails(
        work: WorkInfo?,
    ) {
        if (work == null) {
            return
        }
        val loading = (topActivity as? FragmentActivity)?.supportFragmentManager?.let {
            val dialog = LoadingDialog2()
            dialog.show(it)
            dialog
        }
        MainScope().launchNoCrash(
            block = {
                val res = withContext(Dispatchers.IO) {
                    ApiFactory.apiService.singleEpisodeSeriesDetails(
                        WorkBody(
                            work.workId, null
                        )
                    )
                }
                if (!res.isSuccess) {
                    res.msg?.showToast()
                    loading?.dismissAllowingStateLoss()
                    return@launchNoCrash
                }
                jumpToTarget(
                    workId = work.workId,
                    workTitle = work.workTitle,
                    workType = work.workType,
                    1,
                    singleEpisodeDetailsInfo = res.data
                )
                loading?.dismissAllowingStateLoss()
            },
            onError = {
                loading?.dismissAllowingStateLoss()
            }
        )
    }


    /**
     * 通过条件判断跳转到指定页面
     * @param workId 作品id
     * @param workTitle 作品标题
     * @param workType 作品类型
     * @param singleEpisodeDetailsInfo 剧集详情
     */
    private fun jumpToTarget(
        workId: String?,
        workTitle: String?,
        workType: Int?,
        totalEpisodeCnt: Int?,
        singleEpisodeDetailsInfo: EpisodeInfo?
    ) {
        val sortNo = singleEpisodeDetailsInfo?.sortNo ?: 1
        Logger.d("打印参数：jumpToTarget singleEpisodeDetailsInfo?.stepProgress = ${singleEpisodeDetailsInfo?.stepProgress}")
        //枚举创作流程进度 0=待创建 10=剧本输入 15=剧本生成中 20=资产生成中 30=分镜描述中 32=分镜视频生成中35待合成 40=已完成 45=分镜重绘中 50=重绘已完成
        when (singleEpisodeDetailsInfo?.stepProgress) {
            EpisodeInfo.PROGRESS_SUMMARY_GENERATING -> {
                //10=剧本输入
                StoryEditorActivity.restore(
                    context = topActivity,
                    step = 1,
                    workId = workId,
                    storyId = singleEpisodeDetailsInfo.storyId,
                    workTitle = workTitle,
                    chapterIndex = sortNo,
                    model = getModel(totalEpisodeCnt,workType = workType, sortNo = sortNo, firstCreated = singleEpisodeDetailsInfo.isFirstCreated()),
                    videoRatio = singleEpisodeDetailsInfo.workRatio,
                )
            }

            EpisodeInfo.PROGRESS_STORY_GENERATING -> {
                //15=剧本生成中
                StoryEditorActivity.restore(
                    context = topActivity,
                    step = 2,
                    workId = workId,
                    storyId = singleEpisodeDetailsInfo.storyId,
                    workTitle = workTitle,
                    chapterIndex = sortNo,
                    model = getModel(totalEpisodeCnt,workType = workType, sortNo = sortNo, firstCreated = singleEpisodeDetailsInfo.isFirstCreated()),
                    videoRatio = singleEpisodeDetailsInfo.workRatio,
                )
            }

            EpisodeInfo.PROGRESS_ASSET_GENERATING -> {
                //20=资产生成中
                StoryEditorActivity.restore(
                    context = topActivity,
                    step = 3,
                    workId = workId,
                    storyId = singleEpisodeDetailsInfo.storyId,
                    workTitle = workTitle,
                    taskId = singleEpisodeDetailsInfo.taskGenResultStoryVO?.taskId,
                    chapterIndex = sortNo,
                    model = getModel(totalEpisodeCnt,workType = workType, sortNo = sortNo, firstCreated = singleEpisodeDetailsInfo.isFirstCreated()),
                    videoRatio = singleEpisodeDetailsInfo.workRatio,
                )
            }

            EpisodeInfo.PROGRESS_STORYBOARD_GENERATING_ASSET -> {
                //30=分镜描述中
                StoryEditorActivity.restore(
                    context = topActivity,
                    step = 4,
                    workId = workId,
                    storyId = singleEpisodeDetailsInfo.storyId,
                    workTitle = workTitle,
                    taskId = singleEpisodeDetailsInfo.taskGenResultStoryVO?.taskId,
                    chapterIndex = sortNo,
                    model = getModel(totalEpisodeCnt,workType = workType, sortNo = sortNo, firstCreated = singleEpisodeDetailsInfo.isFirstCreated()),
                    videoRatio = singleEpisodeDetailsInfo.workRatio,
                )
            }

            EpisodeInfo.PROGRESS_STORYBOARD_GENERATING,
            EpisodeInfo.PROGRESS_WAITING_COMPOSING,
            EpisodeInfo.EPISODE_REPAINT_STATE,
            EpisodeInfo.EPISODE_REPAINT_COMPLETED -> {
                //32=分镜视频生成中 35待合成 45=分镜重绘中 50=重绘已完成
                EditStoryboardVideoActivity.start(
                    storyTitle = workTitle?.let { "$it：第${singleEpisodeDetailsInfo.sortNo}集" },
                    workId = workId,
                    storyId = singleEpisodeDetailsInfo.storyId,
                    workRatio = singleEpisodeDetailsInfo.workRatio,
                    story = singleEpisodeDetailsInfo
                )
            }
        }
    }

    fun getModel(totalEpisodeCnt: Int?, workType: Int?, sortNo: Int = 0, firstCreated: Boolean = true): Int {
        if (totalEpisodeCnt == 1) return DramaGenerateConstants.MODEL_STORY_SINGLE
        return getModel(workType = workType, sortNo = sortNo,firstCreated = firstCreated)
    }

    fun getModel(workType: Int?, sortNo: Int = 0, firstCreated: Boolean = true): Int {
        Logger.d("打印参数：getModel workType = $workType ,sortNo = $sortNo ")
        when (workType) {
            WorkInfo.INSPIRATION_CREATION -> {
                //作品类型：1-灵感创作
                if (firstCreated && sortNo <= 1) {
                    //第一集创作(灵感)
                    return DramaGenerateConstants.MODEL_INSPIRATION
                }
                //继续创作(灵感)
                return DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION
            }

            WorkInfo.NOVEL_CREATION -> {
                //作品类型：2-小说创作
                if (firstCreated && sortNo <= 1) {
                    //第一集创作(小说)
                    return DramaGenerateConstants.MODEL_STORY
                }
                //继续创作(小说)
                return DramaGenerateConstants.MODEL_CONTINUE_STORY
            }

            WorkInfo.NOVEL_UPLOAD -> {
                //作品类型：3-上传小说
                if (firstCreated && sortNo <= 1) {
                    //第一集创作(小说文档模式)
                    return DramaGenerateConstants.MODEL_STORY_DOCUMENTS
                }
                //继续创作(小说文档模式)
                return DramaGenerateConstants.MODE_CONTINUE_WRITING_NOVEL_DOCUMENTS
            }

            else -> {
                if (firstCreated && sortNo <= 1) {
                    //第一集创作(小说)
                    return DramaGenerateConstants.MODEL_STORY
                }
                //继续创作(小说)
                return DramaGenerateConstants.MODEL_CONTINUE_STORY
            }
        }
    }
}