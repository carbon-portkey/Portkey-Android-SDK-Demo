package io.aelf.portkey.component.dialog

import android.content.Context
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet

class BasicDialog {
    interface BasicDialogCallback {
        fun onDialogPositiveClick()
    }

    companion object {
        fun show(
            context: Context,
            basicDialogCallback: BasicDialogCallback,
            title: String?,
            message: String?,
        ) {
            MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                title(text = title)
                message(text = message)
                positiveButton(text = "confirm") {
                    basicDialogCallback.onDialogPositiveClick()
                }
                negativeButton(text = "cancel")
            }
        }
    }
}