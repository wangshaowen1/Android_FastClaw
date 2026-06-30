package com.shiyuan.fastclaw.feature.mine.fragment

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfActivity
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.EmptyViewModel
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.FieldItems
import com.shiyuan.fastclaw.databinding.FragmentJobPreferencesBinding
import com.shiyuan.fastclaw.databinding.ItemJobPreference1Binding
import com.shiyuan.fastclaw.databinding.ItemJobPreference1SonBinding
import com.shiyuan.fastclaw.feature.mine.viewModel.UnderstandsViewModel
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration

class JobPreferencesFragment :
    BaseViewModelFragment<FragmentJobPreferencesBinding, EmptyViewModel>() {

    companion object {
        fun newInstance(): JobPreferencesFragment = JobPreferencesFragment()
    }

    private val parentVm by lazy {
        getViewModelOfActivity<UnderstandsViewModel>()
    }
    private val preferenceAdapter by lazy {
        SingleBindingAdapter<ItemJobPreference1Binding, FieldItems>(
            R.layout.item_job_preference1,
            emptyList(),
            onBind = { itemBinding, _, item ->
                itemBinding.tvTile.text = item.fieldName ?: ""
                when {
                    item.fieldItems.size > 1 -> bindTagList(itemBinding, item.fieldItems)
                    item.fieldItems.size == 1 -> {
                        itemBinding.rcConnect.visibility = View.GONE
                        itemBinding.tvConnect.visibility = View.VISIBLE
                        itemBinding.tvConnect.text = item.fieldItems[0].fieldValue?.toString() ?: ""
                    }

                    else -> {
                        itemBinding.rcConnect.visibility = View.GONE
                        itemBinding.tvConnect.visibility = View.VISIBLE
                        itemBinding.tvConnect.text = item.fieldValue?.toString() ?: ""
                    }
                }
            }
        )
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initObservers()
    }

    private fun initView() {
        binding.rvJobPreferences.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            isNestedScrollingEnabled = false
            addItemDecoration(SpaceDecoration(16.dp, SpaceDecoration.Direction.MIDDLE))
            adapter = preferenceAdapter
        }
    }

    private fun initObservers() {
        parentVm?.jobPreference?.observe(this) { data ->
            updateJobPreferences(data.orEmpty())
        }
    }

    private fun updateJobPreferences(data: List<FieldItems>) {
        val showEmpty = data.isEmpty()
        binding.viewUnderstands.root.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.rvJobPreferences.visibility = if (showEmpty) View.GONE else View.VISIBLE
        preferenceAdapter.submitList(data)
    }

    private fun bindTagList(
        itemBinding: ItemJobPreference1Binding,
        fieldItems: List<FieldItems>
    ) {
        itemBinding.tvConnect.visibility = View.GONE
        itemBinding.rcConnect.visibility = View.VISIBLE
        val subAdapter = SingleBindingAdapter<ItemJobPreference1SonBinding, FieldItems>(
            R.layout.item_job_preference1_son,
            fieldItems,
            onBind = { tagBinding, _, sub ->
                tagBinding.tvTag.text = sub.fieldValue?.toString() ?: ""
                val maxTagWidth = itemBinding.rcConnect.measuredWidth
                    .takeIf { it > 0 }
                    ?: (resources.displayMetrics.widthPixels - 64.dp)
                tagBinding.tvTag.maxWidth = maxTagWidth.coerceAtLeast(0)
                (tagBinding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                    it.marginEnd = 10.dp
                    it.bottomMargin = 10.dp
                    tagBinding.root.layoutParams = it
                }
            }
        )
        itemBinding.rcConnect.apply {
            val flexLayoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
                justifyContent = JustifyContent.FLEX_START
                alignItems = AlignItems.FLEX_START
            }
            layoutManager = flexLayoutManager
            isNestedScrollingEnabled = false
            adapter = subAdapter
            post { requestLayout() }
        }
    }
}