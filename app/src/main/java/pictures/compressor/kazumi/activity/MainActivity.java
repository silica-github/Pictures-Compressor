package pictures.compressor.kazumi.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yuyh.library.imgsel.ImgSelActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.shaohui.advancedluban.Luban;
import me.shaohui.advancedluban.OnMultiCompressListener;
import pictures.compressor.kazumi.R;
import pictures.compressor.kazumi.tool.CompressUtil;
import pictures.compressor.kazumi.tool.FileSizeUtil;
import pictures.compressor.kazumi.tool.TimeUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Bind(R.id.et_max_width)
    EditText et_max_width;

    @Bind(R.id.et_max_height)
    EditText et_max_height;

    @Bind(R.id.et_max_size)
    EditText et_max_size;

    @Bind(R.id.tv_data)
    TextView tv_data;

    @Bind(R.id.tv_output_path)
    TextView tv_output_path;

    @Bind(R.id.pb_processing)
    ProgressBar pb_processing;
    private long exitTime = 0;
    // 压缩支持格式
    private String[] supportFormat = {"jpg", "JPG", "png", "PNG", "bmp", "BMP", "jpeg", "JPEG"};

    // 获取文件扩展名
    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.cb_auto_width, R.id.cb_auto_height, R.id.cb_auto_size,
            R.id.rb_cover, R.id.rb_custom, R.id.btn_pick_pic})
    void btn(View v) {
        switch (v.getId()) {

            case R.id.cb_auto_width:
                Toast.makeText(MainActivity.this, "自动宽度", Toast.LENGTH_SHORT).show();
                break;

            case R.id.cb_auto_height:
                Toast.makeText(MainActivity.this, "自动高度", Toast.LENGTH_SHORT).show();
                break;

            case R.id.cb_auto_size:
                Toast.makeText(MainActivity.this, "自动尺寸", Toast.LENGTH_SHORT).show();
                break;

            case R.id.rb_cover:
                Toast.makeText(MainActivity.this, "覆盖原文件", Toast.LENGTH_SHORT).show();
                break;

            case R.id.rb_custom:
                Toast.makeText(MainActivity.this, "指定输出目录", Toast.LENGTH_SHORT).show();
                break;

            case R.id.btn_pick_pic:

                // 检查参数填写
                if (!checkParameter()) {
                    return;
                }

                ImgSelActivity.startActivity(this, CompressUtil.getConfig(this), 0);
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0 && resultCode == RESULT_OK && data != null) {

            if (!data.getBooleanExtra("isFolder", false)) {
                List<String> pathList = data.getStringArrayListExtra(ImgSelActivity.INTENT_RESULT);

                List<File> fileList = new ArrayList<>();

                for (String path : pathList) {
                    fileList.add(new File(path));
                }

                compressImages(fileList);
            } else {
                List<String> pathList = data.getStringArrayListExtra(ImgSelActivity.INTENT_RESULT);
                findImages(pathList.get(0));
            }


        }
    }

    // 遍历文件夹
    public void findImages(String path) {

        List<File> oldFileList = new ArrayList<>();

        int fileNum = 0, folderNum = 0, supportNum = 0, unSupportNum = 0;
        File file = new File(path);
        if (file.exists()) {
            LinkedList<File> list = new LinkedList<File>();
            File[] files = file.listFiles();
            for (File file2 : files) {
                if (file2.isDirectory()) {
                    list.add(file2);
                    folderNum++;
                } else {
                    if (isSupportExtension(getExtensionName(file2.getName()))) {
                        oldFileList.add(new File(file2.getAbsolutePath()));
                        supportNum++;
                    } else {
                        unSupportNum++;
                    }
                    fileNum++;
                }
            }
            File temp_file;
            while (!list.isEmpty()) {
                temp_file = list.removeFirst();
                files = temp_file.listFiles();
                for (File file2 : files) {
                    if (file2.isDirectory()) {
                        list.add(file2);
                        folderNum++;
                    } else {
                        fileNum++;
                    }
                }
            }
        }

        final List<File> tempFileList = oldFileList;

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("文件夹压缩")
                .setMessage("需要压缩: " + supportNum + "\n不支持压缩: " + unSupportNum + "\n要继续吗？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        compressImages(tempFileList);
                    }
                }).setNegativeButton("否", null)
                .show();
    }

    // 检查文件扩展名
    private boolean isSupportExtension(String checkFileName) {
        String tempExtension = getExtensionName(checkFileName);

        for (int i = 0; i < supportFormat.length; i++) {
            if (tempExtension.equals(supportFormat[i])) {
                return true;
            }
        }

        return false;
    }

    class SerialExecutor implements Executor {
        final Queue<Runnable> tasks = new ArrayDeque<>();
        final Executor executor;
        Runnable active;

        SerialExecutor(Executor executor) {
            this.executor = executor;
        }

        public synchronized void execute(final Runnable r) {
            tasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (active == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((active = tasks.poll()) != null) {
                executor.execute(active);
            }
        }
    }

    // 压缩
    private void compressImages(final List<File> oldFileList) {

        tv_data.setText("");

        Runnable run = new Runnable() {
            @Override
            public void run() {
                Luban.compress(MainActivity.this, oldFileList)
                        .putGear(Luban.CUSTOM_GEAR)
                        .setMaxSize(Integer.parseInt(et_max_size.getText().toString()))
                        .setMaxHeight(Integer.parseInt(et_max_height.getText().toString()))
                        .setMaxWidth(Integer.parseInt(et_max_width.getText().toString()))
                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                        .launch(new OnMultiCompressListener() {
                            @Override
                            public void onStart() {
                                tv_data.append(TimeUtil.src2DataTime(System.currentTimeMillis()) + " 开始压缩\n");
                                pb_processing.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onSuccess(List<File> fileList) {

                                for (int i = 0; i < oldFileList.size(); i++) {

                                    try {
                                        if (FileSizeUtil.getFileSize(oldFileList.get(i)) > Integer.parseInt(et_max_size.getText().toString())) {
                                            copyFile(fileList.get(i).getPath(), oldFileList.get(i).getPath());
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                tv_data.append(TimeUtil.src2DataTime(System.currentTimeMillis()) + " 压缩成功\n");

                                pb_processing.setVisibility(View.INVISIBLE);
                            }

                            @Override
                            public void onError(Throwable e) {
                                tv_data.append(TimeUtil.src2DataTime(System.currentTimeMillis()) + " 压缩失败\n");
                                pb_processing.setVisibility(View.INVISIBLE);
                            }
                        });
            }
        };

        new SerialExecutor(new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                command.run();
            }
        }).executor.execute(run);
    }

    // 复制文件
    private void copyFile(String oldPath, String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) {
                InputStream inStream = new FileInputStream(oldPath);
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread;
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        } catch (Exception e) {
            tv_data.append(TimeUtil.src2DataTime(System.currentTimeMillis()) + " 复制文件错误: " + e + "\n");
        }

        pb_processing.setVisibility(View.INVISIBLE);
    }

    // 检查参数
    private boolean checkParameter() {

        if (null == et_max_height.getText() || et_max_height.getText().toString().equals("")) {
            Toast.makeText(MainActivity.this, "最大输出高度错误", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (null == et_max_width.getText() || et_max_width.getText().toString().equals("")) {
            Toast.makeText(MainActivity.this, "最大输出宽度错误", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (null == et_max_size.getText() || et_max_size.getText().toString().equals("")) {
            Toast.makeText(MainActivity.this, "最大输出大小错误", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @Override   // 双击返回退出
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                exitTime = System.currentTimeMillis();
                Toast.makeText(MainActivity.this, "再按一次退出", Toast.LENGTH_SHORT).show();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}