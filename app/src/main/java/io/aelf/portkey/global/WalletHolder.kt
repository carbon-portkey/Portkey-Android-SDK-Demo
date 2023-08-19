package io.aelf.portkey.global

import io.aelf.portkey.behaviour.wallet.PortkeyWallet

class WalletHolder {
    companion object {
        var wallet: PortkeyWallet? = null
        fun lockWallet() {
            wallet = null
        }
    }
}