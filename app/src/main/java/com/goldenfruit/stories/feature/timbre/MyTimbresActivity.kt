package com.kgmedia.fastfilm.feature.timbre

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.network.apis.entity.TimbreInfo
import com.kgmedia.fastfilm.databinding.ActivityMyTimbreBinding
import com.kgmedia.fastfilm.widget.decoration.BottomSpaceDecoration
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * 我的音色弹窗
 */
class MyTimbresActivity : BaseViewModelActivity<ActivityMyTimbreBinding, MyTimbresViewModel>() {

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            val context = context ?: return
            val starter = Intent(context, MyTimbresActivity::class.java)
            context.startActivity(starter)
        }
    }

    val launcher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            mRealVM.getTimbreStatus(it.data?.getStringExtra(Constants.Extra.ID))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.recyclerView.addItemDecoration(BottomSpaceDecoration(80.dp))
        mRealVM.onTimbreLongClick = { vm ->
            showDialog(vm)
//            CommonEditorDialogFragment.show(
//                supportFragmentManager,
//                "修改名称",
//                "请输入自定义名称",
//                8,
//                id = vm.data.get()?.timbreId,
//                type = CommonEditorDialogFragment.TYPE_TIMBRE_NAME
//            ).apply {
//                onModifySuccess = {
//                    vm.name.set(it.second)
//                }
//            }
        }
        mRealVM.onTimbreGenerateClick = { vm ->
            launcher.launch(Intent(this, TimbreExtractionActivity::class.java).apply {
                putExtra(TimbreExtractionActivity.EXTRA_TIMBRE_ID, vm.data.get()?.timbreId)
            })
        }
        binding.btnTimbreClone.setOnSingleClickListener {
            //mRealVM.stopPlay()
            launcher.launch(Intent(this, TimbreExtractionActivity::class.java))
        }
    }

    override fun onStop() {
        mRealVM.stopPlay()
        super.onStop()
    }

    private fun showDialog(vm: ItemMyTimbreVM) {
        TimbreMenuDialog.show(supportFragmentManager, vm.data.get(), object : TimbreMenuDialog.Callback {
            override fun onNameChange(info: TimbreInfo?, name: String?) {
                vm.name.set(name)
            }

            override fun onDelete(info: TimbreInfo?) {
                mRealVM.removeTimbreId(info?.timbreId)
                mRealVM.adapter.remove(vm)
            }
        })
    }
}