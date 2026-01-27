package com.kgmedia.fastfilm.feature.play.data

import com.kgmedia.fastfilm.core.network.apis.entity.TrackInfo

object FakeDataSource {

    fun getFakeData(pageNum: Int): List<TrackInfo> {
        // 如果页数大于 3 就没数据了，模拟分页结束
        if (pageNum > 3) return emptyList()

        return listOf(
            TrackInfo(
                trackId = "${pageNum}_1",
                trackTitle = "Big Buck Bunny (大雄兔)",
                // Google 官方测试视频，带声音，H.264
                playUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                // 随机竖屏封面图
                coverUrl = "https://picsum.photos/seed/${pageNum}_1/720/1280"
            ),
            TrackInfo(
                trackId = "${pageNum}_2",
                trackTitle = "Elephant Dream (大象之梦)",
                playUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                coverUrl = "https://picsum.photos/seed/${pageNum}_2/720/1280"
            ),
            TrackInfo(
                trackId = "${pageNum}_3",
                trackTitle = "For Bigger Blazes (火焰)",
                playUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                coverUrl = "https://picsum.photos/seed/${pageNum}_3/720/1280"
            ),
            TrackInfo(
                trackId = "${pageNum}_4",
                trackTitle = "For Bigger Escapes (逃脱)",
                playUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                coverUrl = "https://picsum.photos/seed/${pageNum}_4/720/1280"
            ),
            TrackInfo(
                trackId = "${pageNum}_5",
                trackTitle = "Tears of Steel (钢铁之泪)",
                // 这是一个稍微长一点的视频，很适合测试加载和拖动
                playUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                coverUrl = "https://picsum.photos/seed/${pageNum}_5/720/1280"
            )
        )
    }
}