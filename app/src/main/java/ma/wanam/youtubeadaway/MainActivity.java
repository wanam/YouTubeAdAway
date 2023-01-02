package ma.wanam.youtubeadaway;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import ma.wanam.youtubeadaway.R.id;

public class MainActivity extends Activity {
    private final String DONATE_URL = "https://www.paypal.me/Wanam";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String pInfo = "";
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Resources res = getResources();
        String status = res.getString(R.string.app_name)
                + " v"
                + pInfo
                + " "
                + (XChecker.isEnabled() ? res.getString(R.string.module_active) : res
                .getString(R.string.module_inactive));
        TextView tvStatus = findViewById(id.moduleStatus);
        tvStatus.setText(status);
        tvStatus.setTextColor((XChecker.isEnabled() ? Color.GREEN : Color.RED));

        findViewById(id.btnOK).setOnClickListener(v -> finish());

        findViewById(id.btnDonate).setOnClickListener(v -> {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL));
                startActivity(browserIntent);
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                finish();
            }
        });

        TextView tv = findViewById(id.textViewBTCAdr);
        tv.setOnLongClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("btc_adr", tv.getText());
            cm.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(), R.string.addr_copied, Toast.LENGTH_SHORT).show();
            return false;
        });
    }
}
