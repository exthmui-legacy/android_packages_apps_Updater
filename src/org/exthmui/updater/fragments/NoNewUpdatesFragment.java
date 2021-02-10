package org.exthmui.updater.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.DateFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import org.exthmui.updater.MainActivity;
import org.exthmui.updater.R;
import org.exthmui.updater.misc.BuildInfoUtils;
import org.exthmui.updater.misc.Constants;
import org.exthmui.updater.misc.StringGenerator;

public class NoNewUpdatesFragment extends Fragment {

    private TextView mVersionView;              //显示exTHmUI 版本
    private TextView mLastCheckTextView;        //显示最后检查日期
    private TextView mAndroidVersionView;       //显示Android版本
    private TextView mHeaderBuildDateView;      //显示构建日期

    private Button mRefreshUpdateButton;

    private Context context;
    private MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_no_new_updates, container, false);

        this.mVersionView = view.findViewById(R.id.main_title);
        this.mVersionView.setText(getString(R.string.main_title_text,
                BuildInfoUtils.getBuildVersion()).replace("{os_name}", getString(R.string.os_name)));

        this.mAndroidVersionView = view.findViewById(R.id.main_android_version);
        this.mAndroidVersionView.setText(
                getString(R.string.main_android_version, Build.VERSION.RELEASE));

        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this.context);
        long lastCheck = preferences.getLong(Constants.PREF_LAST_UPDATE_CHECK, -1) / 1000;
        String lastCheckString = getString(R.string.main_last_updates_check,
                StringGenerator.getDateLocalized(this.context, DateFormat.LONG, lastCheck),
                StringGenerator.getTimeLocalized(this.context, lastCheck));
        this.mLastCheckTextView = view.findViewById(R.id.main_last_check);
        this.mLastCheckTextView.setText(lastCheckString);

        this.mHeaderBuildDateView = view.findViewById(R.id.main_build_date);
        this.mHeaderBuildDateView.setText(StringGenerator.getDateLocalizedUTC(this.context,
                DateFormat.LONG, BuildInfoUtils.getBuildDateTimestamp()));

        this.mRefreshUpdateButton = view.findViewById(R.id.main_check_for_updates);
        this.mRefreshUpdateButton.setOnClickListener(v ->
                this.mainActivity.check_updates(true));

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        this.mainActivity = (MainActivity) context;
    }
}
