package com.kagg886.sylu_eoa.ui.me;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.alibaba.fastjson2.JSON;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.SyluUser;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.databinding.FragmentMeBinding;
import com.kagg886.sylu_eoa.model.Profile;
import com.kagg886.sylu_eoa.sub_activity.LoginActivity;
import com.kagg886.sylu_eoa.sub_activity.ProfileDetailsActivity;
import com.kagg886.sylu_eoa.util.UIUtil;
import com.tencent.mmkv.MMKV;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MeFragment extends Fragment {

    private FragmentMeBinding binding;
    private final Consumer<Profile> profileRegister = (profile) -> {

        Bitmap bitmap = BitmapFactory.decodeByteArray(profile.getAvatar(), 0, profile.getAvatar().length);
        String userName = profile.getName();
        String coll = profile.getCollegeName();
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                binding.name.setText(userName);
                binding.desc.setText(coll);
                binding.avatar.setImageBitmap(bitmap);

                binding.detailButton.setOnClickListener(v -> {
                    Intent i = new Intent(getContext(), ProfileDetailsActivity.class);
                    i.putExtra("data", JSON.toJSONString(profile));
                    startActivity(i);
                });
            });
        }
    };


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        ListView v = binding.list;
        v.setAdapter(new SettingAdapter(getActivity()));

        ActivityResultLauncher<Intent> loginLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            //此处是跳转的result回调方法
            if (result.getResultCode() == Activity.RESULT_OK) {
                CompletableFuture.supplyAsync(() -> {
                    LoginConfig config = MainApplication.getApp().getConfig("account", LoginConfig.class);
                    SyluUser user = config.getUser();
                    return user.getProfile();
                }).thenAccept(profileRegister);
            }
        });

        LoginConfig config = MainApplication.getApp().getConfig("account", LoginConfig.class);
        SyluUser user = config.getUser();
        CompletableFuture.supplyAsync(() -> {
            if (user == null) {
                return true;
            }
            if (user.isCookieOutOfDate()) {
                //重新登录
                try {
                    user.loginByPwd(config.getPass());
                    return false;
                } catch (Exception e) {
                    return true;
                }
            } else {
                return false;
            }
        }).thenAccept((needLogin) -> {
            if (needLogin) {
                MMKV.defaultMMKV().remove("account").apply();
                Intent i = new Intent(MainApplication.getApp(), LoginActivity.class);
                if (user != null) {
                    UIUtil.showToast(getActivity(), "登录会话过期，请重新登录");
                    i.putExtra("user", user.getUserID());
                }
                loginLauncher.launch(i);
                return;
            }
            profileRegister.accept(user.getProfile());
        }).exceptionally((ex) -> {
            //Fragment被销毁后，getContext()为null
            if (getActivity() != null) {
                UIUtil.showDialog(getActivity(), "发生了一个错误:", ex.getMessage());
            }
            return null;
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}