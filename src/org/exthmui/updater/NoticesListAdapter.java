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

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.exthmui.updater.controller.NoticeController;
import org.exthmui.updater.model.NoticeInfo;
import org.exthmui.updater.ui.OnlineImageView;

import java.util.List;

public class NoticesListAdapter extends RecyclerView.Adapter<NoticesListAdapter.ViewHolder> {

    private static final String TAG = "NoticesListAdapter";

    private List<String> mIds;
    private Activity mActivity;
    private NoticeController mNoticeController;


    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mTitle;
        private TextView mText;
        private OnlineImageView mImageView;


        public ViewHolder(final View view) {
            super(view);

            mTitle = (TextView) view.findViewById(R.id.notice_title);
            mText = (TextView) view.findViewById(R.id.notice_text);
            mImageView = (OnlineImageView) view.findViewById(R.id.notice_imageView);
        }
    }

    public NoticesListAdapter(UpdatesListActivity activity) {
        mActivity = activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.notice_item_view, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {

        final String id = mIds.get(i);
        NoticeInfo notice = mNoticeController.getNotice(id);

        String title = notice.getTitle();
        String text = notice.getTexts();
        viewHolder.mTitle.setText(title);
        viewHolder.mText.setText(text);
    }

    @Override
    public int getItemCount() {
        return mIds == null ? 0 : mIds.size();
    }

    public void setData(List<String> ids) {
        mIds = ids;
    }

    public void setNoticeController(NoticeController noticeController) {
        mNoticeController = noticeController;
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