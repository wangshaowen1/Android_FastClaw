package com.shiyuan.fastclaw.feature.information

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.utils.KeyboardUtils.hideKeyboard
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.ext.getBundleValue
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.network.apis.request.JobCityQueryBody
import com.shiyuan.fastclaw.core.network.apis.response.CityLocation
import com.shiyuan.fastclaw.core.network.apis.response.JobCityData
import com.shiyuan.fastclaw.databinding.ActivityJobCitySelectorBinding
import com.shiyuan.fastclaw.feature.information.adapter.JobCitySelectedAdapter
import com.shiyuan.fastclaw.feature.information.adapter.LocationSearchAdapter
import com.shiyuan.fastclaw.feature.information.viewModel.JobCitySelectorModel

/**
 * @desc：工作城市选择
 * @author：CaoWei
 * @date：2026/6/10 16:52
 */
class JobCitySelectorActivity : BaseViewModelActivity<ActivityJobCitySelectorBinding, JobCitySelectorModel>() {

    // 已选城市集合，LinkedHashMap 保证按选中顺序展示且方便按键去重
    private val selectedLocations = linkedMapOf<String, CityLocation>()
    // 搜索结果列表适配器，复用当前搜索关键词用于高亮
    private val searchAdapter = LocationSearchAdapter(
        selectedLocations = selectedLocations,
        keywordProvider = { binding.etSearch.text?.toString().orEmpty() },
    ) {
        binding.etSearch.setText("")
        hideKeyboard(binding.etSearch)
        toggleLocation(it)
    }
    // 底部已选城市横滑标签适配器
    private val selectedAdapter by lazy {
        JobCitySelectedAdapter { item -> removeSelectedLocation(item) }
    }
    // 主线程 Handler，用于处理输入防抖的搜索请求
    private val searchHandler = Handler(Looper.getMainLooper())
    // 当前等待执行的搜索任务，配合防抖取消上一次未执行请求
    private var pendingSearch: Runnable? = null
    // 最大可选城市数，现居住城市为 1，意向城市为 3
    private var maxSelectCount = 3
    // 分组标题视图集合，用于字母索引联动时定位滚动位置
    private val sectionTitleViews = linkedMapOf<String, View>()
    // 右侧 A-Z 索引数据
    private var alphabetIndex = emptyList<String>()
    // 当前选中的字母索引
    private var selectedAlphabet: String? = null
    // 入口场景编码，区分现居住城市与意向城市
    private val mType by lazy { intent.getBundleValue("sceneCode", "") }
    // 城市列表接口请求体，固定当前场景
    private val mCityParams by lazy { JobCityQueryBody(sceneCode = mType) }
    // 接口返回的城市数据缓存
    private var mData: JobCityData? = null
    // 搜索输入的防抖时间
    private val mSearchDelayTime = 300L

