package com.kgmedia.fastfilm.feature.play.data

import com.kgmedia.fastfilm.core.network.apis.entity.TrackInfo

object FakeDataSource {

    fun getFakeData(pageNum: Int): List<TrackInfo> {
        if (pageNum > 3) return emptyList()

        return listOf(
            TrackInfo(
                trackId = "${pageNum}_1",
                trackTitle = "Big Buck Bunny (大雄兔)",
                playUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                coverUrl = "https://picsum.photos/seed/${pageNum}_1/720/1280",
                duration = 596474L // 约 9分56秒
            ),
            TrackInfo(
                trackId = "${pageNum}_2",
                trackTitle = "Elephant Dream",
                playUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                coverUrl = "https://picsum.photos/seed/${pageNum}_2/720/1280",
                duration = 653622L // 约 10分53秒
            ),
            TrackInfo(
                trackId = "${pageNum}_3",
                trackTitle = "For Bigger Blazes",
                playUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                coverUrl = "https://picsum.photos/seed/${pageNum}_3/720/1280",
                duration = 15000L // 15秒短视频（适合测试自动下一集）
            ),
            TrackInfo(
                trackId = "${pageNum}_4",
                trackTitle = "For Bigger Escapes",
                playUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                coverUrl = "https://picsum.photos/seed/${pageNum}_4/720/1280",
                duration = 15000L // 15秒短视频
            ),
            TrackInfo(
                trackId = "${pageNum}_5",
                trackTitle = "Tears of Steel",
                playUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                coverUrl = "https://picsum.photos/seed/${pageNum}_5/720/1280",
                duration = 734137L // 约 12分14秒
            )
        )
    }
}