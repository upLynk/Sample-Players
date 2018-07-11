package com.uplynk.sampleplayer;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.uplynk.media.CaptionEvent;
import com.uplynk.media.CaptionEvent.CaptionEventType;
import com.uplynk.media.CaptionEvent.CaptionMode;
import com.uplynk.media.CaptionEvent.CaptionRow;
import com.uplynk.media.CaptionStyle;
import com.uplynk.media.MediaPlayer;
import com.uplynk.media.MediaPlayer.UplynkAssetInfo;
import com.uplynk.media.MediaPlayer.UplynkID3;
import com.uplynk.media.MediaPlayer.UplynkMetadata;
import com.uplynk.media.MediaPlayer.UplynkSegment;
import com.uplynk.media.MediaPlayer.UplynkTrackInfo;
import com.uplynk.widgets.MediaController;
import com.uplynk.widgets.UplynkTrackAdapter;

import java.io.IOException;
import java.util.Vector;

public class VideoActivity extends AppCompatActivity
        implements DialogInterface.OnClickListener,
        MediaController.MediaPlayerControl,
        MediaPlayer.OnAssetBoundaryListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCaptionEventListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnID3MetadataListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnUplynkMetadataListener,
        MediaPlayer.OnUplynkSegmentListener,
        MediaPlayer.OnVideoSizeChangedListener,
        SurfaceHolder.Callback {

    private static String TAG = "VideoActivity";

    // Sintel w Alt Audio and WebVTT
    private String mUrlToPlay = "";

    // com.uplynk.media.MediaPlayer provides access the uplynk playback library
    private MediaPlayer mMediaPlayer;
    // MediaController will let us show playback controls on the video surface
//    private MediaController mMediaController;
    // SurfaceView - the view where the video will be rendered
    private SurfaceView mSurfaceView;
    // SurfaceHolder - accessor class for SurfaceView
    private SurfaceHolder mSurfaceHolder;
    // the content/ad segment map
    private Vector<UplynkSegment> mSegmentMap;

    // Text view for first line of metadata display
    private TextView mMetadataTextView;
    // Text view for second line of metadata display
    private TextView mMetadataTextView2;
    // Dialog for audio/subtitle track selection
    private AlertDialog mTracksDialog;
    // Keep the type of the last audio/cc dialog shown
    private TrackOptionsType mDialogType;
    // Flag an error condition
    private boolean mError = false;
    // Flag for enabling/disabling display of captions
    private boolean mCaptionsEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video);

        Intent intent = getIntent();
        mUrlToPlay = intent.getStringExtra("java.lang.String");

        logDisplayAndBuildInfo();

        // Setting FULLSCREEN hides top bar (clock and network icons)
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(lp);

        mSurfaceView = (SurfaceView) this.findViewById(R.id.videoSurface);
        mSurfaceHolder = mSurfaceView.getHolder();
        // configure Surface Holder (required for certain devices)
        MediaPlayer.initSurfaceHolder(mSurfaceHolder);
        mSurfaceHolder.addCallback(this);
        mSurfaceView.setVisibility(View.VISIBLE);

        mMetadataTextView = (TextView) this.findViewById(R.id.textView);
        mMetadataTextView2 = (TextView) this.findViewById(R.id.textView2);

        // tap handler to show controls
