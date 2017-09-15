package pictures.compressor.kazumi.tool;

import android.content.Context;
import android.graphics.Color;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.yuyh.library.imgsel.ImageLoader;
import com.yuyh.library.imgsel.ImgSelConfig;

import pictures.compressor.kazumi.R;

public class CompressUtil {

    // 自定义图片加载器
    private static ImageLoader loader = new ImageLoader() {
        @Override
        public void displayImage(Context context, String path, ImageView imageView) {
            Glide.with(context)
                    .load(path)
                    .error(R.mipmap.img_failed)
                    .into(imageView);
        }
    };

    // 初始化
    public static ImgSelConfig getConfig(Context context) {
        ImgSelConfig config = new ImgSelConfig.Builder(context, loader)
                .multiSelect(true)
                .rememberSelected(false)
                .btnBgColor(Color.BLACK)
                .btnTextColor(Color.WHITE)
                .statusBarColor(Color.BLACK)
                .title("")
                .titleColor(Color.BLACK)
                .needCamera(false)
                .titleBgColor(Color.BLACK)
                .maxNum(100)
                .build();
        return config;
    }
}
