package io.aelf.portkey.global

import io.aelf.portkey.behaviour.wallet.PortkeyWallet

class WalletHolder {
    companion object {
        var wallet: PortkeyWallet? = null
            set(value) {
                assert(value != null)
                field = value
            }

        fun lockWallet() {
            wallet = null
        }
    }
}