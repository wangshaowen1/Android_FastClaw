package com.shiyuan.fastclaw.widget.progress

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.shiyuan.fastclaw.R

/**
 * WaveProgressView 使用示例
 */
class WaveProgressDemoActivity : AppCompatActivity() {

    private lateinit var waveProgress1: WaveProgressView
    private lateinit var waveProgress2: WaveProgressView
    private lateinit var waveProgress3: WaveProgressView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.demo_wave_progress)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        waveProgress1 = findViewById(R.id.waveProgress1)
        waveProgress2 = findViewById(R.id.waveProgress2)
        waveProgress3 = findViewById(R.id.waveProgress3)

        // 代码中设置属性
        waveProgress1.progress = 50
        waveProgress1.text = "50%"
        waveProgress1.textColor = 0xFFFFFFFF.toInt()
        waveProgress1.textSize = 24f
        waveProgress1.waveColor = 0xFF4CAF50.toInt()
        waveProgress1.waveBackgroundColor = 0xFFE0E0E0.toInt()
        waveProgress1.waveAlpha = 180

        // 第二层波浪参数
        waveProgress1.waveAmplitude = 20f
        waveProgress1.waveCount = 1f
        waveProgress1.waveSpeed = 100f

        // 是否启用波浪动画
        waveProgress1.isWaveAnimEnabled = true
    }

    private fun setupListeners() {
        // 示例：动态修改进度
        waveProgress1.postDelayed({
            waveProgress1.progress = 75
            waveProgress1.text = "75%"
        }, 2000)
    }
}

/**
 * WaveProgressView 使用示例
 *
 * ## XML 属性说明
 *
 * | 属性 | 类型 | 默认值 | 说明 |
 * |------|------|--------|------|
 * | wpv_progress | int | 0 | 进度值 0-100 |
 * | wpv_waveColor | color | #4CAF50 | 波浪颜色 |
 * | wpv_waveBackgroundColor | color | #E0E0E0 | 波浪背景色 |
 * | wpv_waveAlpha | int | 180 | 波浪透明度 0-255 |
 * | wpv_text | string | "" | 中心文字 |
 * | wpv_textColor | color | #FFFFFF | 中心文字颜色 |
 * | wpv_textSize | dimension | 16sp | 中心文字大小 |
 * | wpv_waveAmplitude | dimension | 20dp | 波浪振幅 |
 * | wpv_waveCount | float | 1.0 | 波浪周期数量 |
 * | wpv_waveSpeed | float | 100f | 波浪移动速度 |
 * | wpv_waveAnimEnabled | boolean | true | 是否启用波浪动画 |
 *
 * ## 代码中使用
 *
 * ```kotlin
 * // 设置进度
 * waveProgressView.progress = 75
 * waveProgressView.text = "75%"
 *
 * // 设置文字样式
 * waveProgressView.textColor = Color.WHITE
 * waveProgressView.textSize = 20f
 *
 * // 设置波浪样式
 * waveProgressView.waveColor = Color.parseColor("#4CAF50")
 * waveProgressView.waveBackgroundColor = Color.parseColor("#E0E0E0")
 * waveProgressView.waveAlpha = 180
 *
 * // 设置波浪动画
 * waveProgressView.waveAmplitude = 20f
 * waveProgressView.waveCount = 1f
 * waveProgressView.waveSpeed = 100f
 * waveProgressView.isWaveAnimEnabled = true
 * ```
 *
 * ## DataBinding / BindingAdapter 使用
 *
 * ```xml
 * <com.shiyuan.fastclaw.widget.progress.WaveProgressView
 *     android:layout_width="150dp"
 *     android:layout_height="150dp"
 *     app:wpv_progress="@{viewModel.progress}"
 *     app:wpv_text="@{viewModel.progressText}"
 *     app:wpv_waveColor="@{viewModel.waveColor}"
 *     app:wpv_textColor="@{viewModel.textColor}"
 *     app:wpv_textSize="24sp" />
 * ```
 */
object WaveProgressUsageExample
