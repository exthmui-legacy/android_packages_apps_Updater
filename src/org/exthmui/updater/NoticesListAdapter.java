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

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.exthmui.updater.controller.UpdaterController;
import org.exthmui.updater.model.NoticeInfo;

import java.util.List;

public class NoticesListAdapter extends RecyclerView.Adapter<NoticesListAdapter.ViewHolder> {

    private static final String TAG = "NoticesListAdapter";

    private final BaseActivity mActivity;
    private List<String> mIds;
    private UpdaterController mUpdaterController;
    private final boolean mIsShort;


    public NoticesListAdapter(BaseActivity activity, boolean isShort) {
        mActivity = activity;
        mIsShort = isShort;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(mIsShort ? R.layout.notice_short_item_view : R.layout.notice_item_view, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {

        final String id = mIds.get(i);
        NoticeInfo notice = mUpdaterController.getNotice(id);

        String title = notice.getTitle();
        String text = notice.getTexts();
        viewHolder.mTitle.setText(title);
        viewHolder.mText.setText(text);
        if (mIsShort) {
            viewHolder.mNoticeLayout.setOnClickListener(v -> mActivity.startActivity(new Intent(mActivity, NoticesActivity.class)));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final View mNoticeLayout;
        private final TextView mTitle;
        private final TextView mText;

        public ViewHolder(final View view) {
            super(view);

            mNoticeLayout = view.findViewById(R.id.notice_layout);
            mTitle = view.findViewById(R.id.notice_title);
            mText = view.findViewById(R.id.notice_text);
        }
    }

    @Override
    public int getItemCount() {
        return mIds == null ? 0 : mIds.size();
    }

    public void setData(List<String> ids) {
        mIds = ids;
    }

    public void setUpdaterController(UpdaterController updaterController) {
        mUpdaterController = updaterController;
        notifyDataSetChanged();
    }

    public void notifyItemChanged(String id) {
        if (mIds == null) {
            return;
        }
        notifyItemChanged(mIds.indexOf(id));
    }

    public void removeItem(String id) {
        if (mIds == null) {
            return;
        }
        int position = mIds.indexOf(id);
        mIds.remove(id);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }
}