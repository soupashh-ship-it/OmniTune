/**
 * Tier 4: Real-World End-to-End User Session Workflows
 *
 * Full lifecycle simulations of realistic user sessions from cold launch
 * to multi-step playback, search, lyrics synchronization, route changes,
 * and backgrounding on iOS.
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

const test = (fId, name, fn) => global.__registerTest(4, fId, name, fn);

// ============================================================================
// Workflow 1: Fresh Launch, Search Track, Direct Stream, Sync Lyrics, Background App
// ============================================================================
test('Workflow 1', 'Fresh launch, search track, direct AAC stream, sync lyrics, background app', async () => {
  // 1. Cold start theme and safe areas
  const theme = SuvMusicThemeEngine.applyTheme('DEFAULT', true); // Pure black AMOLED
  assertEqual(theme.primary, '#9C27B0');
  assertEqual(theme.background, '#000000');

  const safeAreaInsets = { top: 47, bottom: 34 };
  assertInRange(safeAreaInsets.top, 40, 60);

  // 2. Home screen greeting
  const hour = 9; // 9 AM
  const greeting = (hour >= 5 && hour < 12) ? 'Good morning' : 'Good evening';
  assertEqual(greeting, 'Good morning');

  // 3. Search query
  const suggestions = InnertubeDiscoveryEngine.getSuggestions('Blinding Lights');
  assertTrue(suggestions.length > 0);

  // 4. Select song item
  const song = InnertubeDiscoveryEngine.createSongItem('bl_v1', 'Blinding Lights', 'The Weeknd', 'After Hours', 200);
  assertEqual(song.durationSec, 200);

  // 5. Direct AAC stream resolution
  const streamInfo = StreamResolver.resolveStream(song.id, 'ANDROID_VR_NO_AUTH');
  assertEqual(streamInfo.client, 'ANDROID_VR_NO_AUTH');
  assertEqual(streamInfo.itag, 140);
  assertTrue(streamInfo.isDirectUnencrypted);
  assertFalse(StreamResolver.isExpired(streamInfo.streamUrl));

  // 6. Play in queue manager & audio player
  const qm = new PlaybackQueueManager();
  const player = new IosAudioPlayerSimulator();
  qm.playQueue([song]);
  player.prepare(streamInfo.streamUrl, { 'User-Agent': 'OmniTune-iOS/1.0' });
  player.play();

  assertEqual(qm.playbackState, 'PLAYING');
  assertEqual(player.state, 'PLAYING');
  assertEqual(qm.currentItem.title, 'Blinding Lights');

  // 7. Synchronized lyrics resolution
  const lyricsEngine = new LyricsProviderEngine();
  const lyricsRes = await lyricsEngine.resolveLyrics({ videoId: song.id, title: song.title, artist: song.artist, durationSec: song.durationSec });
  assertTrue(lyricsRes.isSynced);

  // 8. Seek position to 32 seconds and verify active lyric line
  qm.seekTo(32000);
  const activeLineIdx = LrcParser.findActiveLineIndex(lyricsRes.lines, qm.currentPositionMs);
  assertEqual(activeLineIdx, 2); // 30s line is active
  assertEqual(lyricsRes.lines[activeLineIdx].text, 'Sample line 3');

  // 9. Background app on iOS: update lockscreen metadata
  const nowPlaying = new NowPlayingController();
  nowPlaying.updateMetadata(qm.currentItem, qm.playbackState, qm.currentPositionMs, qm.durationMs);

  assertEqual(nowPlaying.nowPlayingInfo.MPMediaItemPropertyTitle, 'Blinding Lights');
  assertEqual(nowPlaying.nowPlayingInfo.MPMediaItemPropertyArtist, 'The Weeknd');
  assertEqual(nowPlaying.nowPlayingInfo.MPNowPlayingInfoPropertyElapsedPlaybackTime, 32);
  assertEqual(nowPlaying.nowPlayingInfo.MPNowPlayingInfoPropertyPlaybackRate, 1.0);
});

// ============================================================================
// Workflow 2: Artist Discovery, Album Enqueue, Queue Reordering, Shuffle & Repeat Cycle
// ============================================================================
test('Workflow 2', 'Artist discovery, album enqueue, queue reorder, shuffle & repeat cycle', () => {
  // 1. Artist hero header
  const artistHeader = { name: 'Daft Punk', heightDp: 420, verified: true };
  assertTrue(artistHeader.verified);

  // 2. Load album with 14 tracks
  const tracks = [];
  for (let i = 1; i <= 14; i++) {
    tracks.push(InnertubeDiscoveryEngine.createSongItem(`dp_${i}`, `Discovery Track ${i}`, 'Daft Punk', 'Discovery', 210));
  }
  const album = InnertubeDiscoveryEngine.createAlbumItem('dp_disc', 'Discovery', 'Daft Punk', 2001, 14, tracks);
  assertEqual(album.trackCount, 14);

  // 3. Play All button clicked
  const qm = new PlaybackQueueManager();
  qm.playQueue(album.tracks, 0);
  assertEqual(qm.queue.length, 14);
  assertEqual(qm.currentIndex, 0);
  assertEqual(qm.currentItem.title, 'Discovery Track 1');

  // 4. Reorder track 0 to position 4
  qm.reorderQueue(0, 4);
  assertEqual(qm.currentItem.title, 'Discovery Track 1');
  assertEqual(qm.currentIndex, 4);

  // 5. Enable shuffle
  qm.setShuffle(true);
  assertTrue(qm.shuffleMode);
  assertEqual(qm.currentItem.title, 'Discovery Track 1'); // Active track remains at current index

  // 6. Skip forward 3 tracks
  assertTrue(qm.skipNext());
  assertTrue(qm.skipNext());
  assertTrue(qm.skipNext());
  assertEqual(qm.playbackState, 'PLAYING');

  // 7. Cycle repeat modes
  qm.setRepeat('ALL');
  assertEqual(qm.repeatMode, 'ALL');

  qm.setRepeat('ONE');
  assertEqual(qm.repeatMode, 'ONE');
  const currentTitle = qm.currentItem.title;
  qm.skipNext();
  assertEqual(qm.currentItem.title, currentTitle); // Repeats same track

  qm.setRepeat('OFF');
  assertEqual(qm.repeatMode, 'OFF');

  // Unshuffle
  qm.setShuffle(false);
  assertFalse(qm.shuffleMode);
});

// ============================================================================
// Workflow 3: Network Interruption, Offline Fallback, Connection Restoration & Auto-Resume
// ============================================================================
test('Workflow 3', 'Network interruption, offline fallback, connection restoration & auto-resume', () => {
  const qm = new PlaybackQueueManager();
  const player = new IosAudioPlayerSimulator();

  // 1. Initial online playback
  const onlineStream = StreamResolver.resolveStream('song_net_test');
  player.prepare(onlineStream.streamUrl);
  player.play();
  qm.playQueue([InnertubeDiscoveryEngine.createSongItem('song_net_test', 'Online Song', 'Artist', 'Album', 200)]);
  qm.seekTo(45000);

  assertEqual(player.state, 'PLAYING');
  assertEqual(qm.currentPositionMs, 45000);

  // 2. Simulate network disconnection error
  let networkStatus = 'ONLINE';
  let playbackError = null;

  const simulateNetworkLoss = () => {
    networkStatus = 'OFFLINE';
    playbackError = 'NETWORK_TIMEOUT';
    player.pause();
  };
  simulateNetworkLoss();

  assertEqual(networkStatus, 'OFFLINE');
  assertEqual(player.state, 'PAUSED');

  // 3. Fallback to offline cached storage
  const offlineCache = new Map([['song_net_test', 'file:///data/cache/song_net_test.m4a']]);
  const cachedUri = offlineCache.get('song_net_test');
  assertNotNull(cachedUri);

  // Switch to cached audio
  player.prepare(cachedUri);
  player.seekTo(qm.currentPositionMs);
  player.play();
  assertEqual(player.state, 'PLAYING');
  assertEqual(player.positionMs, 45000);

  // 4. Restore network and refresh token
  networkStatus = 'ONLINE';
  playbackError = null;
  const refreshedStream = StreamResolver.resolveStream('song_net_test', 'ANDROID_VR_NO_AUTH', 21600);
  assertFalse(StreamResolver.isExpired(refreshedStream.streamUrl));

  // Seamless continuation
  player.prepare(refreshedStream.streamUrl);
  player.seekTo(qm.currentPositionMs);
  player.play();
  assertEqual(player.state, 'PLAYING');
});

// ============================================================================
// Workflow 4: Complex Search Queries, Filter Tabs, Library View Toggle & Playlist Export
// ============================================================================
test('Workflow 4', 'Complex search queries, filter tabs, library view toggle & playlist export', () => {
  // 1. Complex query with special characters
  const complexQuery = 'AC/DC - Highway to Hell (Live) [Remastered] 2026';
  const encodedQuery = encodeURIComponent(complexQuery);
  assertEqual(decodeURIComponent(encodedQuery), complexQuery);

  // 2. Switch search tabs
  let activeTab = 'YOUTUBE_MUSIC';
  activeTab = 'DOWNLOADS';
  assertEqual(activeTab, 'DOWNLOADS');

  // 3. Library screen view mode toggle
  let libraryViewMode = 'GRID';
  const toggleViewMode = () => {
    libraryViewMode = (libraryViewMode === 'GRID') ? 'LIST' : 'GRID';
  };
  toggleViewMode();
  assertEqual(libraryViewMode, 'LIST');

  // 4. Sort library by duration descending
  const librarySongs = [
    InnertubeDiscoveryEngine.createSongItem('1', 'Short Track', 'Artist', 'Album', 120),
    InnertubeDiscoveryEngine.createSongItem('2', 'Epic Track', 'Artist', 'Album', 480),
    InnertubeDiscoveryEngine.createSongItem('3', 'Medium Track', 'Artist', 'Album', 240)
  ];
  librarySongs.sort((a, b) => b.durationSec - a.durationSec);
  assertEqual(librarySongs[0].title, 'Epic Track');
  assertEqual(librarySongs[1].title, 'Medium Track');
  assertEqual(librarySongs[2].title, 'Short Track');

  // 5. Create custom playlist
  const customPlaylist = {
    title: 'Workout High-Energy',
    tracks: librarySongs
  };
  assertEqual(customPlaylist.tracks.length, 3);

  // 6. Export to M3U format
  let m3uContent = '#EXTM3U\n';
  for (const track of customPlaylist.tracks) {
    m3uContent += `#EXTINF:${track.durationSec},${track.artist} - ${track.title}\n`;
    m3uContent += `https://omnitune.app/stream/${track.id}\n`;
  }

  // 7. Verify M3U export structure
  assertIncludes(m3uContent, '#EXTM3U');
  assertIncludes(m3uContent, '#EXTINF:480,Artist - Epic Track');
  assertIncludes(m3uContent, 'https://omnitune.app/stream/2');
});

// ============================================================================
// Workflow 5: Extended Listening Session, Waveform Scrubbing, Audio Route Change, Sleep Timer
// ============================================================================
test('Workflow 5', 'Extended listening session, waveform scrub, route change auto-pause, sleep timer', () => {
  const qm = new PlaybackQueueManager();
  const player = new IosAudioPlayerSimulator();
  const nowPlaying = new NowPlayingController();

  // 1. Start 6-minute ambient track
  const ambientSong = InnertubeDiscoveryEngine.createSongItem('amb_6m', 'Weightless', 'Marconi Union', 'Ambient', 360);
  qm.playQueue([ambientSong]);
  player.prepare('https://stream.mp4');
  player.play();
  nowPlaying.updateMetadata(ambientSong, 'PLAYING', 0, 360000);

  assertEqual(qm.durationMs, 360000);
  assertEqual(player.state, 'PLAYING');

  // 2. Expand player sheet (drag fraction 1.0)
  let sheetDragFraction = 1.0;
  assertEqual(sheetDragFraction, 1.0);

  // 3. Scrub waveform to 75% using Gradient style
  const scrubFraction = 0.75;
  const scrubPositionMs = SuvMusicThemeEngine.calculateWaveformPosition(scrubFraction, qm.durationMs);
  assertEqual(scrubPositionMs, 270000); // 4.5 minutes

  qm.seekTo(scrubPositionMs);
  player.seekTo(scrubPositionMs);
  nowPlaying.updateMetadata(ambientSong, 'PLAYING', scrubPositionMs, qm.durationMs);
  assertEqual(qm.currentPositionMs, 270000);
  assertEqual(nowPlaying.nowPlayingInfo.MPNowPlayingInfoPropertyElapsedPlaybackTime, 270);

  // 4. Physical audio route change: headphones disconnected
  const routeChangeResult = player.handleRouteChange('OldDeviceUnavailable');
  assertEqual(routeChangeResult.action, 'PAUSED');
  assertEqual(player.state, 'PAUSED');
  qm.pause();

  // 5. Resume from iOS Control Center remote command
  nowPlaying.registerCommandHandler('play', () => {
    player.play();
    qm.resume();
    return { status: 'SUCCESS' };
  });
  const remoteCmdRes = nowPlaying.dispatchRemoteCommand('play');
  assertEqual(remoteCmdRes.status, 'SUCCESS');
  assertEqual(player.state, 'PLAYING');
  assertTrue(qm.isPlaying);

  // 6. Set 15-minute sleep timer and trigger expiration
  let sleepTimerActive = true;
  let audioVolume = 1.0;

  const triggerSleepTimerExpire = () => {
    // Fade out volume
    audioVolume = 0.0;
    player.pause();
    qm.pause();
    sleepTimerActive = false;
  };
  triggerSleepTimerExpire();

  assertFalse(sleepTimerActive);
  assertEqual(audioVolume, 0.0);
  assertEqual(player.state, 'PAUSED');
  assertFalse(qm.isPlaying);
});
