package io.aelf.portkey.component.storage;

import static io.aelf.portkey.global.NullableTools.stringOrDefault;

import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.aelf.internal.ISuccessCallback;
import io.aelf.portkey.internal.tools.GlobalConfig;
import io.aelf.portkey.storage.IStorageBehaviour;
import io.fastkv.FastKV;
import io.fastkv.interfaces.FastCipher;

public class AndroidStorageHandler implements IStorageBehaviour {
    private final FastKV kvProvider;

    public AndroidStorageHandler(Context context){
        this(context,null,null,null);
    }

    public AndroidStorageHandler(Context context, @Nullable String storagePath, @Nullable String bucketName, @Nullable FastCipher cipher) {
        FastKV.Builder builder =new FastKV.Builder(
                stringOrDefault(storagePath, context.getFilesDir().getAbsolutePath()),
                stringOrDefault(bucketName, GlobalConfig.NAME_PORTKEY_SDK)
        );
        if (cipher != null) {
            builder.cipher(cipher);
        }
        this.kvProvider=builder.build();
    }

    @Override
    public String getValue(String key) {
        return null;
    }

    @Override
    public void putValue(String key, String value) {

    }

    @Override
    public void putValueAsync(String key, String value, @Nullable ISuccessCallback<Boolean> callback) {

    }

    @Override
    public boolean headValue(String key, String value) {
        return false;
    }

    @Override
    public void removeValue(String key) {

    }

    @Override
    public boolean contains(String key) {
        return false;
    }

    @Override
    public void clear() {

    }
}
