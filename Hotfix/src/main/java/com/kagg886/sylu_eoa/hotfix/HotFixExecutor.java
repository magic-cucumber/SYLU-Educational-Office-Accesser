package com.kagg886.sylu_eoa.hotfix;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.kagg886.sylu_eoa.ui.me.MeFragment;
import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

import java.util.concurrent.Callable;

/**
 * @author kagg886
 * @date 2023/9/14 22:35
 **/
public class HotFixExecutor implements Callable<Boolean> {
    @Override
    public Boolean call() throws Exception {
        try {
            Pine.hook(MeFragment.class.getMethod("onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class), new MethodHook() {
                @Override
                public void beforeCall(Pine.CallFrame callFrame) throws Throwable {
                    Log.i(HotFixExecutor.class.getName(), "Hotfix测试成功!");
                }
            });
        } catch (Throwable e) {
            return false;
        }
        return true;
    }
}
