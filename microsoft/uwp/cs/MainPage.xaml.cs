using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Windows.Foundation.Collections;
using Windows.Media.Core;
using Windows.Media.Playback;
using Windows.Media.Protection;
using Windows.Media.Protection.PlayReady;
using Windows.Media.Streaming.Adaptive;
using Windows.Storage.Streams;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Navigation;
using Windows.Web.Http;

namespace SamplePlayer
{
    public sealed partial class MainPage : Page
    {
        private const uint MSPR_E_CONTENT_ENABLING_ACTION_REQUIRED = 0x8004B895;

        private readonly HashSet<string> requestedPlayReadyHeaders = new HashSet<string>();
        private readonly List<PlayReadyContentHeader> parsedPlayReadyHeaders = new List<PlayReadyContentHeader>();

        private AdaptiveMediaSource adaptiveMediaSource;
        private MediaPlayer mediaPlayer;
        private PlayReadyLicenseSession licenseSession;

        public MainPage()
        {
            InitializeComponent();
        }

        protected override async void OnNavigatedTo(NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);

            var protectionManager = CreateMediaProtectionManager();

            licenseSession = CreateLicenseSession();
            licenseSession.ConfigureMediaProtectionManager(protectionManager);

            mediaPlayer = new MediaPlayer { AutoPlay = true };
            mediaPlayer.MediaFailed += MediaPlayer_MediaFailed;
            mediaPlayer.ProtectionManager = protectionManager;

            MediaPlayerElement.SetMediaPlayer(mediaPlayer);

            var url = "<STREAM_URL>";
            var uri = new Uri(url);

            (IInputStream inputStream, string contentType) = await ParseMasterPlaylist(uri);
            var result = await AdaptiveMediaSource.CreateFromStreamAsync(inputStream, uri, contentType);
            switch (result.Status)
            {
                case AdaptiveMediaSourceCreationStatus.Success:
                    adaptiveMediaSource = result.MediaSource;

                    adaptiveMediaSource.Diagnostics.DiagnosticAvailable += DiagnosticAvailable;
                    adaptiveMediaSource.DownloadRequested += DownloadRequested;

                    adaptiveMediaSource.InitialBitrate = adaptiveMediaSource.AvailableBitrates.ElementAt(adaptiveMediaSource.AvailableBitrates.Count() / 2);
                    Debug.WriteLine($"Initial Bitrate: {adaptiveMediaSource.InitialBitrate}");

                    adaptiveMediaSource.PlaybackBitrateChanged += AdaptiveMediaSource_PlaybackBitrateChanged;
                    adaptiveMediaSource.DownloadBitrateChanged += AdaptiveMediaSource_DownloadBitrateChanged;

                    var mediaSource = MediaSource.CreateFromAdaptiveMediaSource(adaptiveMediaSource);
                    var mediaPlaybackItem = new MediaPlaybackItem(mediaSource);
                    mediaPlayer.Source = mediaPlaybackItem;
                    break;

                default:
                    Debug.WriteLine(result.ExtendedError);
                    break;
            }
        }

        private void AdaptiveMediaSource_DownloadBitrateChanged(AdaptiveMediaSource sender, AdaptiveMediaSourceDownloadBitrateChangedEventArgs args)
        {
            Debug.WriteLine($"Download Bitrate Changed: {args.OldValue} -> {args.NewValue}");
        }

        private void AdaptiveMediaSource_PlaybackBitrateChanged(AdaptiveMediaSource sender, AdaptiveMediaSourcePlaybackBitrateChangedEventArgs args)
        {
            Debug.WriteLine($"Playback Bitrate Changed: {args.OldValue} -> {args.NewValue}");
        }

        private async Task<(IInputStream, string)> ParseMasterPlaylist(Uri uri)
        {
            var client = new HttpClient();
            using (var request = new HttpRequestMessage(HttpMethod.Get, uri))
            using (var response = await client.SendRequestAsync(request))
            {
                string contentType = response.Content.Headers.ContentType.MediaType;

                if (response.IsSuccessStatusCode)
                {
                    var contents = await response.Content.ReadAsStringAsync();
                    var headers = ParseHlsManifestForPlayReadyData(contents);

                    foreach (var header in headers)
                    {
                        byte[] data = Convert.FromBase64String(header);
                        var contentHeader = new PlayReadyContentHeader(data);
                        parsedPlayReadyHeaders.Add(contentHeader);

                        var licenseRequest = licenseSession.CreateLAServiceRequest();
                        licenseRequest.ContentHeader = contentHeader;
                        await licenseRequest.BeginServiceRequest();
                    }
                }

                var inputStream = await response.Content.ReadAsInputStreamAsync();
                return (inputStream, contentType);
            }
        }

        private void DiagnosticAvailable(AdaptiveMediaSourceDiagnostics sender, AdaptiveMediaSourceDiagnosticAvailableEventArgs args)
        {
            Debug.WriteLine($"{args.DiagnosticType} - {args.ResourceType}: {args.ResourceUri}: {args.ExtendedError}");
        }

