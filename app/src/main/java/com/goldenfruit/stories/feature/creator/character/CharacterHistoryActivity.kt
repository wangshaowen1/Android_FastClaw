package com.kgmedia.fastfilm.feature.creator.character

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
import com.kgmedia.fastfilm.databinding.ActivityCharacterHistoryBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setupTranslucentStatus


/**
 * @Introduce:角色生成历史
 */
class CharacterHistoryActivity :
    BaseViewModelActivity<ActivityCharacterHistoryBinding, CharacterHistoryViewModel>() {

    companion object {
        @JvmStatic
        fun startForResult(switchRoleLauncher: ActivityResultLauncher<Intent>, context: Context?, dramaId: String?, roleInfo: CharacterInfo?) {
            if (context == null) return
            val starter = Intent(context, CharacterHistoryActivity::class.java)
            starter.putExtra(Constants.Extra.ID, dramaId)
            starter.putExtra(Constants.Extra.DATA, roleInfo)
            switchRoleLauncher.launch(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.switchRoleEvent = {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun setTranslucentStatus() {
        setupTranslucentStatus(false)
    }
}