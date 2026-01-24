package com.kgmedia.fastfilm.feature.settings.personal

import android.app.Activity
import android.content.Intent
import android.view.View
import com.blankj.utilcode.util.ClipboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.selectSinglePhotoFromAlbums
import com.kgmedia.fastfilm.common.permissions.requestImages2VideosPermissions
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.databinding.ActivityPersonalInformationBinding
import com.kgmedia.fastfilm.widget.AreaPicker
import com.kgmedia.fastfilm.widget.BottomDatePickerDialog
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import com.nhsd.mattise.Matisse
import java.util.Calendar

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:个人资料
 * @Data: 2025/4/21  16:40
 * @Version: 1.0
 * *****************************************
 */
class PersonalInformationActivity :
    BaseViewModelActivity<ActivityPersonalInformationBinding, PersonalInformationViewModel>() {

    companion object {
        private const val REQUEST_CODE_CHOOSE = 0x14
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, PersonalInformationActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.cl_avatar -> {
                //选择头像
                requestAlbumsPermission()
            }

            R.id.nicknameLayout -> {
                //昵称
                NicknameEditorDialogFragment.show(supportFragmentManager)
            }

            R.id.idLayout -> {
                //id
                mRealVM.uniqueId?.let {
                    ClipboardUtils.copyText(it)
                    getString(R.string.toast_copied).showToast()
                }
            }

            R.id.genderLayout -> {
                ChooseGenderDialog(true).show(supportFragmentManager)
            }

            R.id.birthdayLayout -> {
                showBirthdaySelector()
            }

            R.id.locationLayout -> {
                showCitySelector()
            }

            R.id.introduceLayout -> {
                //签名
                EditIntroductionDialog(true).show(supportFragmentManager)
            }
        }

    }


    /**
     * 请求存储权限并选择头像
     */
    private fun requestAlbumsPermission() {
        requestImages2VideosPermissions {
            if (it.not()) {
                //权限申请失败
                return@requestImages2VideosPermissions
            }
            //权限申请成功
            selectSinglePhotoFromAlbums(
                activity = this@PersonalInformationActivity,
                selectedUri = null,
                requestCode = REQUEST_CODE_CHOOSE,
                crop = true
            )
        }
    }

    private fun showBirthdaySelector() {
        val mDatePicker = BottomDatePickerDialog()
        val max = Calendar.getInstance()
//        max[Calendar.YEAR] = max[Calendar.YEAR] - 18
        mDatePicker.setMaxDate(max.timeInMillis)
        val min = Calendar.getInstance()
        min[1940, min[Calendar.MONTH]] = min[Calendar.DAY_OF_MONTH] + 1
        mDatePicker.setMinDate(min.timeInMillis)
        val birthday = Calendar.getInstance()
        kotlin.runCatching { birthday.time = DateUtil.parse(mRealVM.birthday.get() ?: Constants.DEFAULT_BIRTHDAY, DateUtil.FORMAT_DATE) }
        mDatePicker.updateDate(birthday[Calendar.YEAR], birthday[Calendar.MONTH], birthday[Calendar.DAY_OF_MONTH])
        mDatePicker.setDatePickListener { calendar ->
            val birthday = DateUtil.format(calendar.time, DateUtil.FORMAT_DATE)
            mRealVM.birthday.set(birthday)
            mRealVM.commit(birthday = calendar.time.time) { }
        }
        mDatePicker.show(supportFragmentManager)
    }

    private fun showCitySelector() {
        AreaPicker.show(supportFragmentManager) { province, city ->
            mRealVM.province = province
            mRealVM.city.set(city)
            mRealVM.commit(prov = province, city = city) { }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE) {
                val result = Matisse.obtainPathResult(data)
                result?.firstOrNull()?.let { mRealVM.changeAvatar(it) }
            }
        }
    }

}