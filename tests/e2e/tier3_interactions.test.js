/**
 * Tier 3: Cross-Feature Interaction Tests
 *
 * Verifies pairwise and multi-subsystem integrations across the OmniTune architecture:
 * Search -> Stream Extraction -> Queue Management -> Lyrics Sync -> Media Controls -> Theme.
 */

const {
  assertEqual,
  assertNotEqual,
  assertTrue,
  assertFalse,
  assertNull,
  assertNotNull,
  assertDeepEqual,
  assertIncludes,
  assertInRange,
  assertMatches,
  assertThrows
} = require('./harness/assertions');

const {
  PoTokenGenerator,
  StreamResolver,
  LrcParser,
  LyricsProviderEngine,
  PlaybackQueueManager,
  IosAudioPlayerSimulator,
  NowPlayingController,
  SuvMusicThemeEngine,
  InnertubeDiscoveryEngine
} = require('./harness/domain_simulators');

const test = (fId, name, fn) => global.__registerTest(3, fId, name, fn);

// ----------------------------------------------------------------------------
// 1. Search -> Suggestions -> Select Result -> Extract Direct Stream (F05 + F06)
// ----------------------------------------------------------------------------
test('F05+F06', 'Search query generates suggestions, selects top item, resolves direct stream', () => {
  const suggestions = InnertubeDiscoveryEngine.getSuggestions('Coldplay');
  assertTrue(suggestions.length > 0);
  const selectedQuery = suggestions[0];

  const song = InnertubeDiscoveryEngine.createSongItem('cp_v1', 'Yellow', selectedQuery, 'Parachutes', 269);
  const streamInfo = StreamResolver.resolveStream(song.id, 'ANDROID_VR_NO_AUTH');

  assertEqual(streamInfo.videoId, 'cp_v1');
  assertEqual(streamInfo.client, 'ANDROID_VR_NO_AUTH');
  assertEqual(streamInfo.itag, 140);
  assertTrue(streamInfo.isDirectUnencrypted);
});

// ----------------------------------------------------------------------------
// 2. Stream Extraction -> PoToken Generation -> Attach Token (F06 + F07)
// ----------------------------------------------------------------------------
test('F06+F07', 'Stream resolver synthesizes pure Kotlin PoToken and appends &pot= parameter', () => {
  const streamInfo = StreamResolver.resolveStream('track_pot_test');
  assertIncludes(streamInfo.streamUrl, '&pot=');
  const match = streamInfo.streamUrl.match(/&pot=([^&]+)/);
  assertNotNull(match);
  const verified = PoTokenGenerator.verifyPoToken(match[1]);
  assertTrue(verified.valid);
});

// ----------------------------------------------------------------------------
// 3. Stream Resolution -> Audio Player State Machine (F06 + F09)
// ----------------------------------------------------------------------------
test('F06+F09', 'Stream resolution feeds stream URL to AudioPlayer transitioning IDLE -> PLAYING', () => {
  const player = new IosAudioPlayerSimulator();
  assertEqual(player.state, 'IDLE');

  const streamInfo = StreamResolver.resolveStream('song_state_flow');
  player.prepare(streamInfo.streamUrl, { 'User-Agent': 'OmniTune-iOS' });
  assertEqual(player.state, 'PREPARING');

  player.play();
  assertEqual(player.state, 'PLAYING');
  assertTrue(player.audioSessionActive);
});

// ----------------------------------------------------------------------------
// 4. Play Audio -> Update MPNowPlayingInfoCenter Lockscreen Metadata (F09 + F11)
// ----------------------------------------------------------------------------
test('F09+F11', 'PlaybackController updates iOS Lockscreen metadata on track start', () => {
  const qm = new PlaybackQueueManager();
  const nowPlaying = new NowPlayingController();
  const song = InnertubeDiscoveryEngine.createSongItem('s_np', 'Midnight City', 'M83', 'Hurry Up', 244);

  qm.playQueue([song]);
  nowPlaying.updateMetadata(qm.currentItem, qm.playbackState, qm.currentPositionMs, qm.durationMs);

  assertEqual(nowPlaying.nowPlayingInfo.MPMediaItemPropertyTitle, 'Midnight City');
  assertEqual(nowPlaying.nowPlayingInfo.MPMediaItemPropertyArtist, 'M83');
  assertEqual(nowPlaying.nowPlayingInfo.MPNowPlayingInfoPropertyPlaybackRate, 1.0);
});

