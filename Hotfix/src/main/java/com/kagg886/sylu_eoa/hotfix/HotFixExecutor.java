package com.kagg886.sylu_eoa.hotfix;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.model.GPAScore;
import com.kagg886.sylu_eoa.ui.me.MeFragment;
import com.kagg886.sylu_eoa.ui.toolbox.Tool;
import com.kagg886.sylu_eoa.ui.toolbox.ToolBoxFragment;
import com.kagg886.sylu_eoa.ui.toolbox.ToolsAdapter;
import com.kagg886.sylu_eoa.util.GridItemDecoration;
import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;
import top.canyie.pine.callback.MethodReplacement;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

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

            Pine.hook(ToolBoxFragment.class.getMethod("onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class), new MethodReplacement() {
                @Override
                protected Object replaceCall(Pine.CallFrame callFrame) throws Throwable {
                    Context ctx = MainApplication.getCurrentActivity();
                    RecyclerView root = new RecyclerView(ctx);
                    root.setLayoutManager(new GridLayoutManager(ctx, 3));

                    ToolsAdapter a = new ToolsAdapter();

                    Field f = a.getClass().getDeclaredField("list");
                    f.setAccessible(true);
                    List<Tool> t = ((List<Tool>) f.get(null));
                    t.add(new Tool() {
                        @Override
                        public String getName() {
                            return "大创学分";
                        }

                        @Override
                        public int getImageResourceId() {
                            return com.kagg886.sylu_eoa.R.drawable.ic_search;
                        }

                        @Override
                        public Intent callActivity() {
                            AlertDialog.Builder b = new AlertDialog.Builder(MainApplication.getCurrentActivity());
                            b.setTitle("大创学分");

                            CompletableFuture.runAsync(() -> {
                                StringBuilder bd = new StringBuilder();
                                LoginConfig c = MainApplication.getApp().getConfig("account", LoginConfig.class);

                                c.getUser().getGPAs().forEach((k, v) -> {
                                    bd.append("\n分类:").append(k);
                                    for (GPAScore gpaScore : v) {
                                        bd.append("\n    ").append(gpaScore.getName()).append(gpaScore.getScore());
                                    }
                                });

                                MainApplication.getCurrentActivity().runOnUiThread(() -> {
                                    b.setMessage(bd.substring(1));
                                    b.show();
                                });
                            });
                            return null;
                        }
                    });

                    root.setAdapter(a);

                    GridItemDecoration gridItemDecoration = new GridItemDecoration(GridLayoutManager.VERTICAL);
                    gridItemDecoration.setColor(ctx.getColor(com.kagg886.sylu_eoa.R.color.purple_200));
                    root.addItemDecoration(gridItemDecoration);


                    return root;
                }
            });
        } catch (Throwable e) {
            return false;
        }
        return true;
    }
}
