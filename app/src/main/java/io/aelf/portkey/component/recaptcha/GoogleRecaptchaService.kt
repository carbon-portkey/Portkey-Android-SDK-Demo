package io.aelf.portkey.component.recaptcha

import android.content.Context
import com.google.android.gms.safetynet.SafetyNet
import io.aelf.portkey.global.SDKTestConfig
import io.aelf.portkey.internal.tools.GlobalConfig
import io.aelf.portkey.utils.log.GLogger
import io.aelf.utils.AElfException

object GoogleRecaptchaService {

    fun verify(context: Context?, callback: GoogleRecaptchaCallback?) {
        if (context != null) {
            SafetyNet.getClient(context).verifyWithRecaptcha(GlobalConfig.GOOGLE_RECAPTCHA_SITE_KEY)
                .addOnSuccessListener { response ->
                    val userResponseToken = response.tokenResult
                    if (userResponseToken != null) {
                        callback?.onGoogleRecaptchaSuccess(userResponseToken)
                    } else {
                        callback?.onGoogleRecaptchaFailed()
                    }
                }
                .addOnFailureListener { e ->
                    GLogger.e("GoogleRecaptchaService failed!", AElfException(e))
                    callback?.onGoogleRecaptchaFailed()
                }
        }
    }

    interface GoogleRecaptchaCallback {
        fun onGoogleRecaptchaSuccess(token: String?)
        fun onGoogleRecaptchaFailed()
    }

}