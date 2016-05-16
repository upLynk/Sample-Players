(function () {
    'use strict';

    var app = WinJS.Application;
    var activation = Windows.ApplicationModel.Activation;

    var url = null;
    var uplynk = Uplynk.MediaSource;
    var mediaSource = null;
    var mediaPlaybackItem = null;
    var uplynkMediaSource = null;

    function attachMediaSource() {

        if (mediaPlaybackItem != null) {
            var vid = document.getElementById("video_player");

            var playlist = new Windows.Media.Playback.MediaPlaybackList();
            playlist.items.append(mediaPlaybackItem);

            vid.src = URL.createObjectURL(playlist, { oneTimeOnly: true });
        }
    }

    function onAssetEntered(event) {
        console.log("now playing: " + event.asset.description);
    }

    function getTimeString(milliseconds) {

        var time = new Date(milliseconds);
        var minutes = time.getMinutes();
        var seconds = time.getSeconds();

        if (minutes < 10) {
            minutes = "0" + minutes;
        }

        if (seconds < 10) {
            seconds = "0" + seconds;
        }

        return minutes + ":" + seconds;
    }

    function onSegmentEntered(event) {

        var segment = event.segment;

        var start = getTimeString(segment.startTime);
        var end = getTimeString(segment.endTime);

        console.log(segment.asset.description + ": " + start + " -> " + end);
    }

    function onMediaSourceCreated(result) {

        if (result.status === Uplynk.MediaSource.UplynkMediaSourceCreationStatus.success) {

            mediaSource = result.uplynkMediaSource.mediaSource;
            mediaPlaybackItem = result.uplynkMediaSource.mediaPlaybackItem;
            uplynkMediaSource = result.uplynkMediaSource;
            uplynkMediaSource.addEventListener("assetentered", onAssetEntered, false);
            uplynkMediaSource.addEventListener("segmententered", onSegmentEntered, false);

            attachMediaSource();
        }
    }

    function loadMediaSourceFromUri(urlString) {

        var vid = document.getElementById("video_player");

        url = new Windows.Foundation.Uri(urlString);

        mediaSource = null;
        vid.removeAttribute("src");

        WinJS.log && WinJS.log("Creating AdaptiveMediaSource for url: " + url, "sample", "status");

        uplynk.UplynkMediaSource.createFromUriAsync(url).done(
            function completed(result) {
                onMediaSourceCreated(result);
            });
    }

    app.onactivated = function (args) {
        if (args.detail.kind === activation.ActivationKind.launch) {

            args.setPromise(WinJS.UI.processAll().then(function () {
                loadMediaSourceFromUri("http://content.uplynk.com/52ab86c3f6c74d6abf7f162204d51609.m3u8");
            }));
        }
    };

    app.oncheckpoint = function (args) { };

    app.start();
}());
