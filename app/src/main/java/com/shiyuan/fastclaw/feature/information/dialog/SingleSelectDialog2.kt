package com.shiyuan.fastclaw.feature.information.dialog

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.BaseBottomDialogFragment
import com.shiyuan.fastclaw.base.BaseWheelSoundBottomDialogFragment
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.DialogSingSelectStatusList2Binding
import com.shiyuan.fastclaw.databinding.ItemSingSelectStatusList2Binding

class SingleSelectDialog2 : BaseWheelSoundBottomDialogFragment<DialogSingSelectStatusList2Binding>() {

    companion object {
        const val TAG = "SingleSelectDialog2"
        private const val ARG_TITLE = "title"

        fun show(
            manager: FragmentManager,
            title: String? = null,
            items: List<CommonEnumItem>,
            selectedItem: CommonEnumItem? = null,
            onSelected: ((CommonEnumItem) -> Unit)? = null,
        ): SingleSelectDialog2 {
            return SingleSelectDialog2().apply {
                this.items = items
                this.selectedItem = selectedItem
                this.onSelected = onSelected
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                }
                show(manager)
            }
        }
    }

    private var items: List<CommonEnumItem> = emptyList()
    private var selectedItem: CommonEnumItem? = null
    private var onSelected: ((CommonEnumItem) -> Unit)? = null
    private val optionAdapter: SingleBindingAdapter<ItemSingSelectStatusList2Binding, CommonEnumItem> by lazy {
        SingleBindingAdapter<ItemSingSelectStatusList2Binding, CommonEnumItem>(
            R.layout.item_sing_select_status_list2,
            emptyList()
        ) { itemBinding, position, item ->
            val selected = isSelected(item)
            itemBinding.tvName.text = item.name.orEmpty()
            itemBinding.tvName.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.color_2f6bff else R.color.color_96_000000
                )
            )
            itemBinding.ivSelected.visibility = if (selected) View.VISIBLE else View.GONE
            itemBinding.root.setOnSingleClickListener {
                onSelected?.invoke(item)
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        if (items.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }
        binding.tvTitle.text = arguments?.getString(ARG_TITLE).orEmpty()
        bindOptions()
    }

    private fun bindOptions() {
        binding.layoutOptions.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.layoutOptions.adapter = optionAdapter
        optionAdapter.submitList(items)
    }

    private fun isSelected(item: CommonEnumItem): Boolean {
        val selectedCode = selectedItem?.code
        val selectedName = selectedItem?.name
        return when {
        /*    !selectedCode.isNullOrBlank() -> item.code == selectedCode*/
            !selectedName.isNullOrBlank() -> item.name == selectedName
            else -> item.isSelected
        }
    }
}
