package io.aelf.portkey.component.dialog

import android.annotation.SuppressLint
import android.content.Context
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.input

class InputDialog {
    interface InputDialogCallback {
        fun onDialogPositiveClick(text: String?)
    }

    companion object {
        @SuppressLint("CheckResult")
        fun show(
            context: Context,
            InputDialogCallback: InputDialogCallback,
            title: String?,
            message: String?,
        ) {
            MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                title(text = title)
                message(text = message)
                input { _, text ->
                    InputDialogCallback.onDialogPositiveClick(text.toString())
                }
            }
        }
    }
}