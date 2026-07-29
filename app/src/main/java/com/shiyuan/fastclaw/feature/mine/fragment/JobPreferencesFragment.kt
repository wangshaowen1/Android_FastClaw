package com.shiyuan.fastclaw.feature.mine.fragment

import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.EmptyViewModel
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.common.utils.JsonUtil
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.network.apis.request.JobCityQueryBody
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldTree
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.core.network.apis.response.InterviewJobData
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeInfoSection
import com.shiyuan.fastclaw.core.network.apis.response.SalaryRangePayload
import com.shiyuan.fastclaw.databinding.FragmentJobPreferencesBinding
import com.shiyuan.fastclaw.databinding.ItemJobInfoBinding
import com.shiyuan.fastclaw.databinding.ItemJobInfoReBinding
import com.shiyuan.fastclaw.feature.information.IndustrySelectionActivity
import com.shiyuan.fastclaw.feature.information.JobCitySelectorActivity
import com.shiyuan.fastclaw.feature.information.JobSelectionActivity
import com.shiyuan.fastclaw.feature.information.JobWelfareSelectorActivity
import com.shiyuan.fastclaw.feature.information.dialog.SalaryRangePickerDialog
import com.shiyuan.fastclaw.feature.information.dialog.SingleSelectDialog2
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import com.shiyuan.fastclaw.feature.mine.fragment.ResumeInfoFragment.Companion.INCOMPLETE
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import org.json.JSONObject
import javax.annotation.meta.When
import kotlin.collections.map
import kotlin.collections.orEmpty


/**
 * “求职偏好”页面。
 *
 * 页面数据由后端按 [OnlineResumeInfoSection] 分组返回：
 * - 求职状态使用顶部独立样式，并在当前页面通过单选弹窗修改；
 * - 其余分组以字段列表展示，点击字段后再根据 attributeGroupCode 进入对应编辑器。
 *
 * 城市、职位、行业和福利等独立选择页通过事件回传结果；当前页面负责把结果提交到在线简历，
 * 提交成功后统一发送刷新事件，重新拉取整页数据，避免局部 UI 与服务端状态不一致。
 */
