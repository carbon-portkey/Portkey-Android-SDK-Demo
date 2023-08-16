package io.aelf.portkey

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.appcompat.app.AppCompatActivity
import io.aelf.portkey.async.PortkeyAsyncCaller
import io.aelf.portkey.behaviour.entry.EntryBehaviourEntity
import io.aelf.portkey.behaviour.entry.EntryBehaviourEntity.CheckedEntry
import io.aelf.portkey.behaviour.global.EntryCheckConfig
import io.aelf.portkey.behaviour.guardian.GuardianBehaviourEntity
import io.aelf.portkey.behaviour.login.LoginBehaviourEntity
import io.aelf.portkey.behaviour.pin.SetPinBehaviourEntity
import io.aelf.portkey.behaviour.register.RegisterBehaviourEntity
import io.aelf.portkey.behaviour.wallet.PortkeyWallet
import io.aelf.portkey.component.dialog.InputDialog
import io.aelf.portkey.component.recaptcha.GoogleRecaptchaService
import io.aelf.portkey.databinding.ActivityMainBinding
import io.aelf.portkey.global.WalletHolder
import io.aelf.portkey.init.InitController
import io.aelf.portkey.internal.model.common.AccountOriginalType
import io.aelf.portkey.utils.log.GLogger
import io.aelf.response.ResultCode
import io.aelf.utils.AElfException

class MainActivity : AppCompatActivity(), OnClickListener {

    private var binding: ActivityMainBinding? = null
    private var entryEntity: CheckedEntry? = null
    private var loginEntity: LoginBehaviourEntity? = null
    private var pinEntity: SetPinBehaviourEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        InitController.init(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        binding!!.entry.setOnClickListener(this)
        binding!!.login.setOnClickListener(this)
        binding!!.register.setOnClickListener(this)
        binding!!.setPin.setOnClickListener(this)
        checkButtonStatus()
        checkForLockedWallet()
    }

    override fun onRestart() {
        super.onRestart()
        entryEntity = null
        loginEntity = null
        pinEntity = null
        checkButtonStatus()
    }

    private fun checkForLockedWallet() {
        if (EntryBehaviourEntity.ifLockedWalletExists()) {
            GLogger.i("locked wallet exists, now to unlock.")
            InputDialog.show(this, object : InputDialog.InputDialogCallback {
                override fun onDialogPositiveClick(text: String?) {
                    GLogger.i("pin: $text")
                    try {
                        var wallet: PortkeyWallet? = null
                        EntryBehaviourEntity.attemptToGetLockedWallet().ifPresent {
                            if (!it.isValidPinValue(text!!)) {
                                GLogger.e("pin is not valid.")
                                return@ifPresent
                            }
                            wallet = it.unlockAndBuildWallet(text)
                        }
                        if (wallet != null) {
                            GLogger.i("wallet: unlocked.")
                            WalletHolder.wallet = wallet
                            val intent = Intent(this@MainActivity, WalletActivity::class.java)
                            startActivity(intent)
                        } else {
                            GLogger.e("wallet init failed. try again.")
                        }
                    } catch (e: Throwable) {
                        GLogger.e("wallet init failed. Restart APP and try again.")
                        throw AElfException(
                            e,
                            ResultCode.INTERNAL_ERROR,
                            "wallet init failed. Restart APP and try again.",
                            true
                        )
                    }
                    runOnUiThread {
                        checkButtonStatus()
                    }
                }
            }, "input pin", "input pin:")
        }
    }


