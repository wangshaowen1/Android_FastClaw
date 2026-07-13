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


class JobPreferencesFragment :
    BaseViewModelFragment<FragmentJobPreferencesBinding, InformationViewModel>() {

    companion object {
        fun newInstance(): JobPreferencesFragment = JobPreferencesFragment()

        private const val CODE_JOB_STATUS = "jobStatus"
        private const val CODE_EXPECTED_CITY = "expectedCity"

        private const val INPUT_MODE_DICT_SINGLE = "DICT_SINGLE"
        private const val INPUT_MODE_DICT_MULTI = "DICT_MULTI"

        private const val CODE_JOB_INTENTION = "jobIntention"

        private const val CODE_JOB_EXPECTED_SALARY = "expectedSalary"

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
        private const val EVENT_EXPECTED_INDUSTRY_COMPLETED = "expectedIndustryCompleted"

        private const val CODE_EXPECTED_INDUSTRY = "expectedIndustry"
    }

    private val preferenceAdapter by lazy {
        SingleBindingAdapter<ItemJobInfoBinding, OnlineResumeInfoSection>(
            R.layout.item_job_info,
            emptyList(),
            onBind = { itemBinding, _, item ->
                bindJobInfoItem(itemBinding, item)
            }
        )
    }


    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initObservers()
        mRealVM.queryJobPreferencesInfo()
    }

    private fun initView() {
        binding.rvJobPreferences.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            isNestedScrollingEnabled = false
            adapter = preferenceAdapter
        }
        updateJobPreferences(emptyList())
    }

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

    private fun updateJobPreferences(data: List<OnlineResumeInfoSection>) {
        val showEmpty = data.isEmpty()
        binding.viewUnderstands.root.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.ll.visibility = if (showEmpty) View.GONE else View.VISIBLE
        preferenceAdapter.submitList(data)
    }

    private fun bindJobInfoItem(
        itemBinding: ItemJobInfoBinding,
        item: OnlineResumeInfoSection
    ) {
        itemBinding.tvTitle.text = item.sectionName ?: ""
        if (item.sectionCode == CODE_JOB_STATUS) {
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
            itemBinding.ll.setOnSingleClickListener {
                singleSelectDialog(item.fields.firstOrNull(), "求职状态"){ code ->
                    FlowEventBus.postSticky(Events.JobSearchStatusEvent(code))
                }
            }
        } else {
            itemBinding.ll.setBackgroundResource(R.color.white)
            itemBinding.ivEdit.visibility = View.VISIBLE
            itemBinding.ivEdit.visibility = View.INVISIBLE
            itemBinding.tvName.visibility = View.GONE
            itemBinding.layoutMissing.visibility = View.INVISIBLE
            bindFieldTreeList(itemBinding, item.fields)
        }
    }

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

    private fun bindJobInfoReItem(
        itemBinding: ItemJobInfoReBinding,
        item: AttributeArrayQueryData
    ) {
        itemBinding.layoutMissing.visibility = if (item.displayValue.isNullOrEmpty()) View.VISIBLE else View.GONE
        itemBinding.tvVale.visibility = if (item.displayValue.isNullOrEmpty()) View.GONE else View.VISIBLE
        itemBinding.tvName.text = item.attributeGroupName ?: ""
        itemBinding.tvVale.text = item.displayValue
        itemBinding.ll.setOnSingleClickListener {
            when (item.attributeGroupCode) {
                CODE_EXPECTED_CITY -> {
                    JobCitySelectorActivity.start(activity, JobCityQueryBody.EXPECTED_CITY)
                }

                CODE_JOB_INTENTION -> {
                    JobSelectionActivity.start(activity, true,multipleNum = 10)
                }
                CODE_EXPECTED_INDUSTRY -> {
                    IndustrySelectionActivity.start(
                        activity,
                        isMultiple = true,
                        resultEventType = EVENT_EXPECTED_INDUSTRY_COMPLETED
                    )
                }
                CODE_JOB_EXPECTED_SALARY -> {
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
                CODE_JOB_WELFAREPREFERENCE ->{
                    JobWelfareSelectorActivity.start(activity)
                }
            }

        }

    }
}
