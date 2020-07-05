/*
 * Copyright (C) 2020 The exTHmUI Project, Copyright (C) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exthmui.updater;

import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.exthmui.updater.controller.UpdaterController;
import org.exthmui.updater.misc.StringGenerator;
import org.exthmui.updater.misc.UpdateActionsUtils;
import org.exthmui.updater.misc.Utils;
import org.exthmui.updater.model.UpdateInfo;
import org.exthmui.updater.model.UpdateStatus;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;

public class UpdatesListAdapter extends RecyclerView.Adapter<UpdatesListAdapter.ViewHolder> {

    private static final String TAG = "UpdateListAdapter";

    private List<String> mDownloadIds;
    private UpdaterController mUpdaterController;
    private final BaseActivity mActivity;

    public UpdatesListAdapter(BaseActivity activity) {
        mActivity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.update_item_view, viewGroup, false);
        return new ViewHolder(view);
    }

    public void setUpdaterController(UpdaterController updaterController) {
        mUpdaterController = updaterController;
        notifyDataSetChanged();
    }

    private void handleActiveStatus(ViewHolder viewHolder, UpdateInfo update) {
        boolean canDelete = false;

        final String downloadId = update.getDownloadId();
        if (mUpdaterController.isDownloading(downloadId)) {
            canDelete = true;
            String downloaded = StringGenerator.bytesToMegabytes(mActivity,
                    update.getFile().length());
            String total = Formatter.formatShortFileSize(mActivity, update.getFileSize());
            String percentage = NumberFormat.getPercentInstance().format(
                    update.getProgress() / 100.f);
            long eta = update.getEta();
            if (eta > 0) {
                CharSequence etaString = StringGenerator.formatETA(mActivity, eta * 1000);
                viewHolder.mProgressText.setText(mActivity.getString(
                        R.string.list_download_progress_eta_new, downloaded, total, etaString,
                        percentage));
            } else {
                viewHolder.mProgressText.setText(mActivity.getString(
                        R.string.list_download_progress_new, downloaded, total, percentage));
            }
            UpdateActionsUtils.setButtonAction(mActivity, mUpdaterController, viewHolder.mAction, UpdateActionsUtils.Action.PAUSE, downloadId, true);
            viewHolder.mProgressBar.setIndeterminate(update.getStatus() == UpdateStatus.STARTING);
            viewHolder.mProgressBar.setProgress(update.getProgress());
        } else if (mUpdaterController.isInstallingUpdate(downloadId)) {
            UpdateActionsUtils.setButtonAction(mActivity, mUpdaterController, viewHolder.mAction, UpdateActionsUtils.Action.CANCEL_INSTALLATION, downloadId, true);
            boolean notAB = mUpdaterController.isNotInstallingABUpdate();
            viewHolder.mProgressText.setText(notAB ? R.string.dialog_prepare_zip_message :
                    update.getFinalizing() ?
                            R.string.finalizing_package :
                            R.string.preparing_ota_first_boot);
            viewHolder.mProgressBar.setIndeterminate(false);
            viewHolder.mProgressBar.setProgress(update.getInstallProgress());
        } else if (mUpdaterController.isVerifyingUpdate(downloadId)) {
            UpdateActionsUtils.setButtonAction(mActivity, mUpdaterController, viewHolder.mAction, UpdateActionsUtils.Action.INSTALL, downloadId, false);
            viewHolder.mProgressText.setText(R.string.list_verifying_update);
            viewHolder.mProgressBar.setIndeterminate(true);
        } else {
            canDelete = true;
            UpdateActionsUtils.setButtonAction(mActivity, mUpdaterController, viewHolder.mAction, UpdateActionsUtils.Action.RESUME, downloadId, Utils.isBusy(mUpdaterController));
            String downloaded = StringGenerator.bytesToMegabytes(mActivity,
                    update.getFile().length());
            String total = Formatter.formatShortFileSize(mActivity, update.getFileSize());
            String percentage = NumberFormat.getPercentInstance().format(
                    update.getProgress() / 100.f);
            viewHolder.mProgressText.setText(mActivity.getString(R.string.list_download_progress_new,
                    downloaded, total, percentage));
            viewHolder.mProgressBar.setIndeterminate(false);
            viewHolder.mProgressBar.setProgress(update.getProgress());
        }

        viewHolder.itemView.setOnLongClickListener(getLongClickListener(update, canDelete,
                viewHolder.mBuildDate));
        viewHolder.mProgressBar.setVisibility(View.VISIBLE);
        viewHolder.mProgressText.setVisibility(View.VISIBLE);
        viewHolder.mBuildSize.setVisibility(View.INVISIBLE);
    }

    private void handleNotActiveStatus(ViewHolder viewHolder, UpdateInfo update) {
        final String downloadId = update.getDownloadId();
        if (mUpdaterController.isWaitingForReboot(downloadId)) {
            viewHolder.itemView.setOnLongClickListener(
                    getLongClickListener(update, false, viewHolder.mBuildDate));
            UpdateActionsUtils.setButtonAction(mActivity, mUpdaterController, viewHolder.mAction, UpdateActionsUtils.Action.REBOOT, downloadId, true);
        } else if (update.getPersistentStatus() == UpdateStatus.Persistent.VERIFIED) {
            viewHolder.itemView.setOnLongClickListener(
                    getLongClickListener(update, true, viewHolder.mBuildDate));
            UpdateActionsUtils.setButtonAction(mActivity, mUpdaterController, viewHolder.mAction,
                    Utils.canInstall(update) ? UpdateActionsUtils.Action.INSTALL : UpdateActionsUtils.Action.DELETE,
                    downloadId, Utils.isBusy(mUpdaterController));
        } else if (!Utils.canInstall(update)) {
            viewHolder.itemView.setOnLongClickListener(
                    getLongClickListener(update, false, viewHolder.mBuildDate));
            UpdateActionsUtils.setButtonAction(mActivity, mUpdaterController, viewHolder.mAction, UpdateActionsUtils.Action.INFO, downloadId, Utils.isBusy(mUpdaterController));
        } else {
            viewHolder.itemView.setOnLongClickListener(
                    getLongClickListener(update, false, viewHolder.mBuildDate));
            UpdateActionsUtils.setButtonAction(mActivity, mUpdaterController, viewHolder.mAction, UpdateActionsUtils.Action.DOWNLOAD, downloadId, Utils.isBusy(mUpdaterController));
        }
        String fileSize = Formatter.formatShortFileSize(mActivity, update.getFileSize());
        viewHolder.mBuildSize.setText(fileSize);

        viewHolder.mProgressBar.setVisibility(View.INVISIBLE);
        viewHolder.mProgressText.setVisibility(View.INVISIBLE);
        viewHolder.mBuildSize.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        if (mDownloadIds == null) {
            viewHolder.mAction.setEnabled(false);
            return;
        }

        final String downloadId = mDownloadIds.get(i);
        UpdateInfo update = mUpdaterController.getUpdate(downloadId);
        if (update == null) {
            // The update was deleted
            viewHolder.mAction.setEnabled(false);
            viewHolder.mAction.setImageResource(R.drawable.ic_file_download);
            return;
        }

        viewHolder.itemView.setSelected(downloadId.equals(UpdateActionsUtils.getmSelectedDownload()));

        boolean activeLayout;
        switch (update.getPersistentStatus()) {
            case UpdateStatus.Persistent.UNKNOWN:
                activeLayout = update.getStatus() == UpdateStatus.STARTING;
                break;
            case UpdateStatus.Persistent.VERIFIED:
                activeLayout = update.getStatus() == UpdateStatus.INSTALLING;
                break;
            case UpdateStatus.Persistent.INCOMPLETE:
                activeLayout = true;
                break;
            default:
                throw new RuntimeException("Unknown update status");
        }

        String buildDate = StringGenerator.getDateLocalizedUTC(mActivity,
                DateFormat.LONG, update.getTimestamp());
        String buildVersion = update.getVersionName().replace("{os_name}", mActivity.getResources().getString(R.string.os_name));
        viewHolder.mBuildDate.setText(buildDate);
        viewHolder.mBuildVersion.setText(buildVersion);
        viewHolder.mBuildVersion.setCompoundDrawables(null, null, null, null);

        viewHolder.mShowChangelog.setOnClickListener(v -> {
            final RotateAnimation animation;
            animation = new RotateAnimation(viewHolder.mBtnReset ? 180 : 0, viewHolder.mBtnReset ? 360 : 180, Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            animation.setInterpolator(new LinearInterpolator());
            animation.setDuration(200);
            //设置动画结束后保留结束状态
            animation.setFillAfter(true);
            viewHolder.mShowChangelog.startAnimation(animation);
            viewHolder.mBtnReset = !viewHolder.mBtnReset;

            int changelogVisibility;
            viewHolder.mShowChangelog.setAnimation(animation);
            animation.start();
            if (viewHolder.mChangelogLayout.getVisibility() == View.VISIBLE) {
                changelogVisibility = View.GONE;
            } else {
                changelogVisibility = View.VISIBLE;
            }
            viewHolder.mChangelog.setText(update.getChangeLog());
            viewHolder.mChangelogLayout.setVisibility(changelogVisibility);
        });
        if (activeLayout) {
            handleActiveStatus(viewHolder, update);
        } else {
            handleNotActiveStatus(viewHolder, update);
        }
    }

    @Override
    public int getItemCount() {
        return mDownloadIds == null ? 0 : mDownloadIds.size();
    }

    public void setData(List<String> downloadIds) {
        mDownloadIds = downloadIds;
    }

    public void notifyItemChanged(String downloadId) {
        if (mDownloadIds == null) {
            return;
        }
        notifyItemChanged(mDownloadIds.indexOf(downloadId));
    }

    public void removeItem(String downloadId) {
        if (mDownloadIds == null) {
            return;
        }
        int position = mDownloadIds.indexOf(downloadId);
        mDownloadIds.remove(downloadId);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    private View.OnLongClickListener getLongClickListener(final UpdateInfo update,
                                                          final boolean canDelete, View anchor) {
        return view -> {
            notifyItemChanged(update.getDownloadId());
            UpdateActionsUtils.startActionMode(mActivity, mUpdaterController, update, canDelete, anchor);
            return true;
        };
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageButton mAction;
        private final ImageButton mShowChangelog;
        private final TextView mBuildDate;
        private final TextView mBuildVersion;
        private final TextView mBuildSize;
        private final RelativeLayout mChangelogLayout;
        private final TextView mChangelog;
        private final ProgressBar mProgressBar;
        private final TextView mProgressText;
        private boolean mBtnReset;


        public ViewHolder(final View view) {
            super(view);
            mAction = view.findViewById(R.id.update_action);
            mShowChangelog = view.findViewById(R.id.show_changelog);
            mChangelogLayout = view.findViewById(R.id.changelog_layout);

            mBtnReset = false;

            mBuildDate = view.findViewById(R.id.build_date);
            mBuildVersion = view.findViewById(R.id.build_version);
            mBuildSize = view.findViewById(R.id.build_size);


            mChangelog = view.findViewById(R.id.changelog);
            //mChangelogLayout.setVisibility(View.GONE);

            mProgressBar = view.findViewById(R.id.progress_bar);
            mProgressText = view.findViewById(R.id.progress_text);
        }
    }

}
