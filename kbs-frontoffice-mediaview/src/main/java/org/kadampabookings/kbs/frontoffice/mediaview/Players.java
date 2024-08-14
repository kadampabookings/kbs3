package org.kadampabookings.kbs.frontoffice.mediaview;

import dev.webfx.extras.player.Player;
import dev.webfx.extras.player.video.VideoPlayer;
import dev.webfx.extras.util.animation.Animations;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.kit.util.properties.Unregisterable;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import one.modality.base.client.mainframe.dialogarea.fx.FXMainFrameOverlayArea;

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
    private static Timeline FULLSCREEN_BUTTON_TIMELINE;
    private static Unregisterable FULLSCREEN_LAYOUT;
    private static Unregisterable PLAYING_VIDEO_SCENE_CHECKER;

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
        PLAYING_PLAYER_PROPERTY.addListener((observable, oldPlayer, newPlayer) -> {
            // If another player was playing, we pause it (keeping only one player playing at one time)
            pausePlayer(oldPlayer);
            hideFullscreenButton();
            if (newPlayer instanceof VideoPlayer) {
                VideoPlayer videoPlayer = (VideoPlayer) newPlayer;
                PLAYING_VIDEO_SCENE_CHECKER = FXProperties.runOnPropertiesChange(() -> {
                    if (videoPlayer.getVideoView().getScene() == null)
                        hideFullscreenButton();
                }, videoPlayer.getVideoView().sceneProperty());
                if (videoPlayer.supportsFullscreen()) {
                    showFullscreenButton();
                }
            }
        });
    }

    private static void showFullscreenButton() {
        ObservableList<Node> overlayChildren = FXMainFrameOverlayArea.getOverlayChildren();
        if (!overlayChildren.contains(FULLSCREEN_BUTTON)) {
            overlayChildren.add(FULLSCREEN_BUTTON);
            Region overlayArea = FXMainFrameOverlayArea.getOverlayArea();
            FULLSCREEN_LAYOUT = FXProperties.runNowAndOnPropertiesChange(() -> {
                double width = overlayArea.getWidth();
                FULLSCREEN_BUTTON.resizeRelocate(width - 70, 10, 50, 50);
            }, overlayArea.widthProperty(), overlayArea.heightProperty());
            FULLSCREEN_BUTTON.setTranslateY(-100);
        }
        if (FULLSCREEN_BUTTON_TIMELINE != null)
            FULLSCREEN_BUTTON_TIMELINE.stop();
        FULLSCREEN_BUTTON_TIMELINE = Animations.animateProperty(FULLSCREEN_BUTTON.translateYProperty(), 0);
    }

    private static void hideFullscreenButton() {
        ObservableList<Node> overlayChildren = FXMainFrameOverlayArea.getOverlayChildren();
        if (overlayChildren.contains(FULLSCREEN_BUTTON)) {
            if (FULLSCREEN_BUTTON_TIMELINE != null)
                FULLSCREEN_BUTTON_TIMELINE.stop();
            FULLSCREEN_BUTTON_TIMELINE = Animations.animateProperty(FULLSCREEN_BUTTON.translateYProperty(), -100);
            FULLSCREEN_BUTTON_TIMELINE.setOnFinished(e -> overlayChildren.remove(FULLSCREEN_BUTTON));
        }
        if (FULLSCREEN_LAYOUT != null)
            FULLSCREEN_LAYOUT.unregister();
        if (PLAYING_VIDEO_SCENE_CHECKER != null)
            PLAYING_VIDEO_SCENE_CHECKER.unregister();
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
            if (player == getPlayingPlayer())
                playingPlayerProperty().set(null);
            if (player instanceof VideoPlayer) {
                Node videoView = ((VideoPlayer) player).getVideoView();
                MediaInfoView mediaInfoView = getAssociatedMediaInfoView(videoView);
                if (mediaInfoView != null)
                    mediaInfoView.onPause();
            }
        }
    }

    public static void associateVideoViewWithMediaInfoView(Node videoView, MediaInfoView mediaInfoView) {
        videoView.getProperties().put("kbs-mediaInfoView", mediaInfoView);
    }

    private static MediaInfoView getAssociatedMediaInfoView(Node videoView) {
        return (MediaInfoView) videoView.getProperties().get("kbs-mediaInfoView");
    }

}