//        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                // respond to touch up events on the video surface
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//                    // show the media controller
//                    if (mMediaController != null) {
//                        mMediaController.show();
//                    }
//                }
//                return true;
//            }
//        });

        Button playBtn = (Button) this.findViewById(R.id.playButton);
        playBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Play Button Clicked");
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.getState() == MediaPlayer.PLAYER_STATE_PAUSED || mMediaPlayer.getState() == MediaPlayer.PLAYER_STATE_PREPARED) {
                        Log.w(TAG, "Calling MediaPlayer.start()");
                        mMediaPlayer.start();
                    } else if (mMediaPlayer.getState() == MediaPlayer.PLAYER_STATE_STOPPED) {
                        Log.w(TAG, "Calling MediaPlayer.setDataSource()");
                        try {
                            mMediaPlayer.setDataSource(mUrlToPlay);
                            mMediaPlayer.prepareAsync();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else
                        Log.w(TAG, String.format("Cannot Start Playback, player is in state: %d", mMediaPlayer.getState()));
                }
            }
        });

        Button pauseBtn = (Button) this.findViewById(R.id.pauseButton);
        pauseBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Pause Button Tapped");
                if (mMediaPlayer != null) {
                    mMediaPlayer.pause();
                }
            }
        });

        Button stopBtn = (Button) this.findViewById(R.id.stopButton);
        stopBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Stop Button Tapped");
                if (mMediaPlayer != null) {
                    mMediaPlayer.stop();
                }
            }
        });

        Button audioBtn = (Button) this.findViewById(R.id.audioButton);
        audioBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Audio Button Tapped");
                if (mMediaPlayer != null) {
                    showTrackOptions(TrackOptionsType.AUDIO);
                }
            }
        });

        Button subtitleBtn = (Button) this.findViewById(R.id.subtitleButton);
        subtitleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "subTitle Button Tapped");
                if (mMediaPlayer != null) {
                    showTrackOptions(TrackOptionsType.SUBTITLES);
                }
            }
        });
    } // end of onCreate

    @Override
    protected void onPause() {

        super.onPause();

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }

        if (mTracksDialog != null && mTracksDialog.isShowing())
            mTracksDialog.dismiss();

    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.w(TAG, "Activity::OnResume()");
    }

    @Override
    protected void onStart() {

        super.onStart();

        Log.w(TAG, "Activity::OnStart()");

        if (mMediaPlayer == null /* || !mMediaPlayer.isPlaying() */) {
            createMediaPlayerAfterDelay(mUrlToPlay, 500);
        }
    }

    @Override
    protected void onDestroy() {
        // make sure to unload the media player
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        super.onDestroy();
    }

    /*
     * Resize the video surface when the screen orientation changes
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.w(TAG, "Activity::OnConfigurationChanged  -  Screen Orientation:" +
                ((newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) ? "Landscape" : "Portrait"));

        final View parent = (View) mSurfaceView.getParent();
        final int oldWidth = parent.getWidth();

        Log.e(TAG, String.format("SurfaceView Parent Size: %dx%d", parent.getWidth(), parent.getHeight()));

        ViewTreeObserver observer = parent.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Getting multiple layout changes, so only do our thing when the size actually changes
                if (parent.getWidth() != oldWidth) {
                    Log.e(TAG, String.format("SurfaceView Parent New Size %dx%d", parent.getWidth(), parent.getHeight()));
                    parent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (mMediaPlayer != null) {
                        resizeSurfaceViewInParent(mSurfaceView, mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
                    }
                }
            }
        });
    }

    // handle FireTV Remote buttons
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                // ... handle selections
                handled = true;
                Log.w(TAG, "Select Button Pressed");
                showToast("Restarting Media");
                restartMedia();
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // ... handle left action
                handled = true;
                Log.w(TAG, "DPad Left Pressed");
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // ... handle right action
                handled = true;
                Log.w(TAG, "DPad Right Pressed");
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                handled = true;
                Log.w(TAG, "DPad Down Pressed");
                if (mMediaPlayer != null) {
                    mCaptionsEnabled = !mCaptionsEnabled;
                    mMediaPlayer.setCaptionsEnabled(mCaptionsEnabled);
                    showToast("Toggle Captions");
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                handled = true;
                Log.w(TAG, "Media Play/Pause Pressed");
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.getState() == MediaPlayer.PLAYER_STATE_PLAYING)
                        mMediaPlayer.pause();
                    else if (mMediaPlayer.getState() == MediaPlayer.PLAYER_STATE_PAUSED
                            || mMediaPlayer.getState() == MediaPlayer.PLAYER_STATE_PREPARED)
                        mMediaPlayer.start();
                }
                break;
            case KeyEvent.KEYCODE_MENU:
                handled = false;
                Log.w(TAG, "Menu Pressed");
                break;
        }
        return handled || super.onKeyDown(keyCode, event);
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

    @Override
    public void surfaceChanged(SurfaceHolder sh, int format, int width, int height) {
        if (sh == mSurfaceHolder) {
            Log.w(TAG, "MediaPlayer::surfaceChanged called   [MAIN]  width " + width + ", height " + height);
            //resizeSurfaceViewInParent(mSurfaceView, width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder sh) {
        Log.d(TAG, "MediaPlayer::surfaceDestroyed called");
        if (sh == mSurfaceHolder) {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying())
                    mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
    }

    // ---- Overrides for MediaPlayer ----
    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mp == mMediaPlayer) {
//            mMediaController.setMediaPlayer(this); // attach media controller (optional)
            //mMediaPlayer.seekTo(20000); // set starting position in milliseconds if > 0 (optional)

            mMediaPlayer.start(); // tell player to start playback
        }
    }

    @Override
    public boolean onUplynkMetadata(MediaPlayer mp, UplynkMetadata metadata) {
        Log.d(TAG, "MediaPlayer::onUplynkMetadata called (" + metadata.toString() + ")");
        UplynkAssetInfo assetInfo = metadata.getAssetInfo();
        mMetadataTextView.setText(metadata.toString());
        mMetadataTextView2.setText((assetInfo != null) ? assetInfo.getDescription() : "");
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
                // CaptionEvent.CaptionCharacter cc = event.character;
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
        Log.e(TAG, "MediaPlayer::onError--->   what:" + what + "    extra:" + extra);

        if (mp != null) {
            mp.stop();
            mp.release();
            mMediaPlayer = null;
        }

        mError = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Error Playing Media: (" + what + ") " + getMediaError(what));

        AlertDialog alert = builder.create();
        alert.show();

        return true;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.i(TAG, "onSeekComplete ********");
    }

    @Override
    public boolean onUplynkSegmentList(MediaPlayer mp, Vector<UplynkSegment> segments) {
//        Log.i(TAG, "Segment List: " + segments.toString());
        mSegmentMap = segments;
        return true;
    }

    @Override
    public void onAssetBoundary(MediaPlayer mp, String assetID) {
        Log.i(TAG, "onAssetBoundary: " + assetID);
        if (assetID == null && mMediaPlayer != null) {
            Log.i(TAG, "********************* POSSIBLE AD BREAK *******************");
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "MediaPlayer::onCompletion called");

        // done so close the video activity
        this.finish();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if (mp != mMediaPlayer) return;

        Log.i(TAG, "MediaPlayer::onVideoSizeChanged called: " + width + "x" + height);
        resizeSurfaceViewInParent(mSurfaceView, width, height);
    }

    // ---- Overrides for MediaController.MediaPlayerControl ----
    @Override
    public boolean canPause() {
        return (mMediaPlayer != null) ? mMediaPlayer.canPause() : true;
    }

    @Override
    public boolean canSeekBackward() {
        return (mMediaPlayer != null) ? mMediaPlayer.canSeekBackward() : false;
    }

    @Override
    public boolean canSeekForward() {
        return (mMediaPlayer != null) ? mMediaPlayer.canSeekForward() : false;
    }

    @Override
    public int getAudioSessionId() {
        return (mMediaPlayer != null) ? mMediaPlayer.getAudioSessionId() : 0;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return (mMediaPlayer != null && !mError) ? mMediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public int getDuration() {
        return (mMediaPlayer != null && !mError) ? mMediaPlayer.getDuration() : 0;
    }

    @Override
    public boolean isPlaying() {
        return (mMediaPlayer != null) ? mMediaPlayer.isPlaying() : false;
    }

    @Override
    public void pause() {
        if (mMediaPlayer != null)
            mMediaPlayer.pause();
    }

    @Override
    public void seekTo(int pos) {
        if (mMediaPlayer != null)
            mMediaPlayer.seekTo(pos);
    }

    @Override
    public void start() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }

    public Vector<MediaPlayer.UplynkTrackInfo> getAudioTracks() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getAudioTrackOptions();
        }
        return null;
    }

    public void selectAudioTrack(int trackNum) {
        if (mMediaPlayer != null) {
            Log.i(TAG, "Audio Track Selection: " + trackNum);
            mMediaPlayer.selectAudioTrack(trackNum);
        }
    }

    public Vector<MediaPlayer.UplynkTrackInfo> getSubtitleTracks() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getSubtitleTrackOptions();
        }
        return null;
    }

    public void selectSubtitleTrack(int trackNum) {
        if (mMediaPlayer != null) {
            Log.i(TAG, "Subtitle Track Selection: " + trackNum);
            mMediaPlayer.selectSubtitleTrack(trackNum);
        }
    }

    public void setCaptionsEnabled(boolean enabled) {
        if (mMediaPlayer != null) {
            Log.i(TAG, "Set Captions Enabled: " + enabled);
            mMediaPlayer.setCaptionsEnabled(enabled);
        }
    }

    public void setCaptionStyle(CaptionStyle style) {
        if (mMediaPlayer != null) {
            Log.i(TAG, "setCaptionStyle: " + style.toString());
            mMediaPlayer.setCaptionStyle(style);
        }
    }

    public void setVolume(float volume) {
        if (mMediaPlayer != null) {
            Log.i(TAG, "setVolume: " + volume);
            mMediaPlayer.setVolume(volume, volume);
        }
    }

    // for future build 96
    public boolean isLive() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isLive();
        }
        return false;
    }

    // for future build 96
    public Vector<MediaPlayer.UplynkSegment> getSegmentMap() {
        return mSegmentMap;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.d(TAG, "MediaPlayer::onBufferingUpdate called --->   percent:" + percent);

    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "MediaPlayer::onInfo: " + what);

//        if (what == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
//
//        }

        return true;
    }

    //  ---- Overrides for DialogInterface.OnClickListener ----
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mMediaPlayer == null) return;

        if (mDialogType == TrackOptionsType.SUBTITLES) {
            Log.i(TAG, "SubTitle Track Selection: " + which);
            mMediaPlayer.selectSubtitleTrack(which);
        } else {
            Log.i(TAG, "Audio Track Selection: " + which);
            mMediaPlayer.selectAudioTrack(which);
        }
    }

    // ---- Helpers ----

    private void playContent(final String url) {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        // Create a new media player
        mMediaPlayer = new MediaPlayer();

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setContext(this);     // allow persisting bandwidth stats (optional)
        //mMediaPlayer.setMaxBitrate(1536);  // bandwidth limit in Kbps (optional)

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
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnInfoListener(this);
        mMediaPlayer.setOnVideoSizeChangedListener(this);
        mMediaPlayer.setOnUplynkSegmentListener(this);

        // Attach Caption container (optional)
        mMediaPlayer.setCaptionsEnabled(true);
        mMediaPlayer.setCaptionLayoutContainer((RelativeLayout) this.findViewById(R.id.ccContainer));

        // Set Caption Styles (optional)
        CaptionStyle style = new CaptionStyle();
        style.setBackgroundColor(0x883B4963); // AARRGGBB
        style.setTextColor(0xffdddddd);
        style.setTextSize(CaptionStyle.TEXT_SIZE_NORMAL); // 100% of native size
        style.setEdgeType(CaptionStyle.EDGE_TYPE_DROP_SHADOW);
        style.setTypeface(Typeface.SANS_SERIF);
        mMediaPlayer.setCaptionStyle(style);

        // Attach a MediaController (optional)
//        mMediaController = new MediaController(this);
//        mMediaController.setAnchorView((FrameLayout) mSurfaceView.getParent());

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

    private String getMediaError(int errorCode) {
        if (errorCode == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            return "Media server died";
// TODO: Error codes in future release of uplynk lib
//        } else if (errorCode == MediaPlayer.MEDIA_ERROR_UNKNOWN_SERVER_ERROR) {
//            return "Unspecified error communicating with media server";
        } else if (errorCode == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
            return "Not valid for progressive playback";
        } else if (errorCode == MediaPlayer.MEDIA_ERROR_UNSUPPORTED_FORMAT) {
            return "Media format is not supported";
//        } else if (errorCode == MediaPlayer.MEDIA_ERROR_CONNECT_ERROR) {
//            return "Network connection error.  Verify network connectivity";
//        } else if (errorCode == MediaPlayer.MEDIA_ERROR_MEDIA_NOT_FOUND) {
//            return "Cannot find requested media asset";
//        } else if (errorCode == MediaPlayer.MEDIA_ERROR_BAD_REQUEST) {
//            return "Server responded with: \"Bad Request\"";
//        } else if (errorCode == MediaPlayer.MEDIA_ERROR_NOT_AUTHORIZED) {
//            return "Not authorized to access requested resource";
//        } else if (errorCode == MediaPlayer.MEDIA_ERROR_MALFORMED_DATA_FROM_SERVER) {
//            return "Malformed data from server";
        } else {
            return "Unknown Error";
        }
    }

    private void resizeSurfaceViewInParent(SurfaceView sv, int width, int height) {
        // get the surface's parent
        View v = ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);
        Rect shRectangle = sv.getHolder().getSurfaceFrame();

        View parent = (View) sv.getParent();

        // window dimensions
        int wW = v.getWidth();
        int wH = v.getHeight();

        // surface's parent dimensions
        int pW = parent.getWidth();
        int pH = parent.getHeight();

        Log.i(TAG,
                String.format(
                        "Window Size: %dx%d     Surface Holder Rect: %dx%d    Parent Class: %s  %dx%d",
                        wW, wH, shRectangle.width(), shRectangle.height(), sv.getParent().getClass().getName(), parent.getWidth(),
                        parent.getHeight()));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sv.getLayoutParams());

        float contentAspect = ((float) width / (float) height);
        float surfaceAspect = ((float) pW / (float) pH);

        // ask player to calculate display rect
        if (mMediaPlayer != null) {
            Rect ur = mMediaPlayer.getDisplayRectForMaxDimensions(pW, pH);
            //Rect ur = MediaPlayer.getDisplayRectForMaxDimensionsAndVideoDimensions(pW, pH, width, height);
            if (ur != null) {
                lp.width = ur.width();
                lp.height = ur.height();
                lp.leftMargin = ur.left;
                lp.topMargin = ur.top;
                Log.i(TAG,
                        String.format(
                                "[Display] Max: %dx%d %f  |  Video Size: %dx%d  %f  |  Margins - left:%d  top:%d  right:%d  bottom:%d  size: %dx%d ",
                                pW, pH, surfaceAspect, width, height, contentAspect,
                                ur.left, ur.top, ur.right, ur.bottom,
                                ur.width(), ur.height())
                );
            }
        }

        // Commit the layout parameters
        sv.setLayoutParams(lp);
        Log.i(TAG, "resizeSurfaceViewInParent done");
        // Android 2.3 may also need the fixedSize property set to updated values
        //sv.getHolder().setFixedSize(lp.width+lp.leftMargin, lp.height+lp.topMargin);
    }

    private String screenSizeToString(int screenLayout) {
        int size = (screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);
        if (size == Configuration.SCREENLAYOUT_SIZE_SMALL)
            return "SMALL";
        else if (size == Configuration.SCREENLAYOUT_SIZE_NORMAL)
            return "NORMAL";
        else if (size == Configuration.SCREENLAYOUT_SIZE_LARGE)
            return "LARGE";
        else if (size == Configuration.SCREENLAYOUT_SIZE_XLARGE)
            return "X-LARGE";
        return "Unknown";
    }

    private void logDisplayAndBuildInfo() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Configuration config = getResources().getConfiguration();
        int smallestScreenWidthDp = (int) (dm.widthPixels / dm.density);

        Log.i("Display", String.format("Density: %f, Width: %dpx, Height: %dpx, Smallest Width: %ddp, Screen Size: %s",
                dm.density, dm.widthPixels, dm.heightPixels, smallestScreenWidthDp, screenSizeToString(config.screenLayout)));

        Log.w(TAG, String.format("INIT WITH Android (%s) Display: %s, Brand: %s, Manufacturer: %s, Model: %s, Product: %s, Hardware: %s",
                Build.VERSION.RELEASE, Build.DISPLAY, Build.BRAND, Build.MANUFACTURER, Build.MODEL, Build.PRODUCT, Build.HARDWARE));
    }

    public enum TrackOptionsType {
        AUDIO,
        SUBTITLES
    }

    private void showTrackOptions(TrackOptionsType optionsType) {
        if (mTracksDialog != null) {
            mTracksDialog.dismiss();
            mTracksDialog = null;
        }

        if (mMediaPlayer == null) return;

        if (mMediaPlayer.getState() < MediaPlayer.PLAYER_STATE_PREPARED) {
            Log.w(TAG, "MediaPlayer is not yet prepared, cannot show track options");
            return;
        }

        Vector<UplynkTrackInfo> tracks = null;
        if (optionsType == TrackOptionsType.AUDIO) {
            tracks = mMediaPlayer.getAudioTrackOptions();
        } else {
            tracks = mMediaPlayer.getSubtitleTrackOptions();
        }

        if (tracks == null || tracks.size() == 0) {
            Log.w(TAG, "MediaPlayer is not reporting track options");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (optionsType == TrackOptionsType.AUDIO) {
            builder.setTitle(R.string.audioChanDialogTitle);
        } else {
            builder.setTitle(R.string.subtitleDialogTitle);
            //builder.setTitle(R.string.subtitleDialogTitle);
        }
        UplynkTrackAdapter uplynkTrackAdapter = new UplynkTrackAdapter(this, android.R.layout.simple_list_item_1, tracks);
        builder.setAdapter(uplynkTrackAdapter, this);
        builder.setNegativeButton("Cancel", null);
        builder.setInverseBackgroundForced(true);

        mDialogType = optionsType;
        mTracksDialog = builder.create();
        // show the dialog
        mTracksDialog.show();
    }

    private void createMediaPlayerAfterDelay(final String url, final int delay) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        playContent(url);
                    }
                });

            }
        }).start();
    }

    private void restartMedia() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        //this will invalidate the surface
        mSurfaceHolder.setFormat(android.graphics.PixelFormat.TRANSPARENT);
        //this will re-initialize the surface
        mSurfaceHolder.setFormat(android.graphics.PixelFormat.OPAQUE);

        createMediaPlayerAfterDelay(mUrlToPlay, 500);
    }

    public void showToast(final String message) {
        final Activity ctx = this;

        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
