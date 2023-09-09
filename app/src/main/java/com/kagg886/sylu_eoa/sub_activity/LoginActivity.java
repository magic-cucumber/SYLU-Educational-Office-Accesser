package com.kagg886.sylu_eoa.sub_activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.SyluUser;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.databinding.ActivityLoginBinding;
import com.kagg886.sylu_eoa.exception.LoginException;

import java.util.concurrent.CompletableFuture;

public class LoginActivity extends AppCompatActivity implements TextWatcher {
    private ActivityLoginBinding binding;

    private LoginConfig config;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setResult(RESULT_CANCELED);

        config = MainApplication.getApp().getConfig("account", LoginConfig.class);

        final EditText userEdit = binding.username;
        final EditText passEdit = binding.password;

        if (config.getUser() != null) {
            userEdit.setText(config.getUser().getUserID());
        }
        //userEdit.setText(Optional.ofNullable(MainApplication.getApp().getConfig("userSession", SyluUser.class).getUserID()).orElse(""));

        userEdit.addTextChangedListener(this);
        passEdit.addTextChangedListener(this);
        final Button loginButton = binding.login;
        final ProgressBar loadingProgressBar = binding.loading;

        loginButton.setOnClickListener(v -> {
            loginButton.setEnabled(false);
            loadingProgressBar.setVisibility(View.VISIBLE);

            CompletableFuture.supplyAsync(() -> {
                if (config.getUser() == null) {
                    config.setUser(SyluUser.createUser(userEdit.getText().toString()));
                }
                config.getUser().loginByPwd(passEdit.getText().toString());
                //TODO 手动触发响应式更新，待深层响应式更新开发完毕后移除
                config.setUser(config.getUser());
                return config.getUser();
            }).thenAccept((session) -> {
                setResult(RESULT_OK);
                finish();
            }).exceptionally((throwable -> {
                throwable = throwable.getCause();
                if (throwable instanceof LoginException) {
                    Throwable finalThrowable = throwable;
                    runOnUiThread(() -> new AlertDialog.Builder(LoginActivity.this).setTitle("登陆失败!").setMessage(finalThrowable.getMessage()).show());
                }
                runOnUiThread(() -> {
                    binding.login.setEnabled(true);
                    binding.loading.setVisibility(View.INVISIBLE);
                });
                return null;
            }));
        });
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        boolean canLogin = binding.username.getText().length() != 0 && binding.password.getText().length() != 0;

        binding.login.setEnabled(canLogin);
    }
}