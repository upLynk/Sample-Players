using System;
using System.Diagnostics;
using Uplynk.MediaSource;

namespace SamplePlayer
{
    public sealed partial class MainPage
    {
        private UplynkMediaSource uplynkMediaSource;

        public MainPage()
        {
            InitializeComponent();
            InitializeAdaptiveMediaSource(new Uri("http://content.uplynk.com/52ab86c3f6c74d6abf7f162204d51609.m3u8"));
        }

        private async void InitializeAdaptiveMediaSource(Uri uri)
        {
            UplynkMediaSourceCreationResult result = await UplynkMediaSource.CreateFromUriAsync(uri);

            if (result.Status == UplynkMediaSourceCreationStatus.Success)
            {
                uplynkMediaSource = result.UplynkMediaSource;
                uplynkMediaSource.AssetEntered += UplynkMediaSource_AssetEntered;
                uplynkMediaSource.SegmentEntered += UplynkMediaSource_SegmentEntered;

                mediaElementPlayer.SetPlaybackSource(uplynkMediaSource.MediaPlaybackItem);
            }
        }
        private void UplynkMediaSource_AssetEntered(UplynkMediaSource sender, AssetEnteredEventArgs args)
        {
            Debug.WriteLine($"Now playing: {args.Asset.Description}");
        }

        private void UplynkMediaSource_SegmentEntered(UplynkMediaSource sender, SegmentEnteredEventArgs args)
        {
            Debug.WriteLine($"{args.Segment.Asset.Description}: {args.Segment.StartTime} -> {args.Segment.EndTime}");
        }
    }
}
