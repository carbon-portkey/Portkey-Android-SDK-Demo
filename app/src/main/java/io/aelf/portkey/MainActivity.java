package io.aelf.portkey;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.portkeysdk.databinding.ActivityMainBinding;
import io.aelf.portkey.init.InitController;
import io.aelf.portkey.utils.log.GLogger;
import io.aelf.response.ResultCode;
import io.aelf.utils.AElfException;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        InitController.init(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        GLogger.e("test",new AElfException(new AElfException(ResultCode.PARAM_ERROR)));
    }

}