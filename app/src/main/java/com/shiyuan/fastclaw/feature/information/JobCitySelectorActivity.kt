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
import com.shiyuan.fastclaw.core.network.apis.response.SimpleLocation
import com.shiyuan.fastclaw.databinding.ActivityJobCitySelectorBinding
import com.shiyuan.fastclaw.feature.information.viewHolder.LocationSearchAdapter
import com.shiyuan.fastclaw.feature.information.viewModel.JobCitySelectorModel
import kotlinx.serialization.json.Json

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
    private val mType by lazy { intent.getBundleValue<String>("sceneCode", "") }
    private val mCityParams by lazy { JobCityQueryBody(sceneCode = mType) }
    private var mData: JobCityData? = null

    companion object {
        const val EXTRA_SELECTED_REGION_CODES = "extra_selected_region_codes"
        const val EXTRA_SELECTED_REGION_NAMES = "extra_selected_region_names"
        private const val SEARCH_DELAY_MS = 300L

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

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        binding.tvSubtitle.text = if (mType == JobCityQueryBody.EXPECTED_CITY) {
            getString(R.string.city_selector_add_multiple)
        } else {
            getString(R.string.city_selector_now_city)
        }
        binding.ivClose.setOnSingleClickListener { finish() }
        binding.ivClearSearch.setOnSingleClickListener {
            binding.etSearch.setText("")
        }
        binding.btnComplete.setOnSingleClickListener {
            if (selectedLocations.isEmpty()) {
                showToast(R.string.city_selector_at_least)
            } else {
                val msgGson = JobCityData.toJson(selectedLocations)
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
                    searchHandler.postDelayed(it, SEARCH_DELAY_MS)
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

    private fun initObserve() {
        mRealVM.jobCityList.observe(this) {
            it?.let {
                mData = it
                renderCityData()
            }
        }
    }

    private fun renderCityData() {
        maxSelectCount = mData?.maxSelectCount ?: maxSelectCount
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
            searchAdapter.submitList(matchedLocations)
        }
        refreshSelectedState()
    }

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

    private fun selectAlphabet(letter: String) {
        selectedAlphabet = letter
        refreshAlphabetIndexText()
        scrollToLetter(letter)
    }

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

    private fun scrollToLetter(letter: String) {
        val target = sectionTitleViews[letter.uppercase()] ?: return
        binding.scrollNormal.post {
            binding.scrollNormal.smoothScrollTo(0, binding.llSections.top + target.top)
        }
    }

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

    private fun refreshSelectedState() {
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