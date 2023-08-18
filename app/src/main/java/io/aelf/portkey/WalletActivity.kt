package io.aelf.portkey

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import io.aelf.portkey.async.PortkeyAsyncCaller
import io.aelf.portkey.behaviour.wallet.PortkeyWallet
import io.aelf.portkey.databinding.ActivityWalletBinding
import io.aelf.portkey.global.WalletHolder
import io.aelf.portkey.internal.model.wallet.WalletBuildConfig
import io.aelf.portkey.utils.log.GLogger
import java.lang.reflect.Field

class WalletActivity : AppCompatActivity(), View.OnClickListener {
    private val binding: ActivityWalletBinding by lazy {
        ActivityWalletBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        assert(WalletHolder.wallet != null)
        binding.lockWallet.setOnClickListener(this)
        binding.buildConfig.setOnClickListener(this)
        binding.chainNetworkStatus.setOnClickListener(this)
        binding.keypair.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.lockWallet -> {
                WalletHolder.lockWallet()
                GLogger.w("wallet locked.")
                finish()
            }

            R.id.buildConfig -> {
                val wallet: PortkeyWallet = WalletHolder.wallet!!
                val sessionId = PortkeyWallet::class.java.getDeclaredField("sessionId")
                sessionId.isAccessible = true
                val sessionIdValue = sessionId.get(wallet)
                val walletBuildConfig = WalletBuildConfig()
                    .setSessionId(sessionIdValue as String)
                    .setAElfEndpoint(null)
                    .setPrivKey(wallet.keyPairInfo.privateKey)
                GLogger.i("walletBuildConfig:${Gson().toJson(walletBuildConfig)}")
            }

            R.id.chain_network_status -> {
                PortkeyAsyncCaller.asyncCall {
                    val wallet: PortkeyWallet = WalletHolder.wallet!!
                    val chainNetworkStatus = wallet.chainInstance.chainStatus
                    GLogger.i("chainNetworkStatus:${Gson().toJson(chainNetworkStatus)}")
                }
            }

            R.id.keypair -> {
                PortkeyAsyncCaller.asyncCall {
                    val wallet: PortkeyWallet = WalletHolder.wallet!!
                    val keyPair = wallet.keyPairInfo
                    GLogger.i("keyPair:${Gson().toJson(keyPair)}")
                }
            }
        }
    }
}