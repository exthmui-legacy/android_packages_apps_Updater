package org.exthmui.updater;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.*;

import java.util.Objects;
import java.util.Set;

public class AdvancedSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private static final int REQUEST_CODE_DL_DIR = 1;
        private SharedPreferences mSharedPreferences;
        private MultiSelectListPreference mPrefAutoDownload;
        private EditTextPreference mDownloadPath;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getContext()));
            mPrefAutoDownload = findPreference("auto_download");
            mDownloadPath = findPreference("download_path");

            mPrefAutoDownload.setOnPreferenceChangeListener(this::OnPreferenceChange);

            mDownloadPath.setOnPreferenceChangeListener(this::OnPreferenceChange);

            mDownloadPath.setOnPreferenceClickListener(p -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_CODE_DL_DIR);
                return false;
            });
            OnPreferenceChange(mPrefAutoDownload, null);
            OnPreferenceChange(mDownloadPath, null);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_CODE_DL_DIR:
                        Uri uri = data.getData();
                        String path = uri.getPath();
                        mDownloadPath.setText(path);
                        break;
                }
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        private boolean OnPreferenceChange(Preference preference, @Nullable Object newValue) {
            try {
                if (preference == mPrefAutoDownload) {
                    Set<String> prefsValue = (Set<String>) newValue;
                    String summary = Objects.requireNonNull(getActivity()).getString(R.string.setting_auto_updates_download_use);
                    if (prefsValue == null) {
                        prefsValue = mSharedPreferences.getStringSet(preference.getKey(), null);
                        if (prefsValue == null) return true;
                    }
                    if (prefsValue.contains("cellular")) {
                        summary += (summary.equals(getActivity().getString(R.string.setting_auto_updates_download_use)) ? " " : " & ") +
                                getActivity().getString(R.string.setting_auto_updates_download_cellular);
                    }
                    if (prefsValue.contains("wifi")) {
                        summary += (summary.equals(getActivity().getString(R.string.setting_auto_updates_download_use)) ? " " : " & ") +
                                getActivity().getString(R.string.setting_auto_updates_download_wifi);
                    }
                    if (prefsValue.contains("bluetooth")) {
                        summary += (summary.equals(getActivity().getString(R.string.setting_auto_updates_download_use)) ? " " : " & ") +
                                getActivity().getString(R.string.setting_auto_updates_download_bluetooth);
                    }
                    if (prefsValue.contains("ethernet")) {
                        summary += (summary.equals(getActivity().getString(R.string.setting_auto_updates_download_use)) ? " " : " & ") +
                                getActivity().getString(R.string.setting_auto_updates_download_ethernet);
                    }
                    if (prefsValue.contains("vpn")) {
                        summary += (summary.equals(getActivity().getString(R.string.setting_auto_updates_download_use)) ? " " : " & ") +
                                getActivity().getString(R.string.setting_auto_updates_download_vpn);
                    }
                    if (prefsValue.contains("wifi_aware")) {
                        summary += (summary.equals(getActivity().getString(R.string.setting_auto_updates_download_use)) ? " " : " & ") +
                                getActivity().getString(R.string.setting_auto_updates_download_wifi_aware);
                    }
                    if (prefsValue.contains("6lowpan")) {
                        summary += (summary.equals(getActivity().getString(R.string.setting_auto_updates_download_use)) ? " " : " & ") +
                                getActivity().getString(R.string.setting_auto_updates_download_6lowpan);
                    }
                    if (summary.equals(getActivity().getString(R.string.setting_auto_updates_download_use))) {
                        summary = getActivity().getString(R.string.setting_auto_updates_download_never);
                    } else summary += " " + getActivity().getString(R.string.setting_auto_updates_download_network);

                    preference.setSummary(summary);
                }
                if (preference == mDownloadPath) {
                    if (newValue == null) {
                        mDownloadPath.setSummary(
                                mSharedPreferences.getString(preference.getKey(), getString(R.string.download_path)));
                        return false;
                    } else if (newValue == "") {
                        mDownloadPath.setText(getString(R.string.download_path));
                        return false;
                    } else preference.setSummary((String) newValue);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            return true;
        }
    }
}