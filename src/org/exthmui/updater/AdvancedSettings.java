package org.exthmui.updater;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.*;

import java.util.Set;

public class AdvancedSettings extends AppCompatActivity{

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
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private SharedPreferences mSharedPreferences;
        private MultiSelectListPreference mPrefAutoDownload;
        private EditTextPreference mDownloadPath;

        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            mPrefAutoDownload = (MultiSelectListPreference) findPreference("auto_download");
            mDownloadPath = (EditTextPreference) findPreference("download_path");

            mPrefAutoDownload.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return OnPreferenceChange(preference, newValue);
                }
            });

            mDownloadPath.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return OnPreferenceChange(preference, newValue);
                }
            });

            OnPreferenceChange(mPrefAutoDownload, null);
            OnPreferenceChange(mDownloadPath, null);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        private boolean OnPreferenceChange(Preference preference, @Nullable Object newValue) {
            try {
                if (preference == mPrefAutoDownload) {
                    Set<String> prefsValue = (Set) newValue;
                    if (prefsValue == null) {
                        prefsValue = mSharedPreferences.getStringSet(preference.getKey(), prefsValue);
                    }
                    if (prefsValue.contains("wifi") && prefsValue.contains("data")) {
                        preference.setSummary(R.string.setting_auto_updates_download_wifi_and_data);
                    } else if (prefsValue.contains("wifi")) {
                        preference.setSummary(R.string.setting_auto_updates_download_wifi);
                    } else if (prefsValue.contains("data")) {
                        preference.setSummary(R.string.setting_auto_updates_download_data);
                    } else {
                        preference.setSummary(R.string.setting_auto_updates_download_never);
                    }
                }
                if (preference == mDownloadPath) {
                    if(newValue == null || newValue == ""){
                        mDownloadPath.setText(getString(R.string.download_path));
                        return false;
                    }else preference.setSummary((String)newValue);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            } finally {
                return true;
            }
        }
    }
}