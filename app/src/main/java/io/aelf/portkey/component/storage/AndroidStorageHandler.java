package io.aelf.portkey.component.storage;

import static io.aelf.portkey.global.NullableTools.stringOrDefault;

import android.content.Context;

import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.Nullable;

import io.aelf.internal.AsyncResult;
import io.aelf.internal.ISuccessCallback;
import io.aelf.portkey.async.PortkeyAsyncCaller;
import io.aelf.portkey.internal.tools.GlobalConfig;
import io.aelf.portkey.storage.IStorageBehaviour;
import io.aelf.portkey.utils.log.GLogger;
import io.aelf.utils.AElfException;
import io.fastkv.FastKV;
import io.fastkv.interfaces.FastCipher;

public class AndroidStorageHandler implements IStorageBehaviour {
    private final FastKV kvProvider;

    public AndroidStorageHandler(Context context) {
        this(context, null, null, null);
    }

    public AndroidStorageHandler(Context context, @Nullable String storagePath, @Nullable String bucketName, @Nullable FastCipher cipher) {
        FastKV.Builder builder = new FastKV.Builder(
                stringOrDefault(storagePath, context.getFilesDir().getAbsolutePath()),
                stringOrDefault(bucketName, GlobalConfig.NAME_PORTKEY_SDK)
        );
        if (cipher != null) {
            builder.cipher(cipher);
        }
        this.kvProvider = builder.build();
    }

    @Override
    public String getValue(String key) {
        return kvProvider.getString(key);
    }

    @Override
    public void putValue(String key, String value) {
        kvProvider.putString(key, value);
    }

    @Override
    public void putValueAsync(String key, String value, @Nullable ISuccessCallback<Boolean> callback) {
        PortkeyAsyncCaller.getInstance().asyncCall(() -> {
            try {
                putValue(key, value);
                if (callback != null) {
                    callback.onSuccess(new AsyncResult<>(Boolean.TRUE));
                }
            } catch (AElfException e) {
                GLogger.e("putValueAsync failed.", e);
                if (callback != null) {
                    callback.onSuccess(new AsyncResult<>(Boolean.FALSE));
                }
            }
            return new AsyncResult<>(Boolean.TRUE);
        }, null, null);
    }

    @Override
    public boolean headValue(String key, String value) {
        String res = kvProvider.getString(key);
        if (TextUtils.isEmpty(value)) {
            return TextUtils.isEmpty(res);
        }
        return value.equals(res);
    }

    @Override
    public void removeValue(String key) {
        kvProvider.remove(key);
    }

    @Override
    public boolean contains(String key) {
        return kvProvider.contains(key);
    }

    @Override
    public void clear() {
        kvProvider.clear();
    }
}
