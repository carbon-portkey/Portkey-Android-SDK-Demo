package io.aelf.portkey.init;


import android.content.Context;

import io.aelf.portkey.component.logger.TimberLogger;
import io.aelf.portkey.component.storage.AndroidStorageHandler;
import io.aelf.portkey.global.NullableTools;
import io.aelf.portkey.global.SDKTestConfig;
import io.aelf.portkey.internal.tools.GlobalConfig;
import io.aelf.portkey.network.retrofit.RetrofitProvider;
import io.aelf.portkey.storage.StorageProvider;
import io.aelf.portkey.utils.log.GLogger;

public class InitController {

    public static void resetHost(Context context, String host) {
        RetrofitProvider.resetOrInitMainRetrofit(NullableTools.stringOrDefault(host, SDKTestConfig.DEFAULT_PORTKEY_API_HOST));
        GLogger.w("RetrofitProvider resetOrInitMainRetrofit success");
    }

    public static void init(Context context) {
        StorageProvider.init(new AndroidStorageHandler(context));
        GLogger.setLogger(new TimberLogger());
        GlobalConfig.setTestEnv(true);
        GLogger.w("PortkeySDK init success");
    }
}