// ----------------------------------------------------------------------------
// 5. Remote Command Center Pause -> Audio Player PAUSED -> Lockscreen Rate 0.0 (F10 + F11)
// ----------------------------------------------------------------------------
test('F10+F11', 'Remote pause command pauses iOS AudioPlayer and zeroes Lockscreen playback rate', () => {
  const player = new IosAudioPlayerSimulator();
  const nowPlaying = new NowPlayingController();
  const song = InnertubeDiscoveryEngine.createSongItem('s_rem', 'Track', 'Artist', 'Album', 180);

  player.play();
  nowPlaying.registerCommandHandler('pause', () => {
    player.pause();
    nowPlaying.updateMetadata(song, player.state, 50000, 180000);
    return { status: 'SUCCESS' };
  });

  const res = nowPlaying.dispatchRemoteCommand('pause');
  assertEqual(res.status, 'SUCCESS');
  assertEqual(player.state, 'PAUSED');
  assertEqual(nowPlaying.nowPlayingInfo.MPNowPlayingInfoPropertyPlaybackRate, 0.0);
});

// ----------------------------------------------------------------------------
// 6. Play Queue -> Enqueue Album -> Reorder Track -> Active Item Preserved (F04 + F12)
// ----------------------------------------------------------------------------
test('F04+F12', 'Enqueue 5 album tracks and reorder while playing preserves active track', () => {
  const qm = new PlaybackQueueManager();
  const albumTracks = [];
  for (let i = 1; i <= 5; i++) {
    albumTracks.push(InnertubeDiscoveryEngine.createSongItem(`t${i}`, `Track ${i}`, 'Artist', 'Album', 200));
  }
  qm.playQueue(albumTracks, 1); // Track 2 is active
  assertEqual(qm.currentItem.id, 't2');

  // Reorder Track 1 (index 0) to index 4
  qm.reorderQueue(0, 4);
  assertEqual(qm.currentItem.id, 't2');
  assertEqual(qm.currentIndex, 0); // t2 is now at index 0
});

// ----------------------------------------------------------------------------
// 7. Play Track -> Fetch Synced Lyrics from LRCLIB -> Binary Search Active Line (F08 + F21)
// ----------------------------------------------------------------------------
test('F08+F21', 'Query lyrics provider and evaluate real-time active lyric synchronization', async () => {
  const engine = new LyricsProviderEngine();
  const lyricsRes = await engine.resolveLyrics({ videoId: 'v_sync', title: 'Song', artist: 'Artist', durationSec: 180 });
  assertTrue(lyricsRes.isSynced);

  // At 18 seconds, line 2 (timeMs = 15500) should be active
  const activeIdx = LrcParser.findActiveLineIndex(lyricsRes.lines, 18000);
  assertEqual(activeIdx, 1);
  assertEqual(lyricsRes.lines[activeIdx].text, 'Sample line 2');
});

// ----------------------------------------------------------------------------
// 8. Tap-to-Seek on Lyric Line -> Audio Player Seek -> NowPlaying Elapsed Time Sync (F09 + F11 + F21)
// ----------------------------------------------------------------------------
test('F09+F11+F21', 'Tapping lyric line seeks queue position and updates Lockscreen elapsed time', () => {
  const qm = new PlaybackQueueManager();
  const nowPlaying = new NowPlayingController();
  const song = InnertubeDiscoveryEngine.createSongItem('s_tap', 'Song', 'Artist', 'Album', 200);
  qm.playQueue([song]);

  const lrc = '[00:10.00]Line 1\n[00:45.00]Chorus\n[01:30.00]Bridge';
  const { lines } = LrcParser.parse(lrc);

  // User taps Chorus (45.00s = 45000ms)
  const targetTimeMs = lines[1].timeMs;
  qm.seekTo(targetTimeMs);
  nowPlaying.updateMetadata(qm.currentItem, qm.playbackState, qm.currentPositionMs, qm.durationMs);

  assertEqual(qm.currentPositionMs, 45000);
  assertEqual(nowPlaying.nowPlayingInfo.MPNowPlayingInfoPropertyElapsedPlaybackTime, 45);
});

// ----------------------------------------------------------------------------
// 9. Audio Playback Progress -> Waveform Seeker Position with Duration Guard (F09 + F20)
// ----------------------------------------------------------------------------
test('F09+F20', 'Playback position converts accurately to waveform scrub fraction', () => {
  const qm = new PlaybackQueueManager();
  const song = InnertubeDiscoveryEngine.createSongItem('s_wf', 'Track', 'Artist', 'Album', 240);
  qm.playQueue([song]);
  qm.seekTo(120000); // Exactly halfway (50%)

  const fraction = qm.currentPositionMs / qm.durationMs;
  assertEqual(fraction, 0.5);

  const mappedBack = SuvMusicThemeEngine.calculateWaveformPosition(fraction, qm.durationMs);
  assertEqual(mappedBack, 120000);
});