class JobPreferencesFragment :
    BaseViewModelFragment<FragmentJobPreferencesBinding, InformationViewModel>() {

    companion object {
        fun newInstance(): JobPreferencesFragment = JobPreferencesFragment()

        // 后端下发的字段组编码，也是本页选择具体编辑方式的依据。
        private const val CODE_JOB_STATUS = "jobStatus"
        private const val CODE_EXPECTED_CITY = "expectedCity"

        // 更新在线简历属性时使用的输入模式，必须与服务端字段定义保持一致。
        private const val INPUT_MODE_DICT_SINGLE = "DICT_SINGLE"
        private const val INPUT_MODE_DICT_MULTI = "DICT_MULTI"

        private const val CODE_JOB_INTENTION = "jobIntention"

        private const val CODE_JOB_EXPECTED_SALARY = "expectedSalary"

        // 以下字段均采用通用单选弹窗编辑。
        private const val CODE_JOB_TYPE = "jobType"

        private const val CODE_JOB_LATESTLEAVING_REASON = "latestLeavingReason"
        private const val CODE_JOB_CAREERDIRECTION = "careerDirection"
        private const val CODE_JOB_WORKMODE = "workMode"
        private const val CODE_JOB_ACCEPTRELOCATION = "acceptRelocation"
        private const val CODE_JOB_WORKSCHEDULEPREFERENCE = "workSchedulePreference"
        private const val CODE_JOB_OVERTIMEACCEPTANCE = "overtimeAcceptance"
        private const val CODE_JOB_COMPANYSCALEPREFERENCE = "companyScalePreference"
        private const val CODE_JOB_SALARYSTRUCTUREPREFERENCE = "salaryStructurePreference"
        private const val CODE_JOB_TEAMATMOSPHEREPREFERENCE = "teamAtmospherePreference"
        private const val CODE_JOB_WELFAREPREFERENCE = "welfarePreference"

        // 行业选择页允许由调用方指定回传事件，因此在本页单独定义事件名。
        private const val EVENT_EXPECTED_INDUSTRY_COMPLETED = "expectedIndustryCompleted"

        private const val CODE_EXPECTED_INDUSTRY = "expectedIndustry"

        private const val CODE_JOB_IDENTITY = "jobIdentity"
    }

    /**
     * 页面外层分组列表。
     *
     * 每个 item 对应一个求职偏好分组；具体字段仍由分组内部的 RecyclerView 展示，
     * 因此这里仅负责把分组数据交给 [bindJobInfoItem]。
     */
    private val preferenceAdapter by lazy {
        SingleBindingAdapter<ItemJobInfoBinding, OnlineResumeInfoSection>(
            R.layout.item_job_info,
            emptyList(),
            onBind = { itemBinding, position, item ->
                bindJobInfoItem(itemBinding,position, item)
            }
        )
    }


    override fun onPrepareView() {
        super.onPrepareView()
        // 先建立列表和观察关系，再发起查询，确保首批数据不会错过 LiveData 回调。
        initView()
        initObservers()
        mRealVM.queryJobPreferencesInfo()
    }

    /**
     * 初始化外层分组列表。
     *
     * Fragment 所在页面负责整体滚动，所以关闭 RecyclerView 自身的嵌套滚动。
     * 请求返回前先展示空态，后续由 [updateJobPreferences] 根据真实数据切换。
     */
    private fun initView() {
        binding.rvJobPreferences.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            isNestedScrollingEnabled = false
            adapter = preferenceAdapter
        }
        updateJobPreferences(emptyList())
    }

    /**
     * 监听页面数据和各选择页的结果事件。
     *
     * [Events.CommonAttributeArrayQueryData] 表示某项在线简历属性已更新，本页只需重新查询；
     * 带选择结果的事件则先解析并提交，提交成功后再触发统一刷新。
     */
    private fun initObservers() {
        mRealVM.jobPreferencesInfo.observe(this) { data ->
            updateJobPreferences(data.orEmpty())
        }
        observeEvents {
            on<Events.CommonAttributeArrayQueryData> { event ->
                mRealVM.queryJobPreferencesInfo()
            }

            on<Events.ClawAgentEvent> { event ->
                when (event.type) {
                    AppEvent.KLX_C_EXPECTED_JOB_COMPLETED -> {
                        handleSelected(event.msgGson, CODE_JOB_INTENTION, INPUT_MODE_DICT_MULTI)
                    }

                    AppEvent.KLX_C_EXPECTED_CITY_COMPLETED -> {
                        handleSelected(event.msgGson, CODE_EXPECTED_CITY, INPUT_MODE_DICT_MULTI)
                    }
                    EVENT_EXPECTED_INDUSTRY_COMPLETED -> {
                        handleSelected(event.msgGson, CODE_EXPECTED_INDUSTRY, INPUT_MODE_DICT_MULTI)
                    }
                    else ->{
                        handleSelected(event.msgGson, CODE_JOB_WELFAREPREFERENCE, INPUT_MODE_DICT_MULTI)
                    }
                }

            }
        }

    }

    /**
     * 处理独立选择页返回的多选结果。
     *
     * 事件中的 JSON 按 [InterviewJobData] 列表解析，并以调用方指定的字段组编码提交。
     * 更新成功后发送公共刷新事件，由观察者重新请求完整的求职偏好数据。
     */
    private fun handleSelected(msgGson: String?, attributeGroupCode: String, inputMode: String) {
        Log.d("handleLocationCitySelected", msgGson.toString())
        val dataList = JsonUtil.parseList(msgGson.toString(), InterviewJobData::class.java)
        mRealVM.updateOnlineResumeAttribute(
            attributeGroupCode = attributeGroupCode,
            inputMode = inputMode,
            value = dataList
        ) {
            FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
        }
    }

    /**
     * 同步空态、内容区和外层列表数据。
     *
     * 空态与内容区互斥展示，避免无数据时仍保留空白列表容器。
     */
    private fun updateJobPreferences(data: List<OnlineResumeInfoSection>) {
        val showEmpty = data.isEmpty()
        binding.viewUnderstands.root.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.ll.visibility = if (showEmpty) View.GONE else View.VISIBLE
        preferenceAdapter.submitList(data)
    }

    /**
     * 绑定一个求职偏好分组。
     *
     * 求职状态是顶部特殊卡片，直接展示当前状态并响应整卡点击；
     * 其他分组隐藏顶部值区域，改由内层字段列表展示和分发编辑入口。
     */
    private fun bindJobInfoItem(
        itemBinding: ItemJobInfoBinding,
        position: Int,
        item: OnlineResumeInfoSection
    ) {
        itemBinding.tvTitle.text = item.sectionName ?: ""
        if (item.sectionCode == CODE_JOB_STATUS) {
            // 求职状态使用独立卡片样式，并补足顶部间距以匹配页面头部视觉。
            itemBinding.ll.setBackgroundResource(R.drawable.bg_f3f9ff_ffffff_r12)
            val paddingTopPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                15f,
                itemBinding.ll.getResources().getDisplayMetrics()
            ).toInt()
            itemBinding.ll.setPadding(
                itemBinding.ll.getPaddingLeft(),
                paddingTopPx,
                itemBinding.ll.getPaddingRight(),
                itemBinding.ll.getPaddingBottom()
            );
            itemBinding.ivEdit.visibility = View.VISIBLE
            itemBinding.viewIndicator.visibility = View.GONE
            itemBinding.tvName.visibility = if (item.fields.firstOrNull()?.passable == true) View.VISIBLE else View.GONE
            itemBinding.tvName.text = item.fields.firstOrNull()?.displayValue.orEmpty()
            itemBinding.layoutMissing.visibility = if (item.fields.firstOrNull()?.passable == true) View.INVISIBLE  else View.VISIBLE
            itemBinding.rcFieldTree.visibility = View.GONE
            itemBinding.rcFieldTree.adapter = null
            // 求职状态只有一个字段，直接复用通用单选弹窗；成功后额外通知状态相关页面。
            itemBinding.ll.setOnSingleClickListener {
                singleSelectDialog(item.fields.firstOrNull(), "求职状态"){ code ->
                    FlowEventBus.postSticky(Events.JobSearchStatusEvent(code))
                }
            }
        } else {
            // 普通分组的具体内容由内层字段列表承载，外层只保留分组标题。
            itemBinding.ll.setBackgroundResource(R.color.white)
            itemBinding.ivEdit.visibility = View.VISIBLE
            itemBinding.ivEdit.visibility = View.INVISIBLE
            itemBinding.tvName.visibility = View.GONE
            itemBinding.layoutMissing.visibility = View.INVISIBLE
            bindFieldTreeList(itemBinding, item.fields)
        }
        itemBinding.viewDivider.visibility = if (position == preferenceAdapter.itemCount - 1) View.GONE else View.VISIBLE
    }

    /**
     * 展示字典型字段的单选弹窗并提交选中值。
     *
     * 弹窗选项来自接口下发的 options，当前值从 value 中的 code 恢复；
     * 若服务端未下发选项则直接提示，避免展示不可操作的空弹窗。
     *
     * @param onCallback 属性更新成功后的附加回调，求职状态用它继续分发状态变更事件。
     */
    private fun singleSelectDialog(item: AttributeArrayQueryData?, title: String,  onCallback: ((code: String) -> Unit)? = null) {
        item?.let { item ->
            val enumItems = item.options.orEmpty()
                .map { CommonEnumItem(code = it.code, name = it.name) }
            if (enumItems.isEmpty()) {
                "暂无选项".showToast()
                return@let
            }
           val selectedCode = (item.value as? Map<*, *>)?.get("code")?.toString()
           val selectedEnumItem =enumItems.firstOrNull { enumItem -> (enumItem.code == selectedCode) }
            SingleSelectDialog2.show(
                manager = childFragmentManager,
                title = title,
                items = enumItems,
                selectedItem = selectedEnumItem
            ) { data ->
                // 服务端字典字段要求同时提交 code 和 name，而不是只提交展示文本。
                val value = mapOf("code" to data.code, "name" to data.name)
                mRealVM.updateOnlineResumeAttribute(
                    attributeGroupCode = item.attributeGroupCode ?: item.metaCode,
                    inputMode = item.inputMode,
                    value = value
                ) {
                    FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
                    onCallback?.invoke(data.code ?: "")
                }
            }
        }
    }


    /**
     * 绑定普通分组下的字段列表。
     *
     * 内层列表同样关闭嵌套滚动，由外层页面统一滚动；间距装饰只添加一次，
     * 防止 RecyclerView 复用并重复绑定时叠加 decoration，导致字段间距越来越大。
     */
    private fun bindFieldTreeList(
        itemBinding: ItemJobInfoBinding,
        fieldTree: List<AttributeArrayQueryData>?
    ) {
        itemBinding.rcFieldTree.visibility = View.VISIBLE
        itemBinding.rcFieldTree.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        itemBinding.rcFieldTree.isNestedScrollingEnabled = false
        if (itemBinding.rcFieldTree.itemDecorationCount == 0) {
            itemBinding.rcFieldTree.addItemDecoration(SpaceDecoration(12.dp))
        }
        itemBinding.rcFieldTree.adapter = createFieldTreeAdapter(fieldTree.orEmpty())
    }

    /**
     * 为当前分组创建字段适配器。
     *
     * 每个分组的数据量和生命周期都跟随外层 item，因此适配器在绑定分组时按当前字段集合创建。
     */
    private fun createFieldTreeAdapter(
        fieldTree: List<AttributeArrayQueryData>
    ): SingleBindingAdapter<ItemJobInfoReBinding, AttributeArrayQueryData> {
        return SingleBindingAdapter(
            R.layout.item_job_info_re,
            fieldTree,
            onBind = { itemBinding, _, item ->
                bindJobInfoReItem(itemBinding, item)
            }
        )
    }

    /**
     * 绑定单个求职偏好字段，并根据字段组编码分发到对应编辑入口。
     *
     * passable 控制“待完善”提示，displayValue 为空时不占用值文本空间；
     * 点击后，复杂多选字段进入独立页面，薪资使用区间选择器，其余字典字段复用单选弹窗。
     */
    private fun bindJobInfoReItem(
        itemBinding: ItemJobInfoReBinding,
        item: AttributeArrayQueryData
    ) {
        itemBinding.layoutMissing.visibility = if (item.passable == true) View.INVISIBLE  else View.VISIBLE
        itemBinding.tvVale.visibility = if (item.displayValue.isNullOrEmpty()) View.GONE else View.VISIBLE
        itemBinding.tvName.text = item.attributeGroupName ?: ""
        itemBinding.tvVale.text = item.displayValue
        itemBinding.ll.setOnSingleClickListener {
            when (item.attributeGroupCode) {
                CODE_EXPECTED_CITY -> {
                    // 城市选择页需要区分“期望城市”场景，并回显服务端当前值。
                    JobCitySelectorActivity.start(
                        activity,
                        JobCityQueryBody.EXPECTED_CITY,
                        selectedValueJson = item.value?.let(JsonUtil::toJson),
                    )
                }

                CODE_JOB_INTENTION -> {
                    // 期望职位最多可选择 10 项，结果通过统一职位完成事件回传。
                    JobSelectionActivity.start(
                        activity,
                        isMultiple = true,
                        multipleNum = 10,
                        selectedValueJson = item.value?.let(JsonUtil::toJson),
                    )
                }
                CODE_EXPECTED_INDUSTRY -> {
                    // 行业选择使用本页专属事件名，避免与其他入口的行业选择结果混用。
                    IndustrySelectionActivity.start(
                        activity,
                        isMultiple = true,
                        resultEventType = EVENT_EXPECTED_INDUSTRY_COMPLETED,
                        selectedValueJson = item.value?.let(JsonUtil::toJson),
                    )
                }
                CODE_JOB_EXPECTED_SALARY -> {
                    // 薪资选择器返回上下限，先转换为接口约定的 payload 再更新字段。
                    SalaryRangePickerDialog.show(
                        manager = childFragmentManager
                    ) { minSalary, maxSalary ->
                        val salaryPayloadJson = SalaryRangePayload.fromPicker(minSalary, maxSalary)
                        mRealVM.updateOnlineResumeAttribute(
                            attributeGroupCode = item.attributeGroupCode ?: item.metaCode,
                            inputMode = item.inputMode,
                            value = salaryPayloadJson
                        ) {
                            FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
                        }
                    }
                }
                CODE_JOB_TYPE ->{
                    singleSelectDialog(item, "工作类型")
                }
                CODE_JOB_LATESTLEAVING_REASON ->{
                    singleSelectDialog(item, "离职原因")
                }
                CODE_JOB_CAREERDIRECTION ->{
                    singleSelectDialog(item, "职业方向")
                }
                CODE_JOB_WORKMODE ->{
                    singleSelectDialog(item, "办公模式")
                }
                CODE_JOB_ACCEPTRELOCATION ->{
                    singleSelectDialog(item, "是否接受外派")
                }
                CODE_JOB_WORKSCHEDULEPREFERENCE ->{
                    singleSelectDialog(item, "工作制度偏好")
                }
                CODE_JOB_OVERTIMEACCEPTANCE ->{
                    singleSelectDialog(item, "是否接受加班")
                }
                CODE_JOB_COMPANYSCALEPREFERENCE ->{
                    singleSelectDialog(item, "公司规模偏好")
                }
                CODE_JOB_SALARYSTRUCTUREPREFERENCE ->{
                    singleSelectDialog(item, "薪资结构偏好")
                }
                CODE_JOB_TEAMATMOSPHEREPREFERENCE ->{
                    singleSelectDialog(item, "团队氛围偏好")
                }
                CODE_JOB_IDENTITY ->{
                    singleSelectDialog(item, "求职身份")
                }
                CODE_JOB_WELFAREPREFERENCE ->{
                    // 福利偏好是多选字段，由专用页面负责选择和回显。
                    JobWelfareSelectorActivity.start(
                        activity,
                        selectedValueJson = item.value?.let(JsonUtil::toJson),
                    )
                }
            }

        }

    }
}