    fun checkButtonStatus() {
        assert(binding != null)
        if (entryEntity == null) {
            binding!!.entry.isEnabled = true
            binding!!.login.isEnabled = false
            binding!!.register.isEnabled = false
            binding!!.setPin.isEnabled = false
        } else if (pinEntity == null) {
            val isRegistered = entryEntity!!.isRegistered
            binding!!.entry.isEnabled = false
            if (isRegistered) {
                binding!!.login.isEnabled = true
                if (loginEntity != null) {
                    binding!!.login.setText("Login(${loginEntity!!.fullFilledGuardianCount}/${loginEntity!!.guardianVerifyLimit})")
                }
                binding!!.register.isEnabled = false
            } else {
                binding!!.login.isEnabled = false
                binding!!.register.isEnabled = true
            }
            binding!!.setPin.isEnabled = false
        } else {
            binding!!.entry.isEnabled = false
            binding!!.login.isEnabled = false
            binding!!.register.isEnabled = false
            binding!!.setPin.isEnabled = true
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.entry -> {
                InputDialog.show(this, object : InputDialog.InputDialogCallback {
                    override fun onDialogPositiveClick(text: String?) {
                        GLogger.e("identity: $text")
                        PortkeyAsyncCaller.asyncCall {
                            val checkedEntry = EntryBehaviourEntity.attemptAccountCheck(
                                EntryCheckConfig()
                                    .setAccountIdentifier(text)
                                    .setAccountOriginalType(AccountOriginalType.Email)
                            )
                            if (checkedEntry.isRegistered) {
                                GLogger.i("account is registered, now go to login stage.")
                            } else {
                                GLogger.i("account is not registered, now go to register stage.")
                            }
                            entryEntity = checkedEntry
                            runOnUiThread {
                                checkButtonStatus()
                            }
                        }
                    }
                }, "input account identity", "input account identity (only accept email):")
            }

            R.id.login -> {
                assert(entryEntity != null)
                PortkeyAsyncCaller.asyncCall {
                    entryEntity?.asLogInChain()?.onLoginStep {
                        if (loginEntity == null) {
                            loginEntity = it
                        }
                        GLogger.i("try to login now.")
                        GLogger.i("now ${loginEntity!!.fullFilledGuardianCount}/${loginEntity!!.guardianVerifyLimit} guardians are fulfilled.")
                        val guardian = loginEntity!!.nextWaitingGuardian().get()
                        PortkeyAsyncCaller.asyncCall {
                            try {
                                guardianCheck(guardian, loginEntity, null)
                            } catch (e: Throwable) {
                                GLogger.e("guardian check failed.", AElfException(e))
                            }
                            runOnUiThread {
                                checkButtonStatus()
                            }
                        }
                    }
                }
            }

            R.id.register -> {
                assert(entryEntity != null)
                if (entryEntity!!.isRegistered) {
                    GLogger.i("account is registered, use another button.")
                    return
                }
                entryEntity!!.asRegisterChain().onRegisterStep {
                    PortkeyAsyncCaller.asyncCall {
                        try {
                            guardianCheck(it.guardian, null, it)
                        } catch (e: Throwable) {
                            GLogger.e("guardian check failed.", AElfException(e))
                        }
                        if (it.isVerified) {
                            GLogger.i("now registered. click lock button to continue.")
                            pinEntity = it.afterVerified()
                        }
                        runOnUiThread {
                            checkButtonStatus()
                        }
                    }
                }
            }

            R.id.lockWallet -> {
                assert(pinEntity != null)
                PortkeyAsyncCaller.asyncCall {
                    InputDialog.show(this@MainActivity, object : InputDialog.InputDialogCallback {
                        override fun onDialogPositiveClick(text: String?) {
                            GLogger.i("pin: $text")
                            if (!pinEntity!!.isValidPin(text!!)) {
                                GLogger.e("pin is not valid.")
                                return
                            }
                            try {
                                val wallet = pinEntity!!.lockAndGetWallet(text)
                                if (wallet != null) {
                                    GLogger.i("wallet: $wallet")
                                    WalletHolder.wallet = wallet
                                    val intent =
                                        Intent(this@MainActivity, WalletActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    GLogger.e("wallet init failed. try again.")
                                }
                            } catch (e: Throwable) {
                                GLogger.e("wallet init failed. try again.")
                            }
                            runOnUiThread {
                                checkButtonStatus()
                            }
                        }
                    }, "input pin", "input pin:")
                }
            }


        }
    }

    private fun guardianCheck(
        guardian: GuardianBehaviourEntity,
        loginBehaviourEntity: LoginBehaviourEntity?,
        registerEntity: RegisterBehaviourEntity?
    ) {
        GLogger.i("next guardian: ${guardian.originalGuardianInfo}")
        if (guardian.checkForReCaptcha()) {
            GLogger.i("we need to show reCaptcha, wait a minute.")
            GoogleRecaptchaService.verify(
                this@MainActivity,
                object : GoogleRecaptchaService.GoogleRecaptchaCallback {
                    override fun onGoogleRecaptchaSuccess(token: String?) {
                        PortkeyAsyncCaller.asyncCall {
                            GLogger.i("reCaptcha success, token: $token")
                            val succeed = guardian.sendVerificationCode(token!!)
                            GLogger.i("send verification code succeed: $succeed")
                            if (!succeed) {
                                GLogger.e("send verification code failed.")
                                return@asyncCall
                            }
                            runOnUiThread {
                                InputDialog.show(
                                    this@MainActivity,
                                    object : InputDialog.InputDialogCallback {
                                        override fun onDialogPositiveClick(text: String?) {
                                            GLogger.i("verification code: $text")
                                            val succeed1 =
                                                guardian.verifyVerificationCode(text!!)
                                            GLogger.i("verify verification code succeed: $succeed1")
                                            if (!succeed1) {
                                                GLogger.e("verify verification code failed.")
                                            }
                                            if (loginBehaviourEntity != null) {
                                                if (!loginBehaviourEntity.isFulfilled) {
                                                    GLogger.i("now ${loginBehaviourEntity.fullFilledGuardianCount}/${loginBehaviourEntity.fullFilledGuardianCount} guardians are fulfilled.")
                                                    GLogger.i("click lock button to continue.")
                                                } else {
                                                    GLogger.i("all guardians are fulfilled, now to lock.")
                                                }
                                            } else if (registerEntity != null) {
                                                GLogger.i("now registered. click lock button to continue.")
                                            }
                                        }
                                    }, "input verification code", "input verification code:"
                                )
                            }
                        }
                    }

                    override fun onGoogleRecaptchaFailed() {
                        GLogger.e("reCaptcha failed.")
                        throw Exception("reCaptcha failed.")
                    }
                })
        } else {
            GLogger.i("we don't need to show reCaptcha, now to send verification code.")
            val succeed = guardian.sendVerificationCode()
            GLogger.i("send verification code succeed: $succeed")
            if (!succeed) {
                GLogger.e("send verification code failed.")
                return
            }
            runOnUiThread {
                InputDialog.show(
                    this@MainActivity,
                    object : InputDialog.InputDialogCallback {
                        override fun onDialogPositiveClick(text: String?) {
                            GLogger.i("verification code: $text")
                            val succeed1 =
                                guardian.verifyVerificationCode(text!!)
                            GLogger.i("verify verification code succeed: $succeed1")
                            if (!succeed1) {
                                GLogger.e("verify verification code failed.")
                            }
                            if (loginBehaviourEntity != null) {
                                if (!loginBehaviourEntity.isFulfilled) {
                                    GLogger.i("now ${loginBehaviourEntity.fullFilledGuardianCount}/${loginBehaviourEntity.fullFilledGuardianCount} guardians are fulfilled.")
                                    GLogger.i("click login again to continue.")
                                } else {
                                    GLogger.i("all guardians are fulfilled, now to lock.")
                                }
                            } else if (registerEntity != null) {
                                GLogger.i("now registered. click lock button to continue.")
                            }
                        }
                    }, "input verification code", "input verification code:"
                )
            }
        }

    }


}