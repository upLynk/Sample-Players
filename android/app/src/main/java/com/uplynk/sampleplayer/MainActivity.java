package com.uplynk.sampleplayer;

import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.MediaController;
import android.widget.RelativeLayout;

import com.uplynk.media.CaptionEvent;
import com.uplynk.media.CaptionEvent.CaptionEventType;
import com.uplynk.media.CaptionEvent.CaptionMode;
import com.uplynk.media.CaptionEvent.CaptionRow;
import com.uplynk.media.CaptionStyle;
import com.uplynk.media.MediaPlayer;
import com.uplynk.media.MediaPlayer.UplynkSegment;
import com.uplynk.media.MediaPlayer.UplynkID3;
import com.uplynk.media.MediaPlayer.UplynkMetadata;

import java.io.IOException;
import java.util.Vector;

public class MainActivity extends AppCompatActivity
        implements SurfaceHolder.Callback,
        MediaController.MediaPlayerControl,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnAssetBoundaryListener,
        MediaPlayer.OnCaptionEventListener,
        MediaPlayer.OnID3MetadataListener,
        MediaPlayer.OnUplynkMetadataListener,
        MediaPlayer.OnUplynkSegmentListener, MediaPlayer.OnVideoSizeChangedListener {

    private static String TAG = "MainActivity";

    private MediaPlayer mMediaPlayer;
    private MediaController mMediaController;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = (SurfaceView) this.findViewById(R.id.videoSurface);
        mSurfaceHolder = mSurfaceView.getHolder();
        // configure Surface Holder (required for certain devices)
        MediaPlayer.initSurfaceHolder(mSurfaceHolder);
        mSurfaceHolder.addCallback(this);
        mSurfaceView.setVisibility(View.VISIBLE);
    }

    private void playContent(final String url) {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        // Create a new media player
        mMediaPlayer = new MediaPlayer();

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setContext(this);     // allow persisting bandwidth stats (optional)
        //mMediaPlayer.setMaxBitrate(1536);  // bandwidth limit in Kps (optional)

        // Attach Listeners
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnVideoSizeChangedListener(this);
        mMediaPlayer.setOnID3MetadataListener(this);

        mMediaPlayer.setOnUplynkMetadataListener(this);
        mMediaPlayer.setOnCaptionEventListener(this);
        mMediaPlayer.setOnAssetBoundaryListener(this);

        // Attach Caption container (optional)
        mMediaPlayer.setCaptionsEnabled(true);
        mMediaPlayer.setCaptionLayoutContainer((RelativeLayout) this.findViewById(R.id.ccContainer));

        // Set Caption Styles (optional)
        CaptionStyle style = new CaptionStyle();
        style.setBackgroundColor(0x3300ff00);
        style.setTextColor(0xffdddddd);
        style.setTextSize(CaptionStyle.TEXT_SIZE_SMALL);
        style.setEdgeType(CaptionStyle.EDGE_TYPE_DROP_SHADOW);
        style.setTypeface(Typeface.SANS_SERIF);
        mMediaPlayer.setCaptionStyle(style);

        // Attach a MediaController (optional)
        mMediaController = new MediaController(this);
        mMediaController.setAnchorView(mSurfaceView);

        // Set the surface for the video output
        mMediaPlayer.setDisplay(mSurfaceHolder); // must already be created
        mMediaPlayer.setScreenOnWhilePlaying(true);

        try {
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.prepareAsync(); // start asynchronous player preparation
        } catch (IllegalArgumentException e) {
        /* handle error */
        } catch (IllegalStateException e) {
        /* handle error */
        } catch (IOException e) {
        /* handle error */
        }
    }

    // ---- Overrides for android.view.SurfaceHolder ----

    // As of build 61 (2014.03.20), a surfaceDestroyed(...) callback will unbind the
    // surface from the player to prevent memory leaks
    // make sure to rebind the surface when it is recreated via it's surfaceCreated(...) callback
    @Override
    public void surfaceCreated(SurfaceHolder sh) {
        Log.d(TAG, "surfaceCreated called");

        // need to re-attach surface
        if (mMediaPlayer != null && sh == mSurfaceHolder) {
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setScreenOnWhilePlaying(true);
        }
    }

    // for SurfaceHolder.Callback
    @Override
    public void surfaceChanged(SurfaceHolder sh, int format, int width, int height) {

    }

    // for SurfaceHolder.Callback
    @Override
    public void surfaceDestroyed(SurfaceHolder sh) {

    }

    // ---- Overrides for MediaPlayer ----
    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mp == mMediaPlayer) {
            mMediaController.setMediaPlayer(this); // attach media controller (optional)
            //mMediaPlayer.seekTo(20000); // set starting position in milliseconds if > 0 (optional)

            mMediaPlayer.start(); // tell player to start playback
        }
    }

    @Override
    public boolean onUplynkMetadata(MediaPlayer mp, UplynkMetadata metadata) {
        Log.d(TAG, "MediaPlayer::onUplynkMetadata called (" + metadata.toString() + ")");
        return true;
    }

    @Override
    public boolean onID3Metadata(MediaPlayer mp, UplynkID3 metadata) {
        Log.d(TAG, String.format("MediaPlayer::onID3Metadata: [%s] %s", metadata.getKey(), metadata.getValue()));
        return true;
    }

    @Override
    public boolean onCaptionEvent(MediaPlayer mp, CaptionEvent event) {
        Log.d(TAG, "MediaPlayer::onCaptionEvent called");

        // if POP_ON, render 'rows' data
        if (event.mode == CaptionMode.POP_ON) {
            // loop through all characters of all rows to render
            SparseArray<CaptionRow> rows = event.rows;
            for (int i = 0; i < rows.size(); ++i) {
                CaptionEvent.CaptionRow row = rows.valueAt(i);
                // Log.i(TAG,String.format("CC [%d] {%d} %s",row.getRow(),(row.getColumn()+row.getIndent()),row.getText()));

                Vector<CaptionEvent.CaptionCharacter> characters = row.getCharacters();
                for (int x = 0; x < characters.size(); ++x) {
                    CaptionEvent.CaptionCharacter cc = characters.get(x);
                    // Log.w(TAG,String.format("CC CHAR [%c]  color:%d  italic:%b  underlined:%b ",cc.character(), cc.color(), cc.isItalic(), cc.isUnderlined()));
                    // handle character
                }
            }
        }
        // if ROLL_UP, render single character or handle other event type
        else if (event.mode == CaptionMode.ROLL_UP) {
            if (event.eventType == CaptionEventType.TEXT) {
                CaptionEvent.CaptionCharacter cc = event.character;
                // Log.d(TAG,String.format("CC %c",cc.character()));
                // render individual character
            } else if (event.eventType == CaptionEventType.LINEBREAK) {
                // line break so push content up one line and start new line
            } else if (event.eventType == CaptionEventType.CLEAR) {
                // clear all content
            }
        }
        return true;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return true;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    @Override
    public boolean onUplynkSegmentList(MediaPlayer mp, Vector<UplynkSegment> segments) {
        return true;
    }

    @Override
    public void onAssetBoundary(MediaPlayer mp, String assetID) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {

    }

    // ---- Overrides for MediaController.MediaPlayerControl ----
    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public boolean isPlaying() {
        return true;
    }

    @Override
    public void pause() {

    }

    @Override
    public void seekTo(int pos) {

    }

    @Override
    public void start() {

    }


}
