package io.aelf.portkey.component.logger

import android.annotation.SuppressLint
import android.util.Log
import com.jraska.console.timber.ConsoleTree
import io.aelf.portkey.utils.log.ILogger
import io.aelf.utils.AElfException
import timber.log.Timber

@SuppressLint("LogNotTimber")
class TimberLogger : ILogger {
    init {
        Timber.plant(ConsoleTree.builder().build())
        Timber.d("Console init success")
    }

    private val tag = "PortkeyLogger"

    override fun e(msg: String) {
        Timber.e(msg)
        Log.e(tag, msg)
    }

    override fun e(msg: String, exception: AElfException) {
        Timber.e(exception, msg)
        Log.e(tag, msg, exception)
    }

    override fun i(vararg msg: String?) {
        assert(msg.isNotEmpty())
        msg.forEach {
            Timber.i(it)
            Log.i(tag, it ?: "")
        }
    }

    override fun w(vararg msg: String?) {
        assert(msg.isNotEmpty())
        msg.forEach {
            Timber.w(it)
            Log.w(tag, it ?: "")
        }
    }
}