        private async Task ReactiveIndividualizationRequestAsync(PlayReadyIndividualizationServiceRequest individualizationRequest)
        {
            try
            {
                await individualizationRequest.BeginServiceRequest();
            }
            catch (Exception x) when (((uint)x.HResult == MSPR_E_CONTENT_ENABLING_ACTION_REQUIRED))
            {
                individualizationRequest.NextServiceRequest();
            }
            catch (Exception e)
            {
                Debug.WriteLine(e);
            }
        }

        private Task ProActiveIndividualizationRequestAsync()
        {
            var individualizationRequest = new PlayReadyIndividualizationServiceRequest();
            return ReactiveIndividualizationRequestAsync(individualizationRequest);
        }

        private MediaProtectionManager CreateMediaProtectionManager()
        {
            var protectionManager = new MediaProtectionManager();
            protectionManager.ComponentLoadFailed += ProtectionManager_ComponentLoadFailed;
            protectionManager.ServiceRequested += ProtectionManager_ServiceRequested;

            var contentProtectionSystems = new PropertySet();
            contentProtectionSystems.Add(
                "{F4637010-03C3-42CD-B932-B48ADF3A6A54}",
                "Windows.Media.Protection.PlayReady.PlayReadyWinRTTrustedInput"
            );

            protectionManager.Properties.Add(
                "Windows.Media.Protection.MediaProtectionSystemIdMapping",
                contentProtectionSystems
            );

            protectionManager.Properties.Add(
                "Windows.Media.Protection.MediaProtectionSystemId",
                "{F4637010-03C3-42CD-B932-B48ADF3A6A54}"
            );

            protectionManager.Properties.Add(
                "Windows.Media.Protection.MediaProtectionContainerGuid",
                "{9A04F079-9840-4286-AB92-E65BE0885F95}"
            );

            return protectionManager;
        }

        private void ProtectionManager_ComponentLoadFailed(MediaProtectionManager sender, ComponentLoadFailedEventArgs e)
        {
            e.Completion.Complete(false);
            Debug.WriteLine($"ProtectionManager_ComponentLoadFailed - {e.Information}");
        }

        private async void ProtectionManager_ServiceRequested(MediaProtectionManager sender, ServiceRequestedEventArgs e)
        {
            try
            {
                if (e.Request is PlayReadyIndividualizationServiceRequest individualizationRequest)
                {
                    await ReactiveIndividualizationRequestAsync(individualizationRequest);
                }
                else if (e.Request is PlayReadyLicenseAcquisitionServiceRequest licenseRequest)
                {
                    if (licenseRequest.Uri == null)
                    {
                        // License Uri is null so try to find a previously parsed PlayReadyContentHeader
                        // that has the same KeyIds as this licese request

                        var keyIdsSet = new HashSet<string>(licenseRequest.ContentHeader.KeyIdStrings);
                        foreach (PlayReadyContentHeader contentHeader in parsedPlayReadyHeaders)
                        {
                            var headerKeySet = new HashSet<string>(contentHeader.KeyIdStrings);
                            if (keyIdsSet.SetEquals(headerKeySet))
                            {
                                var request = licenseSession.CreateLAServiceRequest();
                                request.ContentHeader = contentHeader;
                                await request.BeginServiceRequest();
                                break;
                            }
                        }
                    }
                    else
                    {
                        await licenseRequest.BeginServiceRequest();
                    }
                }
                
                e.Completion.Complete(true);
            }
            catch
            {
                e.Completion.Complete(false);
            }
        }

        private void MediaPlayer_MediaFailed(MediaPlayer sender, MediaPlayerFailedEventArgs args)
        {
            Debug.WriteLine($"MediaPlayer_MediaFailed - {args.Error}: {args.ExtendedErrorCode.Message}");
        }

        private PlayReadyLicenseSession CreateLicenseSession()
        {
            var contentProtectionSystems = new PropertySet();
            contentProtectionSystems.Add(
                "{F4637010-03C3-42CD-B932-B48ADF3A6A54}",
                "Windows.Media.Protection.PlayReady.PlayReadyWinRTTrustedInput"
            );

            var mediaProtectionInfo = new PropertySet();
            mediaProtectionInfo.Add(
                "Windows.Media.Protection.MediaProtectionSystemId",
                "{F4637010-03C3-42CD-B932-B48ADF3A6A54}"
            );

            mediaProtectionInfo.Add(
                "Windows.Media.Protection.MediaProtectionSystemIdMapping",
                contentProtectionSystems
            );

            mediaProtectionInfo.Add(
                "Windows.Media.Protection.MediaProtectionContainerGuid",
                "{9A04F079-9840-4286-AB92-E65BE0885F95}"
            );

            var licenseSessionProperties = new PropertySet();
            licenseSessionProperties.Add(
                "Windows.Media.Protection.MediaProtectionPMPServer",
                new MediaProtectionPMPServer(mediaProtectionInfo)
            );

            return new PlayReadyLicenseSession(licenseSessionProperties);
        }

