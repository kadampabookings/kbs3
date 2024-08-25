package org.kadampabookings.kbs.frontoffice.mediaview;

import dev.webfx.extras.imagestore.ImageStore;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.player.Player;
import dev.webfx.extras.player.Status;
import dev.webfx.extras.player.audio.media.AudioMediaPlayer;
import dev.webfx.extras.player.video.IntegrationMode;
import dev.webfx.extras.player.video.VideoPlayer;
import dev.webfx.extras.player.video.web.wistia.WistiaVideoPlayer;
import dev.webfx.extras.util.animation.Animations;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.kit.util.properties.Unregisterable;
import dev.webfx.platform.util.Objects;
import javafx.animation.Interpolator;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.frontoffice.utility.TextUtility;
import one.modality.base.shared.entities.Video;
import one.modality.base.shared.entities.markers.HasAudioUrl;
import one.modality.base.shared.entities.markers.HasMediaInfo;
import one.modality.base.shared.entities.markers.HasWistiaVideoId;

import java.time.format.DateTimeFormatter;

public abstract class MediaInfoView {

    private final static String FAVORITE_PATH = "M 24.066331,0 C 21.212473,0 18.540974,1.2921301 16.762259,3.4570778 14.983544,1.2920563 12.312119,0 9.4581876,0 4.2429514,0 0,4.2428782 0,9.4581873 0,13.54199 2.4351327,18.265558 7.237612,23.497667 c 3.695875,4.026405 7.716386,7.143963 8.860567,8.003592 L 16.762038,32 17.425897,31.501333 c 1.144181,-0.859629 5.164839,-3.977113 8.860788,-8.003518 4.802627,-5.23211 7.237834,-9.955751 7.237834,-14.0396277 C 33.524519,4.2428782 29.281567,0 24.066331,0 Z";

