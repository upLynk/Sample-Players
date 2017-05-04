package com.uplynk.sampleplayer;

import android.graphics.Bitmap;

public class VideoItemInfo {

    public VideoItemInfo(String title, String url, String poster_url) {
        this.title = title;
        this.url = url;
        this.posterUrl = poster_url;
        this.posterBitmap = null;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getPosterUrl() {
        return posterUrl;
    }
    public Bitmap getPosterBitmap() { return posterBitmap; }
    public void setPosterBitmap(Bitmap bitmap) { this.posterBitmap = bitmap; }

    private String title;
    private String url;
    private String posterUrl;
    private Bitmap posterBitmap;
}
