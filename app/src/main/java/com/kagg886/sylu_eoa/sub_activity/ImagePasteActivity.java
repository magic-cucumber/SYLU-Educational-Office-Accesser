package com.kagg886.sylu_eoa.sub_activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.kagg886.sylu_eoa.util.UIUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author kagg886
 * @date 2023/9/24 11:28
 **/
public class ImagePasteActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> launcher;

    private String stuID;
    private String name;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stuID = getIntent().getStringExtra("stuID");
        name = getIntent().getStringExtra("name");

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        List<Uri> uris = new LinkedList<>();
                        if (Objects.requireNonNull(result.getData()).getData() != null) {
                            try {
                                uris.add(result.getData().getData());
                            } catch (Exception ignored) {
                            }
                        } else {
                            ClipData clipData = result.getData().getClipData();
                            if (clipData != null) {
                                for (int i = 0; i < clipData.getItemCount(); i++) {
                                    ClipData.Item item = clipData.getItemAt(i);
                                    uris.add(item.getUri());
                                }
                            }
                        }
                        Toast.makeText(this, "选择了" + uris.size() + "张图片", Toast.LENGTH_SHORT).show();
                        CompletableFuture.supplyAsync(() -> {
                                    try {
                                        return makePhoto(uris);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .thenAccept((file) -> {
                                    Intent intent = new Intent("android.intent.action.SEND");
                                    intent.putExtra("android.intent.extra.STREAM", FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file));
                                    intent.setType(uris.size() == 1 ? "image/*" : "*/*");
                                    startActivity(intent);
                                    finish();
                                }).exceptionally((e) -> {
                                    UIUtil.showToast(ImagePasteActivity.this, "发生了一个错误，添加失败！");
                                    Log.e(ImagePasteActivity.class.getName(), "executing Image Paste Failed!", e);
                                    finish();
                                    return null;
                                });
                    }
                });

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);//关键！多选参数
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        launcher.launch(intent);
    }


    private File makePhoto(List<Uri> uris) throws Exception {
        String zip = UUID.randomUUID().toString().replace("-", "") + (uris.size() == 1 ? ".png" : ".zip");
        File file = new File(getCacheDir().toPath().resolve("share").toFile(), zip);
        file.getParentFile().mkdirs();

        if (uris.size() != 1) {
            ZipOutputStream zipStream;
            try {
                file.createNewFile();
                zipStream = new ZipOutputStream(Files.newOutputStream(file.toPath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            for (int i = 0; i < uris.size(); i++) {
                Bitmap src = BitmapFactory.decodeStream(getContentResolver().openInputStream(uris.get(i)));
                src = cloneAndDrawText(src, String.format("%s\n%s", stuID, name));

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                src.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();

                zipStream.putNextEntry(new ZipEntry(i + ".png"));
                zipStream.write(out.toByteArray());
                zipStream.flush();
            }
            try {
                zipStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            Bitmap src = BitmapFactory.decodeStream(getContentResolver().openInputStream(uris.get(0)));
            src = cloneAndDrawText(src, String.format("%s\n%s", stuID, name));

            FileOutputStream out = new FileOutputStream(file);
            src.compress(Bitmap.CompressFormat.PNG, 80, out);
            out.flush();
            out.close();
        }
        return file;
    }

    public Bitmap cloneAndDrawText(Bitmap src, String text) {
        //准备画笔类
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(dip2px(px2dip(src.getWidth() * 0.1f)));

        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);

        Bitmap dst = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, 0, 0, null);
        String[] lines = text.split("\n");
        float y = 10;
        for (String line : lines) {
            float len = textPaint.measureText(line);
            y += textPaint.getTextSize();

            float left = (src.getWidth() - len) / 2;
            canvas.drawRect(left, y - textPaint.getTextSize(), left + len, y, bgPaint);
            //居中
            canvas.drawText(line, (src.getWidth() - len) / 2.0f, y, textPaint);
        }
        src.recycle();
        return dst;
    }

    private int dip2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    private int px2dip(float pxValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
