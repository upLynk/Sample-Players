using Android.App;
using Android.Views;
using Android.Widget;
using Android.OS;
using Com.Uplynk.Media;
using Java.IO;

namespace SamplePlayer
{
    [Activity(Label = "SamplePlayer", MainLauncher = true, Icon = "@drawable/icon")]
    public class MainActivity : Activity,
                                MediaController.IMediaPlayerControl,
                                ISurfaceHolderCallback,
                                MediaPlayer.IOnErrorListener,
                                MediaPlayer.IOnPreparedListener,
                                MediaPlayer.IOnVideoSizeChangedListener,
                                View.IOnTouchListener,
                                ViewTreeObserver.IOnGlobalLayoutListener
    {
        private MediaPlayer _mp;
        private MediaController _mc;
        private SurfaceView _sv;
        private ISurfaceHolder _sh;
        private string _mediaUrl;

        //private int _currentSubtitleTrack = 0;
        //private int _currentAudioTrack = 0;

        private bool _svReady = false;
        private bool _error = false;
        private bool _mediaPrepared = false;

        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);

            WindowManagerLayoutParams lp = Window.Attributes;
            lp.Flags = WindowManagerFlags.Fullscreen;
            Window.Attributes = lp;

            SetContentView(Resource.Layout.Main);

            _sv = (SurfaceView)this.FindViewById(Resource.Id.surfaceView1);
            _sh = _sv.Holder;
            MediaPlayer.InitSurfaceHolder(_sh);
            _sh.AddCallback(this);

            _mediaUrl = "http://content.uplynk.com/52ab86c3f6c74d6abf7f162204d51609.m3u8";

            _sv.SetOnTouchListener(this);
        }

        public bool OnTouch(View v, MotionEvent e)
        {
            if (_mc != null && _mediaPrepared)
            {
                _mc.Show();
            }

            return true;
        }

        public void SurfaceChanged(ISurfaceHolder holder, Android.Graphics.Format format, int width, int height)
        {
            //surface changed
        }

        public void SurfaceCreated(ISurfaceHolder holder)
        {
            CreateMediaPlayer(_mediaUrl);
        }

        public void SurfaceDestroyed(ISurfaceHolder holder)
        {
            if (_mp != null)
            {
                if (_mp.IsPlaying)
                {
                    _mp.Stop();
                }

                _mp.Release();
                _mp = null;
            }
        }

        protected override void OnStart()
        {
            base.OnStart();
        }

        protected override void OnStop()
        {
            base.OnStop();

            if (_mp != null)
            {
                if (_mp.IsPlaying)
                {
                    _mp.Stop();
                }

                _mp.Release();
                _mp = null;
            }
        }

        private void CreateMediaPlayer(string url)
        {
            if (_mp != null)
            {
                _mp.Release();
                _mp = null;
            }

            // Create a new media player and set the listeners
            _mp = new MediaPlayer();
            _mp.SetContext(this);

            _mp.SetOnErrorListener(this);
            _mp.SetOnPreparedListener(this);
            _mp.SetAudioStreamType((int)Android.Media.Stream.Music);
            _mp.SetOnVideoSizeChangedListener(this);

            _mc = new MediaController(this);
            _mc.SetAnchorView((View)_sv.Parent);

            _mp.SetDisplay(_sh);
            _mp.SetScreenOnWhilePlaying(true);

            try
            {
                _mp.SetDataSource(url);
                _mp.PrepareAsync();
            }
            catch (IOException e)
            {
                e.PrintStackTrace();
            }
        }

        public void OnPrepared(MediaPlayer mp)
        {
            if (mp == _mp)
            {
                _mc.SetMediaPlayer(this);
                _mediaPrepared = true;

                if (_svReady)
                {
                    _mp.Start();
                }
            }
        }

        private string GetMediaError(int errorCode, int code2)
        {
            return "Unknown Error";
        }

        public bool OnError(MediaPlayer mp, int what, int extra)
        {
            if (mp != null)
            {
                mp.Stop();
                mp.Release();
                _mp = null;
            }

            _error = true;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.SetMessage("Error Playing Media: (" + what + ") " + GetMediaError(what, extra));

            AlertDialog alert = builder.Create();
            alert.Show();

            return true;
        }

        public bool CanPause()
        {
            return true;
        }

        public bool CanSeekBackward()
        {
            return true;
        }

        public bool CanSeekForward()
        {
            return true;
        }

        public void Pause()
        {
            if (_mp != null)
            {
                _mp.Pause();
            }
        }

        public void SeekTo(int position)
        {
            if (_mp != null)
            {
                _mp.SeekTo(position);
            }
        }

        public void Start()
        {
            if (_mp != null)
            {
                _mp.Start();
            }
        }

        public int AudioSessionId
        {
            get { return 0; }
        }

        public int BufferPercentage
        {
            get { return 0; }
        }

        public int CurrentPosition
        {
            get
            {
                if (_mp != null && !_error)
                {
                    return _mp.CurrentPosition;
                }

                return 0;
            }
        }

        public int Duration
        {
            get
            {
                if (_error || _mp == null)
                {
                    return 0;
                }

                return _mp.Duration;
            }
        }

        public bool IsPlaying
        {
            get
            {
                if (_mp == null)
                {
                    return false;
                }

                return _mp.IsPlaying;
            }
        }

        public override void OnConfigurationChanged(Android.Content.Res.Configuration newConfig)
        {
            base.OnConfigurationChanged(newConfig);

            View parent = (View)_sv.Parent;

            ViewTreeObserver observer = parent.ViewTreeObserver;
            observer.AddOnGlobalLayoutListener(this);
        }

        public void OnGlobalLayout()
        {
            View parent = (View)_sv.Parent;
            parent.ViewTreeObserver.RemoveOnGlobalLayoutListener(this);

            if (_mp != null)
            {
                ResizeSurfaceViewInParent(_sv, _mp.VideoWidth, _mp.VideoHeight);
            }
        }

        public void OnVideoSizeChanged(MediaPlayer mp, int width, int height)
        {
            ResizeSurfaceViewInParent(_sv, width, height);
        }

        private void ResizeSurfaceViewInParent(SurfaceView sv, int width, int height)
        {
            View v = ((ViewGroup)FindViewById(Android.Resource.Id.Content)).GetChildAt(0);
            Android.Graphics.Rect shrect = sv.Holder.SurfaceFrame;
            View parent = (View)sv.Parent;

            //window's dimensions
            int wW = v.Width;
            int wH = v.Height;

            //surface's parent dimensions
            int pW = parent.Width;
            int pH = parent.Height;

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sv.LayoutParameters);
            lp.TopMargin = 0;
            lp.LeftMargin = 0;

            float contentAspect = ((float)width / (float)height);
            float surfaceAspect = ((float)pW / (float)pH);

            if (contentAspect < surfaceAspect)
            {
                lp.Width = (int)System.Math.Ceiling(pH * contentAspect);
                lp.Height = pH;
                lp.LeftMargin = (int)((pW - lp.Width) / 2);
            }
            else
            {
                lp.Height = (int)System.Math.Ceiling(pW / contentAspect);
                lp.Width = pW;
                lp.TopMargin = (int)((pH - lp.Height) / 2);
            }

            sv.LayoutParameters = lp;
        }
    }
}