        private async void DownloadRequested(AdaptiveMediaSource sender, AdaptiveMediaSourceDownloadRequestedEventArgs args)
        {
            var deferral = args.GetDeferral();

            var client = new HttpClient();
            using (var request = new HttpRequestMessage(HttpMethod.Get, args.ResourceUri))
            using (var response = await client.SendRequestAsync(request))
            {
                if (response.IsSuccessStatusCode)
                {
                    if (args.ResourceType == AdaptiveMediaSourceResourceType.InitializationSegment)
                    {
                        args.Result.ContentType = "video/mp4";
                    }
                    else if (args.ResourceType == AdaptiveMediaSourceResourceType.Manifest)
                    {
                        if (args.ResourceUri.AbsolutePath.ToLowerInvariant().EndsWith(".m3u8"))
                        {
                            var contents = await response.Content.ReadAsStringAsync();
                            var headers = ParseHlsManifestForPlayReadyData(contents);

                            foreach (var header in headers)
                            {
                                if (!requestedPlayReadyHeaders.Contains(header))
                                {
                                    requestedPlayReadyHeaders.Add(header);
                                    byte[] data = Convert.FromBase64String(header);

                                    var licenseRequest = licenseSession.CreateLAServiceRequest();
                                    licenseRequest.ContentHeader = new PlayReadyContentHeader(data);

                                    await licenseRequest.BeginServiceRequest();
                                }
                            }
                        }
                    }
                }

                args.Result.InputStream = await response.Content.ReadAsInputStreamAsync();
            }

            deferral.Complete();
        }

        private IEnumerable<string> ParseHlsManifestForPlayReadyData(string hlsContent)
        {
            const string KEY_TAG = "#EXT-X-KEY:";
            const string SESSION_KEY_TAG = "#EXT-X-SESSION-KEY:";

            using (var reader = new StringReader(hlsContent))
            {
                string line = null;
                while((line = reader.ReadLine()) != null)
                {
                    if (line.StartsWith(KEY_TAG))
                    {
                        line = line.Substring(KEY_TAG.Length);
                    }
                    else if (line.StartsWith(SESSION_KEY_TAG))
                    {
                        line = line.Substring(SESSION_KEY_TAG.Length);
                    }
                    else
                    {
                        continue;
                    }

                    var attributes = SplitString(line);
                    string uri = null;

                    if (attributes.TryGetValue("URI", out uri))
                    {
                        if (uri.Contains("base64,"))
                        {
                            var base64Data = uri.Split("base64,")[1];
                            yield return base64Data;
                        }
                    }
                }
            }
        }

        private static Dictionary<string, string> SplitString(string value)
        {
            return GetKeyValuePairs(value).ToDictionary(kvp => kvp.Key, kvp => kvp.Value, StringComparer.Ordinal);
        }

        private static IEnumerable<KeyValuePair<string, string>> GetKeyValuePairs(string line)
        {
            // given a string: 'PROGRAM-ID=1,RESOLUTION=416x234,BANDWIDTH=420772'
            // splits on ',' and then on '='

            foreach (string keyValuePair in GetPairs(line))
            {
                // can't use Split('=') here because some values
                // have an '=' in them, like urls.
                int equalsIndex = keyValuePair.IndexOf('=');
                if (equalsIndex >= 0)
                {
                    string key = keyValuePair.Substring(0, equalsIndex);
                    string value = keyValuePair.Substring(equalsIndex + 1).Trim('"');

                    yield return new KeyValuePair<string, string>(key, value);
                }
            }
        }

        private static IEnumerable<string> GetPairs(string value)
        {
            // breaks up a string like 'PROGRAM-ID=1,RESOLUTION=416x234,CODECS="mp3,aac",BANDWIDTH=420772'
            // into 4 parts: 'PROGRAM-ID=1'
            //               'RESOLUTION=416x234'
            //               'CODECS="mp3,aac"'
            //               'BANDWIDTH=420772'

            while (true)
            {
                int firstCommaIndex = value.IndexOf(',');

                if (firstCommaIndex < 0)
                {
                    yield return value;
                    break;
                }

                int firstQuoteIndex = value.IndexOf('"');

                if (firstQuoteIndex < 0 || firstCommaIndex < firstQuoteIndex)
                {
                    yield return value.Substring(0, firstCommaIndex);
                    value = value.Substring(firstCommaIndex + 1);
                }
                else
                {
                    int nextQuoteIndex = value.IndexOf('"', firstQuoteIndex + 1);
                    int nextCommaIndex = value.IndexOf(',', nextQuoteIndex + 1);

                    if (nextCommaIndex < 0)
                    {
                        // last pair in string
                        yield return value;
                        break;
                    }

                    yield return value.Substring(0, nextCommaIndex);
                    value = value.Substring(nextCommaIndex + 1);
                }
            }
        }
    }
}
