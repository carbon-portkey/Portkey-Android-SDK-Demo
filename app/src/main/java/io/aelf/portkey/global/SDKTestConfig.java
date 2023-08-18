package io.aelf.portkey.global;

public interface SDKTestConfig {
    String TEST_PORTKEY_API_HOST = "https://did-portkey-test.portkey.finance";
    String TEST1_PORTKEY_API_HOST = "https://localtest-applesign.portkey.finance";
    String TEST2_PORTKEY_API_HOST = "https://localtest-applesign2.portkey.finance";
//    String LOCAL_PORTKEY_API_HOST = "http://192.168.10.59:5577";
    String DEFAULT_PORTKEY_API_HOST = TEST2_PORTKEY_API_HOST;

    String MOCK_PIN="123456";



    String TEST_AELF_NODE_HOST = "https://aelf-test-node.aelf.io";
    String TEST_IDENTITY = "carbon.lv@aelf.io";
}