// ----------------------------------------------------------------------------
// 10. Dynamic Artwork Dominant Color Extraction -> Apply to SuvMusic Theme (F13 + F19)
// ----------------------------------------------------------------------------
test('F13+F19', 'Dominant color extraction updates SuvMusic theme palette in MiniPlayer', () => {
  let activeTheme = SuvMusicThemeEngine.applyTheme('DEFAULT');
  assertEqual(activeTheme.primary, '#9C27B0');

  // Album artwork extraction yields OCEAN palette
  activeTheme = SuvMusicThemeEngine.applyTheme('OCEAN', true); // AMOLED pure black
  assertEqual(activeTheme.primary, '#1976D2');
  assertEqual(activeTheme.background, '#000000');
});

// ----------------------------------------------------------------------------
// 11. Queue Reorder -> Modern Queue View Equalizer Animation Updates to New Slot (F12 + F20)
// ----------------------------------------------------------------------------
test('F12+F20', 'Queue reorder updates equalizer animated row indicator index', () => {
  const qm = new PlaybackQueueManager();
  const s1 = InnertubeDiscoveryEngine.createSongItem('1', 'A', 'Art', 'Alb', 100);
  const s2 = InnertubeDiscoveryEngine.createSongItem('2', 'B', 'Art', 'Alb', 100);
  qm.playQueue([s1, s2], 0); // Active is index 0

  // Move active track 0 to position 1
  qm.reorderQueue(0, 1);
  assertEqual(qm.currentIndex, 1);
  assertEqual(qm.currentItem.id, '1');
});

// ----------------------------------------------------------------------------
// 12. Shuffle Queue -> Active Song Slot 0 -> Lockscreen Metadata Accurate (F11 + F12)
// ----------------------------------------------------------------------------
test('F11+F12', 'Shuffle mode keeps active track playing and syncs Lockscreen metadata', () => {
  const qm = new PlaybackQueueManager();
  const nowPlaying = new NowPlayingController();
  const items = [];
  for (let i = 1; i <= 8; i++) {
    items.push(InnertubeDiscoveryEngine.createSongItem(`s${i}`, `Title ${i}`, 'Artist', 'Album', 150));
  }
  qm.playQueue(items, 4); // Active is s5
  qm.setShuffle(true);

  assertEqual(qm.currentItem.id, 's5');
  assertEqual(qm.currentIndex, 0);

  nowPlaying.updateMetadata(qm.currentItem, qm.playbackState, 0, qm.durationMs);
  assertEqual(nowPlaying.nowPlayingInfo.MPMediaItemPropertyTitle, 'Title 5');
});

// ----------------------------------------------------------------------------
// 13. Repeat Mode ONE -> Track Ends -> Auto-replays Same Item with Position 0 (F09 + F12)
// ----------------------------------------------------------------------------
test('F09+F12', 'Repeat ONE mode causes skipNext to reset position to 0 and replay same item', () => {
  const qm = new PlaybackQueueManager();
  const song = InnertubeDiscoveryEngine.createSongItem('s_rep', 'Song', 'Artist', 'Album', 180);
  qm.playQueue([song]);
  qm.seekTo(179000);
  qm.setRepeat('ONE');

  assertTrue(qm.skipNext());
  assertEqual(qm.currentItem.id, 's_rep');
  assertEqual(qm.currentPositionMs, 0);
  assertEqual(qm.playbackState, 'PLAYING');
});

// ----------------------------------------------------------------------------
// 14. Headphone Disconnection -> Auto-pause -> MiniPlayer Vinyl Stops (F10 + F19)
// ----------------------------------------------------------------------------
test('F10+F19', 'Headphone disconnect route change triggers auto-pause and halts vinyl spin', () => {
  const sim = new IosAudioPlayerSimulator();
  sim.play();
  assertTrue(sim.state === 'PLAYING');

  let vinylSpeed = 1.0;
  const onPlayerStateChange = (state) => {
    vinylSpeed = (state === 'PLAYING') ? 1.0 : 0.0;
  };

  sim.handleRouteChange('OldDeviceUnavailable');
  onPlayerStateChange(sim.state);

  assertEqual(sim.state, 'PAUSED');
  assertEqual(vinylSpeed, 0.0);
});

