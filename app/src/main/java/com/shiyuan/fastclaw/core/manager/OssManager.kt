package com.shiyuan.fastclaw.core.manager

import com.kuaige.oss.OssConfigProvider
import com.kuaige.oss.OssStsConfig
import com.kuaige.oss.OssType
import com.shiyuan.fastclaw.BuildConfig

/**
 * 使用TosV2时主moudle注意脱糖 不然低版本Android会崩
 *
 *   compileOptions {
 *         coreLibraryDesugaringEnabled true
 *     }
 *
 *     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.1.4'
 */
object OssManager {

    const val TOS_NET_IMAGE_URL: String = "https://kuailongxia.oss-cn-shenzhen.aliyuncs.com/" //OSS 图片基础地址（桶域名:kuaige.tos-cn-guangzhou.volces.com）
    const val ALIYUN_NET_IMAGE_URL = "https://kuailongxia.oss-cn-shenzhen.aliyuncs.com/" //OSS 图片基础地址

    private const val ENV_PRODUCTION = "production"

    private val aliyunTestConfig = AliyunOssConfig(
        accessKeyId = "LTAI5t6PBTCGF1sst1p3bj9S",
        accessKeySecret = "fIsQEGSKydcODP0EZwZEAvf8gq1zmv",
        bucket = "kuailongxia",
        endpoint = "oss-cn-shenzhen.aliyuncs.com",
        netImageUrl = "https://kuailongxia.oss-cn-shenzhen.aliyuncs.com/",
    )

    private val aliyunProductionConfig = AliyunOssConfig(
        accessKeyId = "LTAI5t6tw3rRUzzobCXfgURZ",
        accessKeySecret = "6nk4UBNKiIRrwiO8cRfdtt9eWxUif7",
        bucket = "kuailongxia-res",
        endpoint = "oss-cn-shenzhen.aliyuncs.com",
        netImageUrl = "https://kuailongxia-res.oss-cn-shenzhen.aliyuncs.com/",
    )

    @JvmStatic
    fun init(ossType: OssType, ossFileNameProvider: (ossPathName: String,suffix: String) -> String) {
        val ossConfig = when (ossType) {
            OssType.TOS_V2 -> {
                getTosOssStsConfig()
            }
            OssType.ALIYUN -> {
                getALiYunOssStsConfig()
            }
            else -> {
                getTencentOssStsConfig()
            }
        }
        OssConfigProvider.init(ossType, ossConfig, ossFileNameProvider)
    }

    private fun getTosOssStsConfig(): OssStsConfig {
        return OssStsConfig(
            accessKeyId = "AKLTYzFjZjJhMDQ2YTRkNGZiMDhmN2UyNGJkYzE4MzM0ODY",
            accessKeySecret = "TWpZMlpUbGlOVGxtWldabE5HUm1PR0ZpTldKa1pHRmhPVEEzTm1Oall6VQ==",
            bucket = "kuaige",
            endpoint = "tos-cn-guangzhou.volces.com",
            region = "cn-guangzhou",
            netImageUrl = TOS_NET_IMAGE_URL,
        )
    }

    private fun getALiYunOssStsConfig(): OssStsConfig {
        val aliyunConfig = if (BuildConfig.FLAVOR_env == ENV_PRODUCTION) {
            aliyunProductionConfig
        } else {
            aliyunTestConfig
        }
        return OssStsConfig(
            accessKeyId = aliyunConfig.accessKeyId,
            accessKeySecret = aliyunConfig.accessKeySecret,
            bucket = aliyunConfig.bucket,
            endpoint = aliyunConfig.endpoint,
            region = null,
            netImageUrl = aliyunConfig.netImageUrl,
        )
    }

    private fun getTencentOssStsConfig(): OssStsConfig {
        return OssStsConfig(
            accessKeyId = "AKIDXwBF6lCSWN1ReeR1FMHC2ulSxHNGyTmG",
            accessKeySecret = "LSM3Xr44rUuDIA1jOuELFvOdfWIuUr0w",
            bucket = "jinguo-1357264972",
            endpoint = "",
            region = "ap-guangzhou",
            netImageUrl = "",
        )
    }

    private data class AliyunOssConfig(
        val accessKeyId: String,
        val accessKeySecret: String,
        val bucket: String,
        val endpoint: String,
        val netImageUrl: String,
    )
}
