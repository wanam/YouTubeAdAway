package ma.wanam.youtubeadaway;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
    private static SharedPreferences prefs;

    @SuppressLint("WorldReadableFiles")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        try {
            prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_WORLD_READABLE);
        } catch (SecurityException ignored) {
            Log.e(getPackageName(), ignored.toString());
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // copy btc wallet address to the clipboard
            getPreferenceScreen().findPreference("donate_btc")
                    .setOnPreferenceClickListener(preference -> {
                        ClipboardManager clipboardManager = (ClipboardManager)
                                getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText(preference.getKey(), preference.getSummary());
                        clipboardManager.setPrimaryClip(clipData);

                        return true;
                    });

            // set module status
            Preference statusPreference = getPreferenceScreen().findPreference("status");
            statusPreference.setIcon(XChecker.isEnabled() ? android.R.drawable.ic_dialog_info : android.R.drawable.ic_dialog_alert);
            statusPreference.setSummary(XChecker.isEnabled() ? R.string.module_active : R.string.module_inactive);
        }
    }
}