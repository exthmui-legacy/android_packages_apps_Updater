package org.exthmui.updater;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

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
            case android.R.id.home:   //返回键的id
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
        private MultiSelectListPreference mAutoAttendZip;
        private EditTextPreference mAppendORS;

        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            mPrefAutoDownload = (MultiSelectListPreference) findPreference("auto_download");
            mDownloadPath = (EditTextPreference) findPreference("download_path");
            mAutoAttendZip = (MultiSelectListPreference) findPreference("auto_append_zip");
            mAppendORS = (EditTextPreference) findPreference("append_ors");

            mPrefAutoDownload.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return OnPreferenceChange(preference, newValue);
                }
            });

            mAutoAttendZip.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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
            OnPreferenceChange(mAutoAttendZip, null);
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
                if (preference == mAutoAttendZip) {
                    Set<String> prefsValue = (Set) newValue;
                    if (prefsValue == null) {
                        prefsValue = mSharedPreferences.getStringSet(preference.getKey(), prefsValue);
                        mAppendORS.setText("");
                    }
                    if (prefsValue.contains("magisk") && prefsValue.contains("gapps")) {
                        preference.setSummary(R.string.setting_append_ors_both);
                        mAppendORS.setText(getString(R.string.append_ors_gapps_script)+"\n"+getString(R.string.append_ors_magisk_script));

                    } else if (prefsValue.contains("magisk")) {
                        preference.setSummary(R.string.setting_append_ors_magisk);
                        mAppendORS.setText(getString(R.string.append_ors_magisk_script));
                    } else if (prefsValue.contains("gapps")) {
                        preference.setSummary(R.string.setting_append_ors_gapps);
                        mAppendORS.setText(getString(R.string.append_ors_gapps_script));
                    } else {
                        preference.setSummary("");
                        mAppendORS.setText("");
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            } finally {
                return true;
            }
        }
    }
}