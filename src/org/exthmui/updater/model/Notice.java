package org.exthmui.updater.model;

import android.util.Base64;

public class Notice implements NoticeInfo {

    private String mBase64Title;
    private String mBase64Texts;
    private String mTitle;
    private String mTexts;
    private String mImageUrl;
    private String mId;

    public Notice() {
    }

    public Notice(String base64Title, String base64Texts, String id, String imageUrl) {
        mBase64Title = base64Title;
        mBase64Texts = base64Texts;
        mId = id;
        mImageUrl = imageUrl;
        init();
    }

    public Notice(NoticeInfo noticeInfo) {
        mBase64Title = noticeInfo.getBase64Title();
        mBase64Texts = noticeInfo.getBase64Texts();
        mId = noticeInfo.getId();
        mImageUrl = noticeInfo.getImageUrl();
        init();
    }

    public void init() {
        mTitle = new String(Base64.decode(mBase64Title.getBytes(), Base64.DEFAULT));
        mTexts = new String(Base64.decode(mBase64Texts.getBytes(), Base64.DEFAULT));
    }

    public void setBase64Title(String base64Title) {
        mBase64Title = base64Title;
    }

    public void setBase64Texts(String base64Texts) {
        mBase64Texts = base64Texts;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setTexts(String texts) {
        mTexts = texts;
    }

    public void setId(String id) {
        mId = id;
    }

    public void setImageUrl(String imageUrl) {
        mImageUrl = imageUrl;
    }

    @Override
    public String getBase64Title() {
        return mBase64Title;
    }

    @Override
    public String getBase64Texts() {
        return mBase64Texts;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getTexts() {
        return mTexts;
    }

    @Override
    public String getImageUrl() {
        return mImageUrl;
    }

    @Override
    public String getId() {
        return mId;
    }
}

