package com.uplynk.sampleplayer;

public class VideoItemInfo {

    public VideoItemInfo(String title, String url, String poster_url) {
        this.title = title;
        this.url = url;
        this.posterUrl = poster_url;
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

    private String title;
    private String url;
    private String posterUrl;
}
