package com.kgmedia.fastfilm.feature.splash.newer

import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.ExampleWorkInfo

class GuideStep2ViewModel : AppRepositoryViewModel<AppRepository>() {

    val examples = MutableLiveData<List<ExampleWorkInfo>>()


    override fun onCreate() {
        super.onCreate()
        getExamples()
    }

    fun getExamples() {
//        examples.value = testWorkList
        requestWithLoading({ mRepo.api.getNewerExampleWorkList() }) {
            examples.value = data
        }
    }

    val testWorkList = listOf(
        // 1. 横屏电影 (16:9) - 自然风光
        ExampleWorkInfo(
            workId = "video_001",
            workTitle = "壮丽的山脉河流",
            workCoverUrl = "https://picsum.photos/id/10/800/450", // 16:9 比例封面
            workDesc = "高清自然风光实拍，适合测试横屏播放器的渲染效果。",
            workRatio = "16:9",
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            duration = 596
        ),

        // 2. 竖屏短视频 (9:16) - 城市生活 (使用兼容性高的短视频源)
        ExampleWorkInfo(
            workId = "video_002",
            workTitle = "都市霓虹夜景",
            workCoverUrl = "https://picsum.photos/id/230/450/800", // 9:16 比例封面
            workDesc = "垂直竖屏视频，用于测试 TikTok/抖音 风格的全屏滑动翻页流。",
            workRatio = "9:16",
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            duration = 15
        ),

        // 3. 电影长片 (21:9) - 宇宙探索
        ExampleWorkInfo(
            workId = "video_003",
            workTitle = "大象之梦：开源动画",
            workCoverUrl = "https://picsum.photos/id/111/840/360", // 宽屏比例封面
            workDesc = "经典开源电影项目，适合测试不同比例下的黑边填充逻辑。",
            workRatio = "21:9",
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            duration = 653
        ),

        // 4. 正方形视频 (1:1) - 创意设计
        ExampleWorkInfo(
            workId = "video_004",
            workTitle = "极简主义色彩转换",
            workCoverUrl = "https://picsum.photos/id/145/500/500", // 1:1 比例封面
            workDesc = "正方形比例视频，常用于社交软件动态或广告展示位测试。",
            workRatio = "1:1",
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            duration = 15
        ),

        // 5. 自动适配 (Adaptive) - HLS 流媒体
        ExampleWorkInfo(
            workId = "video_005",
            workTitle = "流媒体自适应测试 (M3U8)",
            workCoverUrl = "https://picsum.photos/id/201/600/400",
            workDesc = "这是一个 HLS (.m3u8) 链接，用于测试 Media3 对多码率自适应流的支持。",
            workRatio = "adaptive",
            videoUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            duration = 630
        )
    )

}