    companion object {
        /**
         * 打开城市选择器页面。
         *
         * @param activity 当前页面 Activity。
         * @param sceneCode 城市字段场景编码，区分现居住城市和意向城市。
         */
        fun start(activity: Activity?, sceneCode: String) {
            activity ?: return
            val intent = Intent(activity, JobCitySelectorActivity::class.java)
            intent.putExtra("sceneCode", sceneCode)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 注册数据观察
        initObserve()
        // 初始化视图与点击事件，并触发首次加载
        initView()
    }

    override fun setTranslucentStatus() {
        // 设置沉浸式状态栏：深色字体、白色导航栏、透明状态栏
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

    /**
     * 初始化页面文案、点击事件、搜索输入、字母索引触摸和列表适配器。
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        // 根据入口场景切换标题文案与最大可选数
        if (mType == JobCityQueryBody.EXPECTED_CITY) {
            binding.tvSubtitle.text = getString(R.string.city_selector_add_multiple)
            maxSelectCount = 10
        } else {
            binding.tvSubtitle.text = getString(R.string.city_selector_now_city)
            maxSelectCount = 1
        }
        // 顶部关闭按钮
        binding.ivClose.setOnSingleClickListener { finish() }
        // 搜索框右侧清空图标
        binding.ivClearSearch.setOnSingleClickListener {
            binding.etSearch.setText("")
        }
        // 底部完成按钮：未选时提示，至少选一个才能确认
        binding.btnComplete.setOnSingleClickListener {
            if (selectedLocations.isEmpty()) {
                showToast(R.string.city_selector_at_least)
            } else {
                val msgGson = JobCityData.toJson(mType, selectedLocations)
                FlowEventBus.postSync(Events.ClawAgentEvent(msgGson,AppEvent.KLX_C_EXPECTED_CITY_COMPLETED))
                finish()
            }
        }
        // 监听软键盘搜索按钮触发
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                mCityParams.apply { keyword = binding.etSearch.text?.toString().orEmpty() }
                mRealVM.getJobCityList(mCityParams)
                true
            } else {
                false
            }
        }
        // 输入框文本变化：清空图标显隐 + 搜索防抖
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString().orEmpty()
                // 有内容才显示清空图标
                binding.ivClearSearch.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
                // 取消上一次还未执行的搜索，重新计时
                pendingSearch?.let(searchHandler::removeCallbacks)
                pendingSearch = Runnable {
                    mRealVM.getJobCityList(mCityParams.apply { keyword = text })
                }.also {
                    searchHandler.postDelayed(it, mSearchDelayTime)
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        // 右侧 A-Z 索引触摸事件
        binding.tvAlphabetIndex.setOnTouchListener { _, event ->
            handleAlphabetTouch(event)
        }
        // 左侧滚动时反向同步 A-Z 选中态
        binding.scrollNormal.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            syncAlphabetByScroll(scrollY)
        }
        // 搜索结果列表
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapter
        // 底部已选城市横滑列表，关闭默认动画避免重复添加闪烁
        binding.rvSelectedChips.apply {
            layoutManager = LinearLayoutManager(
                this@JobCitySelectorActivity,
                RecyclerView.HORIZONTAL,
                false,
            )
            itemAnimator = null
            adapter = selectedAdapter
        }
        // 首次进入加载默认城市列表
        mRealVM.getJobCityList(mCityParams)
    }

    /**
     * 监听城市接口数据变化，收到新数据后刷新页面。
     */
    private fun initObserve() {
        mRealVM.jobCityList.observe(this) {
            it?.let {
                mData = it
                renderCityData()
            }
        }
    }

    /**
     * 根据当前是否处于搜索状态，渲染普通城市分组或搜索结果。
     */
    private fun renderCityData() {
        //maxSelectCount = mData?.maxSelectCount ?: maxSelectCount
        // 先把接口反显的已选城市合并到当前已选集合
        mData?.selectedLocations.orEmpty().forEach { item ->
            item.regionCode?.let { selectedLocations[it] = item }
        }
        if (binding.etSearch.text.isBlank()) {
            // 非搜索状态：展示普通滚动列表 + A-Z 索引
            binding.scrollNormal.visibility = View.VISIBLE
            binding.tvAlphabetIndex.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
            binding.tvEmptySearch.visibility = View.GONE
            renderNormalSections(mData)
        } else {
            // 搜索状态：展示搜索结果或空态
            binding.scrollNormal.visibility = View.GONE
            binding.tvAlphabetIndex.visibility = View.GONE
            val matchedLocations = mData?.matchedLocations.orEmpty()
            binding.rvSearchResults.visibility = if (matchedLocations.isEmpty()) View.GONE else View.VISIBLE
            binding.tvEmptySearch.visibility = if (matchedLocations.isEmpty()) View.VISIBLE else View.GONE
            searchAdapter.submitList(matchedLocations)
        }
        // 任何一种渲染后都要刷新底部已选状态
        refreshSelectedState()
    }

    /**
     * 渲染历史城市、热门城市和 A-Z 分组城市列表。
     */
    private fun renderNormalSections(data: JobCityData?) {
        // 每次重绘前清空旧节点
        binding.llSections.removeAllViews()
        sectionTitleViews.clear()
        // 历史城市和热门城市
        addSection("历史城市", data?.historyLocations.orEmpty())
        addSection("热门城市", data?.hotLocations.orEmpty())
        // A-Z 字母分组
        data?.groupedLocations.orEmpty().forEach { group ->
            addSection(group.letter.orEmpty(), group.locations.orEmpty())
        }
        // 过滤空字母并同步选中字母
        alphabetIndex = data?.alphabetIndex.orEmpty()
            .filter { it.isNotBlank() }
        if (selectedAlphabet !in alphabetIndex) selectedAlphabet = null
        refreshAlphabetIndexText()
        binding.tvAlphabetIndex.visibility = if (alphabetIndex.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * 添加一个城市分组标题和分组内的城市按钮。
     *
     * @param title 分组标题，包含历史城市、热门城市或字母。
     * @param locations 当前分组下的城市列表。
     */
    private fun addSection(title: String, locations: List<CityLocation>) {
        // 空标题或空数据不渲染
        if (title.isBlank() || locations.isEmpty()) return
        // 分组标题：加粗、垂直居中、首个分组无上间距
        binding.llSections.addView(TextView(this).apply {
            text = title
            setTextColor(ContextCompat.getColor(context, R.color.color_90_000000))
            textSize = 17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                24.dp
            ).apply {
                topMargin = if (binding.llSections.childCount == 0) 0 else 20.dp
            }
        })
        // 记录标题视图位置，供字母索引联动
        sectionTitleViews[title.uppercase()] = binding.llSections.getChildAt(binding.llSections.childCount - 1)
        // 城市按钮网格（三列）
        val grid = GridLayout(this).apply {
            columnCount = 3
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12.dp }
        }
        locations.forEach { item ->
            grid.addView(createLocationChip(item))
        }
        binding.llSections.addView(grid)
    }

    /**
     * 处理右侧 A-Z 索引的点击和滑动选择。
     */
    private fun handleAlphabetTouch(event: MotionEvent): Boolean {
        // 不可见或没有索引时不处理
        if (alphabetIndex.isEmpty() || binding.scrollNormal.visibility != View.VISIBLE) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // 按触摸 y 坐标换算到字母索引
                val letterHeight = binding.tvAlphabetIndex.height.toFloat() / alphabetIndex.size
                if (letterHeight <= 0f) return true
                val index = (event.y / letterHeight).toInt().coerceIn(0, alphabetIndex.lastIndex)
                selectAlphabet(alphabetIndex[index])
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> return true
        }
        return false
    }

    /**
     * 选中一个字母索引，并滚动左侧列表到对应分组。
     */
    private fun selectAlphabet(letter: String) {
        // 记录选中字母，刷新高亮，滚动到对应分组
        selectedAlphabet = letter
        refreshAlphabetIndexText()
        scrollToLetter(letter)
    }

    /**
     * 刷新右侧 A-Z 索引文本，高亮当前选中的字母。
     */
    private fun refreshAlphabetIndexText() {
        // 普通色 / 选中色 / 选中背景色
        val normalColor = ContextCompat.getColor(this, R.color.color_55_3c3c43)
        val selectedColor = ContextCompat.getColor(this, R.color.color_2f6bff)
        val selectedBackgroundColor = ContextCompat.getColor(this, R.color.color_E8F1FF)
        // 用 SpannableString 为每个字母独立上色
        val spannable = android.text.SpannableString(alphabetIndex.joinToString(separator = "\n"))
        var start = 0
        alphabetIndex.forEachIndexed { index, letter ->
            val end = start + letter.length
            val isSelected = letter == selectedAlphabet
            // 文本色：选中/普通
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(
                    if (isSelected) selectedColor else normalColor
                ),
                start,
                end,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // 选中字母追加背景色 + 加粗
            if (isSelected) {
                spannable.setSpan(
                    android.text.style.BackgroundColorSpan(selectedBackgroundColor),
                    start,
                    end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    start,
                    end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            // 换行符占 1 个字符
            start = end + if (index == alphabetIndex.lastIndex) 0 else 1
        }
        binding.tvAlphabetIndex.text = spannable
    }

    /**
     * 将左侧城市列表滚动到指定字母分组。
     */
    private fun scrollToLetter(letter: String) {
        // 找到对应分组标题的视图位置并平滑滚动
        val target = sectionTitleViews[letter.uppercase()] ?: return
        binding.scrollNormal.post {
            binding.scrollNormal.smoothScrollTo(0, binding.llSections.top + target.top)
        }
    }

    /**
     * 根据左侧列表滚动位置，同步右侧 A-Z 索引的选中态。
     */
    private fun syncAlphabetByScroll(scrollY: Int) {
        if (alphabetIndex.isEmpty() || binding.scrollNormal.visibility != View.VISIBLE) return
        // 转换成相对分组容器的 y 坐标
        val contentScrollY = scrollY - binding.llSections.top
        // 找到当前最靠近顶部且 top 已进入视区的字母
        val currentLetter = alphabetIndex
            .mapNotNull { letter -> sectionTitleViews[letter.uppercase()]?.let { letter to it.top } }
            .lastOrNull { (_, top) -> top <= contentScrollY + 8.dp }
            ?.first
            ?: alphabetIndex
                .firstOrNull { letter -> sectionTitleViews.containsKey(letter.uppercase()) }
        // 仅在变化时刷新，避免无谓的文本重建
        if (currentLetter != null && currentLetter != selectedAlphabet) {
            selectedAlphabet = currentLetter
            refreshAlphabetIndexText()
        }
    }

    /**
     * 创建普通城市列表中的城市按钮，并按选中状态切换样式。
     */
    private fun createLocationChip(item: CityLocation): TextView {
        val code = item.regionCode.orEmpty()
        val selected = code.isNotBlank() && selectedLocations.containsKey(code)
        return TextView(this).apply {
            // 文本：城市名（单行省略）
            text = item.regionName.orEmpty()
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            // 文本颜色按选中切换
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.color_2f6bff else R.color.color_90_000000
                )
            )
            textSize = 14f
            // 背景按选中切换
            background = ContextCompat.getDrawable(
                context,
                if (selected) R.drawable.bg_location_chip_selected else R.drawable.bg_location_chip
            )
            setPadding(8.dp, 0, 8.dp, 0)
            // 点击切换选中
            setOnSingleClickListener { toggleLocation(item) }
            // GridLayout 等分三列
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 36.dp
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(0, 0, 8.dp, 6.dp)
            }
        }
    }

