package io.aelf.portkey

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import com.jraska.console.Console
import io.aelf.internal.sdkv2.AElfClientV2
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
import io.aelf.portkey.global.SDKTestConfig
import io.aelf.portkey.global.WalletHolder
import io.aelf.portkey.init.InitController
import io.aelf.portkey.internal.model.common.AccountOriginalType
import io.aelf.portkey.internal.model.wallet.WalletBuildConfig
import io.aelf.portkey.utils.log.GLogger
import io.aelf.utils.AElfException


class MainActivity : AppCompatActivity(), OnClickListener, AdapterView.OnItemSelectedListener {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
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
        binding!!.mockWallet.setOnClickListener(this)
        binding!!.googleLogIn.setOnClickListener(this)
        val spinner: Spinner = binding!!.spinner
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.hosts_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = this
        checkButtonStatus()
        checkForLockedWallet()
        initGoogleSignInClient()
    }

    private fun clear() {
        entryEntity = null
        loginEntity = null
        pinEntity = null
        Console.clear()
        GLogger.w("clear : all data cleared.")
        checkButtonStatus()
    }

    private fun initGoogleSignInClient() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN) //请求邮箱
            .requestEmail()
            .requestIdToken(SDKTestConfig.GOOGLE_AUTH_TOKEN)
            .build()
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
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
                                GLogger.w("now using: " + (Gson().toJson(guardian.originalGuardianInfo)))
                                guardianCheck(guardian, loginEntity, null)
                            } catch (e: Throwable) {
                                GLogger.e("guardian check failed.", AElfException(e))
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
                            GLogger.w("now using: " + (Gson().toJson(it.guardian.originalGuardianInfo)))
                            guardianCheck(it.guardian, null, it)
                        } catch (e: Throwable) {
                            GLogger.e("guardian check failed.", AElfException(e))
                        }
                    }
                }
            }

            R.id.setPin -> {
                assert(pinEntity != null)
                InputDialog.show(this@MainActivity, object : InputDialog.InputDialogCallback {
                    override fun onDialogPositiveClick(text: String?) {
                        PortkeyAsyncCaller.asyncCall {
                            GLogger.i("pin: $text")
                            if (!pinEntity!!.isValidPin(text!!)) {
                                GLogger.e("pin is not valid.")
                                return@asyncCall
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
                    }
                }, "input pin", "input pin:")
            }

            R.id.googleLogIn -> {
                val account = GoogleSignIn.getLastSignedInAccount(this)
                if (account == null) {
                    val signInIntent = mGoogleSignInClient.signInIntent
                    mActivityLauncher.launch(signInIntent)
                } else {
                    GLogger.i("already logged in.")
                }
            }

            R.id.mockWallet -> {
                val keyPairInfo =
                    AElfClientV2(SDKTestConfig.TEST_AELF_NODE_HOST).generateKeyPairInfo()
                val mockEntity = SetPinBehaviourEntity(
                    WalletBuildConfig()
                        .setSessionId("mockSessionId")
                        .setAElfEndpoint(SDKTestConfig.TEST_AELF_NODE_HOST)
                        .setPrivKey(keyPairInfo.privateKey)
                )
                WalletHolder.wallet = mockEntity.lockAndGetWallet(SDKTestConfig.MOCK_PIN)
                val intent = Intent(this@MainActivity, WalletActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private val mActivityLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
//        if (result.resultCode == RESULT_OK) {
            val task =
                GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
//        } else {
//            GLogger.e("login failed.")
//            result.data?.extras?.getByteArray(result.data?.extras?.keySet()?.elementAt(0))?.let {
//                GLogger.e("status: ${String(it)}")
//            }
//        }
    }


    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Signed in successfully, show authenticated UI.
            GLogger.w("signInResult:success, account: $account")
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            GLogger.t("signInResult:failed code=" + e.statusCode)
            GLogger.t("signInResult:failed msg=" + e.statusMessage)
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
                                            PortkeyAsyncCaller.asyncCall {
                                                GLogger.i("verification code: $text")
                                                val succeed1 =
                                                    guardian.verifyVerificationCode(text!!)
                                                GLogger.i("verify verification code succeed: $succeed1")
                                                if (!succeed1) {
                                                    GLogger.e("verify verification code failed.")
                                                    return@asyncCall
                                                }
                                                if (loginBehaviourEntity != null) {
                                                    if (!loginBehaviourEntity.isFulfilled) {
                                                        GLogger.i("now ${loginBehaviourEntity.fullFilledGuardianCount}/${loginBehaviourEntity.fullFilledGuardianCount} guardians are fulfilled.")
                                                        GLogger.i("click lock button to continue.")
                                                    } else {
                                                        GLogger.w("all guardians are fulfilled, now to pin.")
                                                        pinEntity =
                                                            loginBehaviourEntity.afterVerified()
                                                    }
                                                } else if (registerEntity != null) {
                                                    GLogger.i("now registered. click lock button to continue.")
                                                    pinEntity = registerEntity.afterVerified()
                                                }
                                                runOnUiThread {
                                                    checkButtonStatus()
                                                }
                                            }
                                        }
                                    },
                                    "input verification code",
                                    "guardian info : \n" + (Gson().toJson(guardian.originalGuardianInfo)) +
                                            "\ninput verification code:"
                                )
                            }
                        }
                    }

                    override fun onGoogleRecaptchaFailed() {
                        GLogger.e("reCaptcha failed.")
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
                            PortkeyAsyncCaller.asyncCall {
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
                                        GLogger.w("all guardians are fulfilled, now to pin.")
                                        pinEntity = loginBehaviourEntity.afterVerified()
                                    }
                                } else if (registerEntity != null) {
                                    GLogger.i("now registered. click lock button to continue.")
                                    pinEntity = registerEntity.afterVerified()
                                }
                                runOnUiThread {
                                    checkButtonStatus()
                                }
                            }
                        }
                    },
                    "input verification code",
                    "guardian info : \n" + (Gson().toJson(guardian.originalGuardianInfo)) +
                            "\ninput verification code:"
                )
            }
        }

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (position) {
            0 -> {
                InitController.resetHost(this, SDKTestConfig.TEST2_PORTKEY_API_HOST)
            }

            1 -> {
                InitController.resetHost(this, SDKTestConfig.TEST1_PORTKEY_API_HOST)
            }

            2 -> {
                InitController.resetHost(this, SDKTestConfig.TEST_PORTKEY_API_HOST)
            }
        }
        clear()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        InitController.resetHost(this, SDKTestConfig.DEFAULT_PORTKEY_API_HOST)
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        GLogger.i("requestCode:$requestCode")
//        GLogger.i("resultCode:$resultCode")
//        GLogger.i("data:$data")
//    }
}