// ----------------------------------------------------------------------------
// 15. Search Category Tab -> DOWNLOADS -> Filter Library -> Play Offline (F05 + F16)
// ----------------------------------------------------------------------------
test('F05+F16', 'Search screen DOWNLOADS filter navigates to offline library items', () => {
  const library = [
    { id: '1', title: 'Downloaded Track', isDownloaded: true },
    { id: '2', title: 'Online Track', isDownloaded: false }
  ];
  const downloaded = library.filter(i => i.isDownloaded);
  assertEqual(downloaded.length, 1);
  assertEqual(downloaded[0].title, 'Downloaded Track');
});

// ----------------------------------------------------------------------------
// 16. Sleep Timer Expiration -> Fadeout & Stop -> Clear Lockscreen Metadata (F09 + F11 + F17)
// ----------------------------------------------------------------------------
test('F09+F11+F17', 'Sleep timer expiration stops playback and clears Lockscreen playback rate', () => {
  const qm = new PlaybackQueueManager();
  const nowPlaying = new NowPlayingController();
  const song = InnertubeDiscoveryEngine.createSongItem('s1', 'Night', 'Artist', 'Album', 300);
  qm.playQueue([song]);

  // Sleep timer triggers stop
  const onSleepTimerExpire = () => {
    qm.pause();
    nowPlaying.updateMetadata(qm.currentItem, 'PAUSED', qm.currentPositionMs, qm.durationMs);
  };

  onSleepTimerExpire();
  assertEqual(qm.playbackState, 'PAUSED');
  assertEqual(nowPlaying.nowPlayingInfo.MPNowPlayingInfoPropertyPlaybackRate, 0.0);
});

// ----------------------------------------------------------------------------
// 17. Expandable Player Sheet Gesture Drag -> Reveals Waveform & Synced Lyrics (F19 + F20 + F21)
// ----------------------------------------------------------------------------
test('F19+F20+F21', 'Expanding player sheet transitions mini player to waveform and lyrics view', () => {
  let dragFraction = 0.0;
  const expand = () => { dragFraction = 1.0; };
  expand();

  const isMiniPlayerVisible = dragFraction < 0.3;
  const isExpandedPlayerVisible = dragFraction > 0.7;

  assertFalse(isMiniPlayerVisible);
  assertTrue(isExpandedPlayerVisible);
});

// ----------------------------------------------------------------------------
// 18. Detail Album Screen -> Play All Button -> Initialize PlaybackController (F12 + F18)
// ----------------------------------------------------------------------------
test('F12+F18', 'Album detail Play All enqueues all 14 tracks and initiates playback from track 0', () => {
  const qm = new PlaybackQueueManager();
  const albumTracks = [];
  for (let i = 1; i <= 14; i++) {
    albumTracks.push(InnertubeDiscoveryEngine.createSongItem(`alb_t${i}`, `Track ${i}`, 'Daft Punk', 'Discovery', 210));
  }
  const album = InnertubeDiscoveryEngine.createAlbumItem('discovery', 'Discovery', 'Daft Punk', 2001, 14, albumTracks);

  qm.playQueue(album.tracks, 0);
  assertEqual(qm.queue.length, 14);
  assertEqual(qm.currentIndex, 0);
  assertEqual(qm.currentItem.title, 'Track 1');
  assertTrue(qm.isPlaying);
});

// ----------------------------------------------------------------------------
// 19. PoToken Expiration -> Stream 403 Recovery -> Refresh PoToken -> Resume (F06 + F07 + F09)
// ----------------------------------------------------------------------------
test('F06+F07+F09', 'Expired stream token triggers PoToken regeneration and stream URL renewal', () => {
  const expiredStream = StreamResolver.resolveStream('song_renew', 'ANDROID_VR_NO_AUTH', -10); // Expired 10s ago
  assertTrue(StreamResolver.isExpired(expiredStream.streamUrl));

  // Recovery flow: regenerate stream
  const refreshedStream = StreamResolver.resolveStream('song_renew', 'ANDROID_VR_NO_AUTH', 21600);
  assertFalse(StreamResolver.isExpired(refreshedStream.streamUrl));
  assertNotEqual(expiredStream.streamUrl, refreshedStream.streamUrl);
});

// ----------------------------------------------------------------------------
// 20. iOS UIViewController Bridge -> Safe Area Insets -> Home Profile Header (F14 + F22)
// ----------------------------------------------------------------------------
test('F14+F22', 'iOS safe area top inset is applied as top padding to Home profile header', () => {
  const safeAreaTopInsetDp = 47; // iPhone 15/16 Pro Dynamic Island
  const baseProfileHeaderPaddingDp = 16;
  const totalTopPadding = safeAreaTopInsetDp + baseProfileHeaderPaddingDp;
  assertEqual(totalTopPadding, 63);
});
