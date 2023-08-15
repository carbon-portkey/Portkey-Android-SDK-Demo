package io.aelf.portkey.init;


import android.content.Context;

import io.aelf.portkey.component.logger.TimberLogger;
import io.aelf.portkey.component.storage.AndroidStorageHandler;
import io.aelf.portkey.global.SDKTestConfig;
import io.aelf.portkey.internal.tools.GlobalConfig;
import io.aelf.portkey.network.retrofit.RetrofitProvider;
import io.aelf.portkey.storage.StorageProvider;
import io.aelf.portkey.utils.log.GLogger;

public class InitController {
    public static void init(Context context) {
        StorageProvider.init(new AndroidStorageHandler(context));
        RetrofitProvider.resetOrInitMainRetrofit(SDKTestConfig.TEST_PORTKEY_API_HOST);
        GLogger.setLogger(new TimberLogger());
        GlobalConfig.setTestEnv(true);
        GlobalConfig.setCurrentChainId(GlobalConfig.ChainIds.TESTNET_CHAIN_ID_ALTERNATIVE);
        GLogger.w("PortkeySDK init success");
    }
}
