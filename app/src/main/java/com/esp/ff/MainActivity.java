package com.esp.ff;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;

public class MainActivity extends Activity {

    static final int REQ_OVERLAY = 1001;
    Button btnInject, btnStop;
    TextView tvStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        btnInject = findViewById(R.id.btnInject);
        btnStop   = findViewById(R.id.btnStop);
        tvStatus  = findViewById(R.id.tvStatus);

        btnInject.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, REQ_OVERLAY);
            } else {
                doInject();
            }
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatMenuService.class));
            tvStatus.setText("Stopped.");
        });
    }

    void doInject() {
        // copy patch files vào FF data dir qua Shizuku/shell
        new Thread(() -> {
            try {
                copyPatchFiles();
                runOnUiThread(() -> {
                    startService(new Intent(this, FloatMenuService.class));
                    tvStatus.setText("Injected! Open FF Max now.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> tvStatus.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    void copyPatchFiles() throws Exception {
        // copy leomodzdev_esp.dat → FF patch path
        String ffDir = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/il2cpp/Managed/";
        copyAsset("leomodzdev_esp.dat",
                  ffDir + "Assembly-CSharp-patch.bytes");
        copyAsset("localConfig.json",
                  ffDir + "localConfig.json");
    }

    void copyAsset(String assetName, String destPath) throws Exception {
        // tạo dir qua shell
        new ProcessBuilder("sh", "-c", "mkdir -p " + new File(destPath).getParent())
            .start().waitFor();

        InputStream in = getAssets().open(assetName);
        FileOutputStream out = new FileOutputStream(destPath);
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close();
        out.close();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_OVERLAY && Settings.canDrawOverlays(this)) doInject();
    }
}