    /**
     * 切换城市选中状态，并处理最大可选数量限制。
     */
    private fun toggleLocation(item: CityLocation) {
        val code = item.regionCode
        if (code.isNullOrBlank()) return
        if (selectedLocations.containsKey(code)) {
            // 已选 → 取消
            selectedLocations.remove(code)
        } else {
            // 未选 → 校验上限
            if (selectedLocations.size >= maxSelectCount) {
                getString(R.string.city_selector_max_select_tip, maxSelectCount).showToast()
                return
            }
            selectedLocations[code] = item
        }
        // 搜索态只刷新搜索结果，非搜索态重渲染分组
        if (binding.etSearch.text.isNullOrBlank()) {
            renderCityData()
        } else {
            searchAdapter.notifyDataSetChanged()
        }
        refreshSelectedState()
    }

    /**
     * 从底部已选城市中删除指定城市，并同步列表选中态。
     */
    private fun removeSelectedLocation(item: CityLocation) {
        val code = item.regionCode
        if (code.isNullOrBlank()) return
        if (selectedLocations.remove(code) == null) return
        refreshSelectedState()
        // 删除后回到非搜索态时重新拉一次以恢复分组按钮的选中样式
        if (binding.etSearch.text.isNullOrBlank()) {
            mRealVM.getJobCityList(mCityParams.apply { keyword = "" })
        } else {
            searchAdapter.notifyDataSetChanged()
        }
    }

    /**
     * 更新底部完成按钮状态。
     */
    private fun updateCompleteButtonState() {
        val hasSelection = selectedLocations.isNotEmpty()
        binding.btnComplete.isEnabled = hasSelection
        // 未选时半透明灰化
        binding.btnComplete.alpha = if (hasSelection) 1f else 0.3f
    }

    /**
     * 刷新底部已选城市区域，包括数量、上限和可删除城市标签。
     */
    private fun refreshSelectedState() {
        updateCompleteButtonState()
        // 当前已选数量 / 上限
        binding.tvCount.text = "${selectedLocations.size}"
        binding.tvMax.text = "/$maxSelectCount"
        selectedAdapter.submitList(selectedLocations.values.toList())
    }
}