    // The media player associated with this particular podcast. Note that this podcast view can be recycled, which
    // means its associated podcast can change (through setPodcast()). When recycled, the media player can eventually
    // be retrieved from the already existing media players (if the user already played that podcast) so its visual
    // state can be re-established in that case. Otherwise - if the podcast hasn't been played so far in this session -
    // the media player will be null until the user presses the play button.
    protected Player player;
    protected boolean isAudio, isVideo;
    private Unregisterable mediaPlayerBinding; // will allow to unbind a recycled view from its previous associated media player.
    protected HasMediaInfo mediaInfo;
    private Duration mediaDuration;
    private final MonoPane videoContainer = new MonoPane();
    private final ImageView imageView = new ImageView();
    private Image image;
    private final Rectangle imageClip = new Rectangle();
    private final Text dateText = TextUtility.createText(StyleUtility.ELEMENT_GRAY_COLOR);
    private final Label titleLabel = GeneralUtility.createLabel(StyleUtility.MAIN_ORANGE_COLOR);
    private final Label excerptLabel = GeneralUtility.createLabel(Color.BLACK);
    private final Pane playButton = MediaButtons.createPlayButton();
    private final Pane pauseButton = MediaButtons.createPauseButton();
    private final Pane forwardButton = MediaButtons.createForwardButton();
    private final Pane backwardButton = MediaButtons.createBackwardButton();
    private final ProgressBar progressBar = new ProgressBar();
    private final Text elapsedTimeText = TextUtility.createText(StyleUtility.ELEMENT_GRAY_COLOR);
    private final SVGPath favoriteSvgPath = new SVGPath();
    private final Pane favoritePane = new MonoPane(favoriteSvgPath);
    private Timeline imageFadeTimeline;
    private boolean timelineShowImage;
    protected final Pane mediaPane = new Pane(videoContainer, imageView, dateText, titleLabel, excerptLabel, backwardButton, pauseButton, playButton, forwardButton, progressBar, elapsedTimeText, favoritePane) {
        private double fontFactor;
        private double imageY, imageWidth, imageHeight, rightX, rightWidth, dateY, dateHeight, titleY, titleHeight, excerptY, excerptHeight, buttonY, buttonSize, favoriteY, favoriteHeight;

        @Override
        public Orientation getContentBias() {
            return Orientation.HORIZONTAL; // To indicate that the height of this pane depends on its width
        }

        @Override
        protected void layoutChildren() {
            computeLayout(getWidth());
            imageView.setFitWidth(isAudio ? imageWidth : 0);
            imageView.setFitHeight(imageHeight);
            layoutInArea(imageView, 0, imageY, imageWidth, imageHeight, 0, HPos.CENTER, VPos.CENTER);
            layoutInArea(dateText, rightX, dateY, rightWidth, dateHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(titleLabel, rightX, titleY, rightWidth, titleHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(excerptLabel, rightX, excerptY, rightWidth, excerptHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(favoritePane, rightX, favoriteY, rightWidth, favoriteHeight, 0, HPos.LEFT, VPos.TOP);
            if (isVideo) {
                layoutInArea(videoContainer, 0, imageY, imageWidth, imageHeight, 0, HPos.CENTER, VPos.CENTER);
                layoutInArea(playButton, rightX, buttonY, buttonSize, buttonSize, 0, HPos.LEFT, VPos.TOP);
                layoutInArea(pauseButton, rightX, buttonY, buttonSize, buttonSize, 0, HPos.LEFT, VPos.TOP);
                layoutInArea(elapsedTimeText, rightX + buttonSize + 20, buttonY, rightWidth, buttonSize, 0, HPos.LEFT, VPos.CENTER);
            } else {
                imageClip.setWidth(imageWidth);
                imageClip.setHeight(imageHeight);
                double arcWidthHeight = imageWidth / 4;
                imageClip.setArcWidth(arcWidthHeight);
                imageClip.setArcHeight(arcWidthHeight);
                layoutInArea(backwardButton, rightX, buttonY, buttonSize, buttonSize, 0, HPos.LEFT, VPos.TOP);
                layoutInArea(playButton, rightX + buttonSize + 5, buttonY, buttonSize, buttonSize, 0, HPos.LEFT, VPos.TOP);
                layoutInArea(pauseButton, rightX + buttonSize + 5, buttonY, buttonSize, buttonSize, 0, HPos.LEFT, VPos.TOP);
                layoutInArea(forwardButton, rightX + 2 * (buttonSize + 5), buttonY, buttonSize, buttonSize, 0, HPos.LEFT, VPos.TOP);
                double progressBarX = rightX + 3 * (buttonSize + 5), progressBarHeight = 10, progressBarY = buttonY + buttonSize / 2 - progressBarHeight / 2;
                progressBar.setPrefWidth(getWidth() - progressBarX);
                layoutInArea(progressBar, progressBarX, progressBarY, progressBar.getPrefWidth(), progressBarHeight, 0, HPos.LEFT, VPos.CENTER);
                layoutInArea(elapsedTimeText, progressBarX, buttonY + buttonSize, rightWidth, buttonSize, 0, HPos.LEFT, VPos.TOP);
            }
        }

        @Override
        protected double computePrefHeight(double width) {
            computeLayout(width);
            return Math.max(imageY + imageHeight, favoriteY + favoriteHeight);
        }

        private void computeLayout(double width) {
            if (width == -1)
                width = getWidth();
            /* Updating fonts if necessary (required before layout) */
            double fontFactor = GeneralUtility.computeFontFactor(width);
            if (fontFactor != this.fontFactor) {
                this.fontFactor = fontFactor;
                GeneralUtility.setLabeledFont(  titleLabel, StyleUtility.TEXT_FAMILY,  FontWeight.SEMI_BOLD, fontFactor * StyleUtility.MAIN_TEXT_SIZE);
                TextUtility.setTextFont(          dateText, StyleUtility.TEXT_FAMILY,  FontWeight.NORMAL,    fontFactor * StyleUtility.SUB_TEXT_SIZE);
                GeneralUtility.setLabeledFont(excerptLabel, StyleUtility.TEXT_FAMILY,  FontWeight.NORMAL,    fontFactor * StyleUtility.MEDIUM_TEXT_SIZE);
                TextUtility.setTextFont(   elapsedTimeText, StyleUtility.CLOCK_FAMILY, FontWeight.NORMAL,    fontFactor * StyleUtility.SUB_TEXT_SIZE);
            }
            double imageRatio;
            if (isAudio) {
                imageRatio = image == null || image.getWidth() == 0 ? 1 : image.getWidth() / image.getHeight();
            } else {
                imageRatio = 16d / 9;
                if (mediaInfo instanceof Video && (imageFadeTimeline != null || imageView.getOpacity() < 1)) {
                    Video video = (Video) mediaInfo;
                    Integer videoWidth = video.getWidth();
                    Integer videoHeight = video.getHeight();
                    if (videoWidth != null && videoHeight != null)
                        imageRatio = videoWidth * 1d / videoHeight;
                }
            }
            if (width <= 400) { // Small screen => vertical alignment: image above title, date, excerpt, buttons & favorite
            /*Image:*/       imageY = 0;                                                 imageWidth = width; imageHeight = imageWidth / imageRatio;
            /*Right side:*/  rightX = 0; /* Actually no right side */                    rightWidth = width - rightX;
            /*Tile:*/        titleY = imageY + imageHeight + 10;                        titleHeight = titleLabel.prefHeight(rightWidth);
            } else { // Normal or large screen => image on left, title, date, excerpt, buttons & favorite on right
            /*Image:*/       imageY = 0;                                                 imageWidth = isVideo ? width / 2 : width / 4; imageHeight = imageWidth / imageRatio;
            /*Right side:*/  rightX = imageWidth + 20;                                   rightWidth = width - rightX;
            /*Tile:*/        titleY = 0;       titleHeight = titleLabel.prefHeight(rightWidth);
            }
            /*Date:*/         dateY = titleY + titleHeight + 10;                         dateHeight = dateText.prefHeight(rightWidth);
            /*Excerpt:*/   excerptY = dateY + dateHeight + 10;                        excerptHeight = excerptLabel.prefHeight(rightWidth);
            /*Buttons:*/    buttonY = excerptY + excerptHeight + (isAudio ? 30 : 20);    buttonSize = 32;
            /*Favorite:*/ favoriteY = buttonY + buttonSize + (isAudio ? 30 : 20);    favoriteHeight = 32;
        }
    };

    {
        // Arming buttons
        GeneralUtility.onNodeClickedWithoutScroll(e -> play(), playButton, imageView);
        GeneralUtility.onNodeClickedWithoutScroll(e -> pause(), pauseButton);
        GeneralUtility.onNodeClickedWithoutScroll(e -> seekRelative(30), forwardButton);
        GeneralUtility.onNodeClickedWithoutScroll(e -> seekRelative(-10), backwardButton);
        GeneralUtility.onNodeClickedWithoutScroll(e -> seekX(e.getX()), progressBar);
        progressBar   .setOnMouseDragged(         e -> seekX(e.getX()));
        GeneralUtility.onNodeClickedWithoutScroll(e -> {
            toggleAsFavorite();
            updateFavorite();
            e.consume();
        }, favoritePane);
        favoriteSvgPath.setContent(FAVORITE_PATH);
        favoriteSvgPath.setStrokeWidth(2);
        updateFavorite();
    }

    public void setMediaInfo(HasMediaInfo mediaInfo) {
        if (mediaInfo == this.mediaInfo)
            return;
        this.mediaInfo = mediaInfo;
        isAudio = (mediaInfo instanceof HasAudioUrl) && ((HasAudioUrl) mediaInfo).getAudioUrl() != null;
        isVideo = !isAudio && (mediaInfo instanceof HasWistiaVideoId && ((HasWistiaVideoId) mediaInfo).getWistiaVideoId() != null);
        // Updating all fields and UI from the podcast
        imageView.setPreserveRatio(true);
        imageView.setClip(isAudio ? imageClip : null);
        updateText(dateText, DateTimeFormatter.ofPattern("d MMMM yyyy").format(mediaInfo.getDate()));
        updateLabel(titleLabel, mediaInfo.getTitle());
        updateLabel(excerptLabel, mediaInfo.getExcerpt());
        image = ImageStore.getOrCreateImage(mediaInfo.getImageUrl());
        imageView.setImage(image);
        mediaDuration = Duration.millis(mediaInfo.getDurationMillis());
        backwardButton.setVisible(isAudio);
        forwardButton.setVisible(isAudio);
        progressBar.setVisible(isAudio);
        updateFavorite();
        // If no, the player associated with this podcast should be null
        // If this podcast view was previously associated with a player, we unbind it.
        unbindMediaPlayer(); // will unregister the possible existing binding, and reset the visual state
        // We check if this track has already been played
        player = Players.getPlayerAssociatedWithTrack(getTrack());
        if (player != null) {  // If yes, we reuse the same player straightaway
            bindMediaPlayer(); // => will restore the visual state from the player (play/pause button & progress bar)
        } /* Commented, as not sure why video players creation couldn't be postponed like audio players TODO: completely remove if no side effects
        else if (isVideo) { // otherwise we create a new one for video only
            createPlayer();
        }*/
    }

    private String getTrack() {
        return isAudio ? ((HasAudioUrl) mediaInfo).getAudioUrl() : ((HasWistiaVideoId) mediaInfo).getWistiaVideoId();
    }

    public Node getView() {
        return mediaPane;
    }

    private Node getVideoView() {
        return getVideoView(player);
    }

    private static Node getVideoView(Player player) {
        if (player instanceof VideoPlayer)
            return ((VideoPlayer) player).getVideoView();
        return null;
    }

    private void updateFavorite() {
        boolean isFavorite = isMarkedAsFavorite();
        favoriteSvgPath.setStroke(isFavorite ? Color.RED : Color.GRAY);
        favoriteSvgPath.setFill(isFavorite ? Color.RED : null);
    }

    protected abstract boolean isMarkedAsFavorite();

    protected abstract void toggleAsFavorite();

    private void play() {
        // Creating the media player if not already done
        if (player == null)
            createPlayer();
        // Starting playing
        player.play();
        updatePlayPauseButtons(true);
    }

    private void pause() {
        Players.pausePlayer(player); // will callback onPause()
    }

    void onPause() {
        // Normally the previous call should update the player status and the listener set in bindMediaPlayer()
        // should detect it and update the play/pause button, but just in case this doesn't happen for some reason,
        // we ensure the button is displayed as paused.
        updatePlayPauseButtons(false);
    }

    private void seekRelative(double relativeSeconds) {
        if (player != null)
            player.seek(player.getCurrentTime().add(Duration.seconds(relativeSeconds)));
    }

    private void seekX(double x) {
        if (player != null) {
            double percentage = x / progressBar.getWidth();
            Duration seekTime = mediaDuration.multiply(percentage);
            player.seek(seekTime);
        }
    }

    private void createPlayer() {
        player = isAudio ? new AudioMediaPlayer() : new WistiaVideoPlayer();
        String track = getTrack();
        player.getPlaylist().setAll(track);
        player.setOnEndOfPlaying(player::stop); // Forcing stop status (sometimes this doesn't happen automatically for any reason)
        // Registering the playing track
        Players.associatePlayerWithTrack(player, getTrack());
        // Binding this media player with this podcast view
        bindMediaPlayer();
    }

    private void bindMediaPlayer() {
        unbindMediaPlayer(); // in case this view was previously bound with another player
        videoContainer.setContent(getVideoView());
        if (isVideo) {
            Node videoView = getVideoView();
            if (videoView != null) {
                Players.associateVideoViewWithMediaInfoView(videoView, this);
                /*if (((VideoPlayer) player).getIntegrationMode() == IntegrationMode.SEAMLESS) {
                    Player p = player;
                    FXProperties.runOnPropertiesChange(() -> {
                        if (videoView.getScene() == null) {
                            p.pause();
                        } else
                            videoContainer.requestLayout();
                    }, videoView.sceneProperty());
                }*/
            }
            mediaPlayerBinding = FXProperties.runNowAndOnPropertiesChange(() -> {
                boolean playing = player != null && player.isPlaying();
                updatePlayPauseButtons(playing);
            }, player.statusProperty());
        } else { // audio
            mediaPlayerBinding = FXProperties.runNowAndOnPropertiesChange(() -> {
                if (player == null)
                    return;
                boolean isPlaying = player.isPlaying();
                updatePlayPauseButtons(isPlaying);
                Status status = player.getStatus();
                if (status == null || status == Status.UNKNOWN)
                    progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                else
                    updateElapsedTimeAndProgressBar(isPlaying || status == Status.PAUSED ? player.getCurrentTime() : mediaDuration);
            }, player.statusProperty(), player.currentTimeProperty());
        }
        //mediaPlayer.setOnError(() -> System.out.println("An error occurred: " + mediaPlayer.getError()));
    }

    private void unbindMediaPlayer() {
        if (mediaPlayerBinding != null) {
            mediaPlayerBinding.unregister();
            mediaPlayerBinding = null;
        }
        updatePlayPauseButtons(false);
        updateElapsedTimeAndProgressBar(Duration.ZERO);
    }

    private void updatePlayPauseButtons(boolean isPlaying) {
        pauseButton.setVisible(isPlaying);
        playButton.setVisible(!isPlaying);
        Status status = player == null ? null : player.getStatus();
        // Sometimes this method is called with an anticipated value for isPlaying (ex: play() method). We check if
        // the player is really playing
        boolean reallyPlaying = status == Status.PLAYING;
        // Note: when paused, only the seamless player is able to resume, others are just stopped
        boolean showVideo = isVideo && isPlaying && (reallyPlaying || status == Status.PAUSED && player instanceof VideoPlayer && ((VideoPlayer) player).getIntegrationMode() == IntegrationMode.SEAMLESS);
        // Note: using setVisible(false) doesn't prevent wistia player to appear sometimes, while setOpacity(0) does
        videoContainer.setOpacity(showVideo ? 1 : 0);
        imageView.setMouseTransparent(showVideo);
        showImage(!showVideo);
        // Updating Players when relevant:
        if (isPlaying) { // if this player is playing, then we report this to Players
            if (reallyPlaying) // we double-check the status because the play() method actually anticipates
                Players.setPlayingPlayer(player); // the playing status while Players concept is to for actual playing status
        } else if (Players.getPlayingPlayer() == player) { // if it's not playing while it was declared as the playing player
            Players.setNoPlayingPlayer(); // we report Players that it's not anymore
        }
    }

    private void showImage(boolean showImage) {
        double requestedImageOpacity = showImage ? 1 : 0;
        if (imageFadeTimeline != null) { // animation already running
            if (timelineShowImage == showImage) // same as new request => we just keep going
                return;
            imageFadeTimeline.stop(); // different as new request => we stop the previous one
        } else { // no animation running => we need an animation only if the opacity is not the requested one
            if (imageView.getOpacity() == requestedImageOpacity)
                return;
        }
        timelineShowImage = showImage;
        imageFadeTimeline = Animations.animateProperty(imageView.opacityProperty(), requestedImageOpacity, Duration.seconds(1), Interpolator.EASE_BOTH, true);
        imageFadeTimeline.setOnFinished(e -> {
            imageFadeTimeline = null;
            mediaPane.requestLayout();
        });
        mediaPane.requestLayout();
    }

    private void updateElapsedTimeAndProgressBar(Duration elapsed) {
        if (isAudio) {
            updateText(elapsedTimeText, formatDuration(elapsed) + " / " + formatDuration(mediaDuration));
            progressBar.setProgress(elapsed.toMillis() / mediaDuration.toMillis());
        } else
            updateText(elapsedTimeText, formatDuration(mediaDuration));
    }

    private static void updateText(Text text, String newContent) {
        if (!Objects.areEquals(newContent, text.getText()))
            text.setText(newContent);
    }

    private static void updateLabel(Label label, String newContent) {
        if (!Objects.areEquals(newContent, label.getText()))
            label.setText(newContent);
    }

    private static String formatDuration(Duration duration) {
        if (duration.isIndefinite() || duration.isUnknown())
            return "xx:xx";
        int minutes = (int) duration.toMinutes();
        int seconds = ((int) duration.toSeconds()) % 60;
        return (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }
}