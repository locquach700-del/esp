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
        tvStatus.setText("Injecting...");
        new Thread(() -> {
            try {
                // step 1: copy assets vào app private dir (không cần permission)
                File privateDir = getFilesDir();
                File patchFile  = new File(privateDir, "Assembly-CSharp-patch.bytes");
                File configFile = new File(privateDir, "localConfig.json");

                copyAssetToFile("leomodzdev_esp.dat", patchFile);
                copyAssetToFile("localConfig.json",   configFile);

                // step 2: dùng shell để move sang FF dir
                String ffDir = "/sdcard/Android/data/com.dts.freefiremax/files/il2cpp/Managed";
                String[] cmds = {
                    "mkdir -p " + ffDir,
                    "cp " + patchFile.getAbsolutePath() + " " + ffDir + "/Assembly-CSharp-patch.bytes",
                    "cp " + configFile.getAbsolutePath() + " " + ffDir + "/localConfig.json",
                    "chmod 777 " + ffDir + "/Assembly-CSharp-patch.bytes",
                    "chmod 777 " + ffDir + "/localConfig.json",
                };

                StringBuilder log = new StringBuilder();
                boolean ok = true;
                for (String cmd : cmds) {
                    java.lang.Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
                    p.waitFor();
                    int code = p.exitValue();
                    log.append(cmd.substring(0, Math.min(30, cmd.length()))).append(" → ").append(code).append("\n");
                    if (code != 0) ok = false;
                }

                boolean success = ok;
                String logStr = log.toString();

                runOnUiThread(() -> {
                    if (success) {
                        tvStatus.setText("Injected! Open FF Max now.");
                        startService(new Intent(this, FloatMenuService.class));
                    } else {
                        // shell fail — thử Shizuku path
                        tvStatus.setText("Shell failed. Try ZArchiver:\n\n"
                            + patchFile.getAbsolutePath() + "\n→ copy vào:\n"
                            + ffDir + "\n\nLog:\n" + logStr);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> tvStatus.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    void copyAssetToFile(String assetName, File dest) throws IOException {
        InputStream in = getAssets().open(assetName);
        FileOutputStream out = new FileOutputStream(dest);
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
