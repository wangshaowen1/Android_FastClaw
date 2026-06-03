package com.shiyuan.fastclaw.feature.mine.viewModel


import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.parseMessage
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.response.FieldItems
import com.shiyuan.fastclaw.core.network.apis.response.UnderstandingScore
import com.shiyuan.fastclaw.core.response
import kotlin.String

/**
 */
class UnderstandsViewModel : AppRepositoryViewModel<AppRepository>() {


    val mToolbarVMUnderstands = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.shrimp_understands.getResString(),
        background = Color.TRANSPARENT,

        )

    var tableIndex = MutableLiveData(0)

    var resumeInfo = MutableLiveData<List<FieldItems>>()
    var jobPreference = MutableLiveData<List<FieldItems>>()
    var titleInfo = MutableLiveData<List<FieldItems>>()


    var underStandingScore = MutableLiveData<UnderstandingScore>()
     fun understandingScore() {
            launch(block = {
                response(
                    ApiFactory.apiService.understandingScore(),
                    result = {
                        underStandingScore.postValue(this.data)
                    },
                    error = {
                        this?.msg?.showToast()
                    })
            }, onError = {
                parseMessage(it)?.showToast()
            }, onComplete = {
                setLoading(false)
            })
        }

    fun understandingDetail() {
        setLoading(true)
        launch(block = {
            response(
                ApiFactory.apiService.understandingDetail(),
                result = {
                    resumeInfo.postValue(this.data?.resumeInfo)
                    jobPreference.postValue(this.data?.jobPreference)
                    titleInfo.postValue(this.data?.titleInfo)
                },
                error = {
                    this?.msg?.showToast()
                })
        }, onError = {
            parseMessage(it)?.showToast()
        }, onComplete = {
            setLoading(false)
        })
    }
}

