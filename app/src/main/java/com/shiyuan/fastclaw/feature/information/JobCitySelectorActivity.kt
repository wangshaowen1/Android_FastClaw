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
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.ext.getBundleValue
import com.shiyuan.fastclaw.core.network.apis.request.JobCityQueryBody
import com.shiyuan.fastclaw.core.network.apis.response.CityLocation
import com.shiyuan.fastclaw.core.network.apis.response.JobCityData
import com.shiyuan.fastclaw.databinding.ActivityJobCitySelectorBinding
import com.shiyuan.fastclaw.feature.information.viewHolder.LocationSearchAdapter
import com.shiyuan.fastclaw.feature.information.viewModel.JobCitySelectorModel

/**
 * @desc：工作城市选择
 * @author：CaoWei
 * @date：2026/6/10 16:52
 */
class JobCitySelectorActivity : BaseViewModelActivity<ActivityJobCitySelectorBinding, JobCitySelectorModel>() {

    private val selectedLocations = linkedMapOf<String, CityLocation>()
    private val searchAdapter = LocationSearchAdapter(selectedLocations) { toggleLocation(it) }
    private val searchHandler = Handler(Looper.getMainLooper())
    private var pendingSearch: Runnable? = null
    private var maxSelectCount = 3
    private val sectionTitleViews = linkedMapOf<String, View>()
    private var alphabetIndex = emptyList<String>()
    private var selectedAlphabet: String? = null
    private val mType by lazy { intent.getBundleValue("sceneCode", "") }
    private val mCityParams by lazy { JobCityQueryBody(sceneCode = mType) }
    private var mData: JobCityData? = null
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
        initObserve()
        initView()
    }

    override fun setTranslucentStatus() {
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
        if (mType == JobCityQueryBody.EXPECTED_CITY) {
            binding.tvSubtitle.text = getString(R.string.city_selector_add_multiple)
            maxSelectCount = 3
        } else {
            binding.tvSubtitle.text = getString(R.string.city_selector_now_city)
            maxSelectCount = 1
        }
        binding.ivClose.setOnSingleClickListener { finish() }
        binding.ivClearSearch.setOnSingleClickListener {
            binding.etSearch.setText("")
        }
        binding.btnComplete.setOnSingleClickListener {
            if (selectedLocations.isEmpty()) {
                showToast(R.string.city_selector_at_least)
            } else {
                val msgGson = JobCityData.toJson(mType, selectedLocations)
                FlowEventBus.postSync(Events.ClawAgentEvent(msgGson))
                finish()
            }
        }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                mCityParams.apply { keyword = binding.etSearch.text?.toString().orEmpty() }
                mRealVM.getJobCityList(mCityParams)
                true
            } else {
                false
            }
        }
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString().orEmpty()
                binding.ivClearSearch.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
                pendingSearch?.let(searchHandler::removeCallbacks)
                pendingSearch = Runnable {
                    mRealVM.getJobCityList(mCityParams.apply { keyword = text })
                }.also {
                    searchHandler.postDelayed(it, mSearchDelayTime)
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        binding.tvAlphabetIndex.setOnTouchListener { _, event ->
            handleAlphabetTouch(event)
        }
        binding.scrollNormal.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            syncAlphabetByScroll(scrollY)
        }
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapter
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
        mData?.selectedLocations.orEmpty().forEach { item ->
            item.regionCode?.let { selectedLocations[it] = item }
        }
        if (binding.etSearch.text.isBlank()) {
            binding.scrollNormal.visibility = View.VISIBLE
            binding.tvAlphabetIndex.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
            binding.tvEmptySearch.visibility = View.GONE
            renderNormalSections(mData)
        } else {
            binding.scrollNormal.visibility = View.GONE
            binding.tvAlphabetIndex.visibility = View.GONE
            val matchedLocations = mData?.matchedLocations.orEmpty()
            binding.rvSearchResults.visibility = if (matchedLocations.isEmpty()) View.GONE else View.VISIBLE
            binding.tvEmptySearch.visibility = if (matchedLocations.isEmpty()) View.VISIBLE else View.GONE
            searchAdapter.setKeyWord(binding.etSearch.text?.toString().orEmpty())
            searchAdapter.submitList(matchedLocations,)
        }
        refreshSelectedState()
    }

    /**
     * 渲染历史城市、热门城市和 A-Z 分组城市列表。
     */
    private fun renderNormalSections(data: JobCityData?) {
        binding.llSections.removeAllViews()
        sectionTitleViews.clear()
        addSection("历史城市", data?.historyLocations.orEmpty())
        addSection("热门城市", data?.hotLocations.orEmpty())
        data?.groupedLocations.orEmpty().forEach { group ->
            addSection(group.letter.orEmpty(), group.locations.orEmpty())
        }
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
        if (title.isBlank() || locations.isEmpty()) return
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
        sectionTitleViews[title.uppercase()] = binding.llSections.getChildAt(binding.llSections.childCount - 1)
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
        if (alphabetIndex.isEmpty() || binding.scrollNormal.visibility != View.VISIBLE) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
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
        selectedAlphabet = letter
        refreshAlphabetIndexText()
        scrollToLetter(letter)
    }

    /**
     * 刷新右侧 A-Z 索引文本，高亮当前选中的字母。
     */
    private fun refreshAlphabetIndexText() {
        val normalColor = ContextCompat.getColor(this, R.color.color_55_3c3c43)
        val selectedColor = ContextCompat.getColor(this, R.color.color_2f6bff)
        val selectedBackgroundColor = ContextCompat.getColor(this, R.color.color_E8F1FF)
        val spannable = android.text.SpannableString(alphabetIndex.joinToString(separator = "\n"))
        var start = 0
        alphabetIndex.forEachIndexed { index, letter ->
            val end = start + letter.length
            val isSelected = letter == selectedAlphabet
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(
                    if (isSelected) selectedColor else normalColor
                ),
                start,
                end,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
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
            start = end + if (index == alphabetIndex.lastIndex) 0 else 1
        }
        binding.tvAlphabetIndex.text = spannable
    }

    /**
     * 将左侧城市列表滚动到指定字母分组。
     */
    private fun scrollToLetter(letter: String) {
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
        val contentScrollY = scrollY - binding.llSections.top
        val currentLetter = alphabetIndex
            .mapNotNull { letter -> sectionTitleViews[letter.uppercase()]?.let { letter to it.top } }
            .lastOrNull { (_, top) -> top <= contentScrollY + 8.dp }
            ?.first
            ?: alphabetIndex
                .firstOrNull { letter -> sectionTitleViews.containsKey(letter.uppercase()) }
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
            text = item.regionName.orEmpty()
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.color_2f6bff else R.color.color_90_000000
                )
            )
            textSize = 14f
            background = ContextCompat.getDrawable(
                context,
                if (selected) R.drawable.bg_location_chip_selected else R.drawable.bg_location_chip
            )
            setPadding(8.dp, 0, 8.dp, 0)
            setOnSingleClickListener { toggleLocation(item) }
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
            selectedLocations.remove(code)
        } else {
            if (selectedLocations.size >= maxSelectCount) {
                getString(R.string.city_selector_max_select_tip, maxSelectCount).showToast()
                return
            }
            selectedLocations[code] = item
        }
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
        if (binding.etSearch.text.isNullOrBlank()) {
            mRealVM.getJobCityList(mCityParams.apply { keyword = "" })
        } else {
            searchAdapter.notifyDataSetChanged()
        }
    }

    /**
     * 更新底部完成按钮状态
     */
    private fun updateCompleteButtonState() {
        val hasSelection = selectedLocations.isNotEmpty()
        binding.btnComplete.isEnabled = hasSelection
        binding.btnComplete.alpha = if (hasSelection) 1f else 0.3f
    }

    /**
     * 刷新底部已选城市区域，包括数量、上限和可删除城市标签。
     */
    private fun refreshSelectedState() {
        updateCompleteButtonState()
        binding.tvCount.text = "${selectedLocations.size}"
        binding.tvMax.text = "/$maxSelectCount"
        binding.llSelectedChips.removeAllViews()
        selectedLocations.values.forEach { item ->
            binding.llSelectedChips.addView(LinearLayout(this).apply {
                gravity = Gravity.CENTER
                orientation = LinearLayout.HORIZONTAL
                background = ContextCompat.getDrawable(context, R.drawable.bg_location_selected_tag)
                setPadding(8.dp, 0, 6.dp, 0)
                isClickable = true
                setOnSingleClickListener { removeSelectedLocation(item) }
                layoutParams = LinearLayout.LayoutParams(60.dp, 22.dp).apply {
                    marginEnd = 8.dp
                }
                addView(TextView(context).apply {
                    text = item.regionName.orEmpty()
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setTextColor(ContextCompat.getColor(context, R.color.color_2f6bff))
                    textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                })
                addView(TextView(context).apply {
                    text = "×"
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.color_2f6bff))
                    textSize = 12f
                    setOnSingleClickListener { removeSelectedLocation(item) }
                    layoutParams = LinearLayout.LayoutParams(10.dp, ViewGroup.LayoutParams.MATCH_PARENT)
                })
            })
        }
    }
}
