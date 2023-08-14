package io.aelf.portkey.global;

import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NullableTools {

    public static @NotNull String stringOrDefault(@Nullable String inputStr, @NotNull String defaultStr) {
        return !TextUtils.isEmpty(inputStr) ? inputStr : defaultStr;
    }

}
