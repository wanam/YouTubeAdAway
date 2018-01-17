package ma.wanam.youtubeadaway;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
        TextView tvStatus = ((TextView) findViewById(R.id.moduleStatus));
        tvStatus.setText(status);
        tvStatus.setTextColor((XChecker.isEnabled() ? Color.GREEN : Color.RED));

        findViewById(id.btnOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(id.btnDonate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL));
                    startActivity(browserIntent);
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    finish();
                }
            }
        });
    }
}
