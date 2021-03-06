package com.vansz.glideimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.FileUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.hitomi.tilibrary.loader.ImageLoader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Vans Z on 2020-02-28.
 */
public class GlideImageLoader implements ImageLoader {
    private Context context;
    private Map<String, SourceCallback> callbackMap;

    private static final String CACHE_DIR = "TransGlide";

    private GlideImageLoader(Context context) {
        this.context = context;
        this.callbackMap = new HashMap<>();
    }

    public static GlideImageLoader with(Context context) {
        return new GlideImageLoader(context);
    }

    @Override
    public void showImage(final String imageUrl, final ImageView imageView, final Drawable placeholder, final SourceCallback sourceCallback) {
        callbackMap.put(imageUrl, sourceCallback);
        if (sourceCallback != null) sourceCallback.onStart();
        Glide.with(imageView).download(imageUrl).placeholder(placeholder).listener(new RequestListener<File>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                SourceCallback callback = callbackMap.get(imageUrl);
                if (callback != null) callback.onDelivered(STATUS_DISPLAY_FAILED, null);
                return false;
            }

            @Override
            public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                if (!imageUrl.endsWith(".gif"))
                    imageView.setImageBitmap(BitmapFactory.decodeFile(resource.getAbsolutePath()));
                checkSaveFile(resource, getFileName(imageUrl));
                SourceCallback callback = callbackMap.get(imageUrl);
                if (callback != null) {
                    callback.onDelivered(STATUS_DISPLAY_SUCCESS, resource);
                    callbackMap.remove(imageUrl);
                }
                return false;
            }
        }).preload();
    }

    @Override
    public void loadImageAsync(final String imageUrl, final ThumbnailCallback callback) {
        Glide.with(context).download(imageUrl).listener(new RequestListener<File>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                if (callback != null)
                    callback.onFinish(null);
                return false;
            }

            @Override
            public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                checkSaveFile(resource, getFileName(imageUrl));
                if (callback != null)
                    callback.onFinish(new BitmapDrawable(context.getResources(),
                            BitmapFactory.decodeFile(resource.getAbsolutePath())));
                return false;
            }
        }).preload();
    }

    @Override
    public Bitmap loadImageSync(String imageUrl) {
        return BitmapFactory.decodeFile(getCache(imageUrl).getAbsolutePath());
    }

    @Override
    public File getCache(String url) {
        final File cacheFile = new File(getCacheDir(), getFileName(url));
        return cacheFile.exists() ? cacheFile : null;
    }

    @Override
    public void clearCache() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(context).clearDiskCache();
                Glide.get(context).clearMemory();
                FileUtils.delete(getCacheDir());
            }
        });
    }

    private File getCacheDir() {
        return new File(context.getCacheDir(), CACHE_DIR);
    }

    private String getFileName(String imageUrl) {
        String[] nameArray = imageUrl.split("/");
        return nameArray[nameArray.length - 1];
    }

    private void checkSaveFile(final File file, final String fileName) {
        final File cacheDir = getCacheDir();
        if (!cacheDir.exists()) cacheDir.mkdirs();

        boolean exists = FileUtils.isFileExists(new File(cacheDir, fileName));
        if (!exists) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File targetFile = new File(cacheDir, fileName);
                    FileUtils.copy(file, targetFile);
                }
            }).start();
        }
    }
}
