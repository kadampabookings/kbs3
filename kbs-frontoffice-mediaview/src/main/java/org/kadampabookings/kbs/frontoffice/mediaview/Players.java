package org.kadampabookings.kbs.frontoffice.mediaview;

import dev.webfx.extras.player.Player;
import dev.webfx.extras.player.video.VideoPlayer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public class Players {

    private static final ObjectProperty<Player> PLAYING_PLAYER_PROPERTY = new SimpleObjectProperty<>();
    // will hold the player currently playing (only one player can be playing at one time)
    // Keeping all media players in memory (even if paused) to hold their states (ex: current time). There shouldn't be
    // that many because we create the media players only when the user actually presses the podcast play button.
    private static final Map<String /* track */, Player> PLAYED_PLAYERS = new HashMap<>();

    private static final Pane FULLSCREEN_BUTTON = MediaButtons.createFullscreenButton();

    static {
        FULLSCREEN_BUTTON.setOnMouseClicked(e -> {
            Player playingPlayer = getPlayingPlayer();
            if (playingPlayer instanceof VideoPlayer) {
                VideoPlayer videoPlayer = (VideoPlayer) playingPlayer;
                if (videoPlayer.supportsFullscreen() && videoPlayer.isPlaying()) {
                    videoPlayer.requestFullscreen();
                }
            }
        });
        PLAYING_PLAYER_PROPERTY.addListener((observable, oldPlayer, newValue) -> {
            // If another player was playing, we pause it (keeping only one player playing at one time)
            pausePlayer(oldPlayer);
        });
    }

    public static ObjectProperty<Player> playingPlayerProperty() {
        return PLAYING_PLAYER_PROPERTY;
    }

    public static Player getPlayingPlayer() {
        return PLAYING_PLAYER_PROPERTY.get();
    }

    public static void setPlayingPlayer(Player player, String track) {
        PLAYED_PLAYERS.put(track, player);
        PLAYING_PLAYER_PROPERTY.set(player);
    }

    public static Player getPlayedPlayerFromTrack(String track) {
        return PLAYED_PLAYERS.get(track);
    }

    public static void pausePlayingPlayer() {
        pausePlayer(getPlayingPlayer());
    }

    public static void pausePlayer(Player player) {
        if (player != null) {
            player.pause();
            if (player instanceof VideoPlayer) {
                Node videoView = ((VideoPlayer) player).getVideoView();
                MediaInfoView mediaInfoView = (MediaInfoView) videoView.getProperties().get("kbs-mediaInfoView");
                if (mediaInfoView != null)
                    mediaInfoView.pause();
            }
        }
    }

    public static void associateVideoViewWithMediaInfoView(Node videoView, MediaInfoView mediaInfoView) {
        videoView.getProperties().put("kbs-mediaInfoView", mediaInfoView);
    }

}
