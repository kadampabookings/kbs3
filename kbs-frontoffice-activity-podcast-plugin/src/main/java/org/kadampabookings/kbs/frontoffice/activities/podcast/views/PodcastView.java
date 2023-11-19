package org.kadampabookings.kbs.frontoffice.activities.podcast.views;

import dev.webfx.extras.imagestore.ImageStore;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.kit.util.properties.Unregisterable;
import dev.webfx.platform.util.Objects;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import one.modality.base.frontoffice.utility.GeneralUtility;
import one.modality.base.frontoffice.utility.StyleUtility;
import one.modality.base.frontoffice.utility.TextUtility;
import one.modality.base.shared.entities.Podcast;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;

public final class PodcastView {
    private static MediaPlayer PLAYING_MEDIA_PLAYER; // will hold the player currently playing (only one player can be playing at one time)
    // Keeping all media players in memory (even if paused) to hold their states (ex: current time). There shouldn't be
    // that many because we create the media players only when the user actually presses the podcast play button.
    private static final Map<String /* audioUrl */, MediaPlayer> MEDIA_PLAYERS = new HashMap<>();
    // The media player associated with this particular podcast. Note that this podcast view can be recycled, which
    // means its associated podcast can change (through setPodcast()). When recycled, the media player can eventually
    // be retrieved from the already existing media players (if the user already played that podcast) so its visual
    // state can be re-established in that case. Otherwise - if the podcast hasn't been played so far in this session -
    // the media player will be null until the user presses the play button.
    private MediaPlayer mediaPlayer;
    private Unregisterable mediaPlayerBinding; // will allow to unbind a recycled view from its previous associated media player.
    private Podcast podcast;
    private Duration podcastDuration;
    private final ImageView authorImageView = new ImageView();
    private final Rectangle authorImageClip = new Rectangle();
    private final Text dateText = TextUtility.getSubText(null);
    private final Label titleLabel = GeneralUtility.getMainLabel(null, StyleUtility.MAIN_BLUE);
    private final Label excerptLabel = GeneralUtility.getMainLabel(null, StyleUtility.ELEMENT_GRAY);
    private final Pane playButton = PodcastButtons.createPlayButton();
    private final Pane pauseButton = PodcastButtons.createPauseButton();
    private final Pane forwardButton = PodcastButtons.createForwardButton();
    private final Pane backwardButton = PodcastButtons.createBackwardButton();
    private final ProgressBar progressBar = new ProgressBar();
    private final Text elapsedTimeText = TextUtility.getSubText(null, StyleUtility.ELEMENT_GRAY);

    {
        authorImageView.setPreserveRatio(true);
        authorImageView.setClip(authorImageClip);
        TextUtility.setFontFamily(elapsedTimeText, StyleUtility.CLOCK_FAMILY, 9);
        // Arming buttons
        playButton    .setOnMouseClicked(e -> play());
        pauseButton   .setOnMouseClicked(e -> pause());
        forwardButton .setOnMouseClicked(e -> seekRelative(30));
        backwardButton.setOnMouseClicked(e -> seekRelative(-10));
        progressBar   .setOnMouseClicked(e -> seekX(e.getX()));
        progressBar   .setOnMouseDragged(e -> seekX(e.getX()));
    }

    private final Pane podcastContainer = new Pane(authorImageView, dateText, titleLabel, excerptLabel, backwardButton, pauseButton, playButton, forwardButton, progressBar, elapsedTimeText) {
        private double imageY, imageSize, rightX, rightWidth, dateY, dateHeight, titleY, titleHeight, excerptY, excerptHeight, buttonY, buttonSize;

        @Override
        protected void layoutChildren() {
            computeLayout(getWidth());
            authorImageView.setFitWidth(imageSize);
            authorImageClip.setWidth(imageSize);
            authorImageClip.setHeight(imageSize);
            double arcWidthHeight = imageSize / 4;
            authorImageClip.setArcWidth(arcWidthHeight);
            authorImageClip.setArcHeight(arcWidthHeight);
            layoutInArea(authorImageView, 0, imageY, imageSize, imageSize, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(dateText, rightX, dateY, rightWidth, dateHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(titleLabel, rightX, titleY, rightWidth, titleHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(excerptLabel, rightX, excerptY, rightWidth, excerptHeight, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(backwardButton, rightX, buttonY, buttonSize, buttonSize, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(playButton, rightX + buttonSize + 5, buttonY, buttonSize, buttonSize, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(pauseButton, rightX + buttonSize + 5, buttonY, buttonSize, buttonSize, 0, HPos.LEFT, VPos.TOP);
            layoutInArea(forwardButton, rightX + 2 * (buttonSize + 5), buttonY, buttonSize, buttonSize, 0, HPos.LEFT, VPos.TOP);
            progressBar.setPrefWidth(rightWidth - 6 * (buttonSize + 5));
            layoutInArea(progressBar, rightX + 3 * (buttonSize + 5), buttonY, progressBar.getPrefWidth(), buttonSize, 0, HPos.LEFT, VPos.CENTER);
            layoutInArea(elapsedTimeText, rightX + 3 * (buttonSize + 5), buttonY + buttonSize, rightWidth, buttonSize, 0, HPos.LEFT, VPos.TOP);
        }

        @Override
        protected double computePrefHeight(double width) {
            computeLayout(width);
            return Math.max(imageY + imageSize, buttonY + buttonSize);
        }

        private void computeLayout(double width) {
            if (width == -1)
                width = getWidth();
            /* Image: */      imageY = 0;                               imageSize = width / 4;
            /* Right side: */ rightX = imageSize + 20;                  rightWidth = width - rightX;
            /* Date: */       dateY = 0;                                dateHeight = dateText.prefHeight(rightWidth);
            /* Title: */      titleY = dateY + dateHeight + 10;         titleHeight = titleLabel.prefHeight(rightWidth);
            /* Excerpt: */    excerptY = titleY + titleHeight + 10;     excerptHeight = excerptLabel.prefHeight(rightWidth);
            /* Buttons: */    buttonY = excerptY + excerptHeight + 10;  buttonSize = 32;
        }
    };

    public void setPodcast(Podcast podcast) {
        this.podcast = podcast;
        // Updating all fields and UI from the podcast
        podcastDuration = Duration.millis(podcast.getDurationMillis());
        authorImageView.setImage(ImageStore.getOrCreateImage(podcast.getImageUrl()));
        updateText(dateText, DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(podcast.getDate()));
        updateLabel(titleLabel, podcast.getTitle().toUpperCase());
        updateLabel(excerptLabel, podcast.getExcerpt());
        // We check if the podcast has already been played
        String audioUrl = podcast.getAudioUrl();
        MediaPlayer playedMediaPlayer = MEDIA_PLAYERS.get(audioUrl);
        // If yes, we associate this podcast view with that player
        if (playedMediaPlayer != null) {
            mediaPlayer = playedMediaPlayer;
            bindMediaPlayer(); // Will restore the visual state from the player (play/pause button & progress bar)
        } else { // If no, the player associated with this podcast should be null
            // If this podcast view was previously associated with a player, we unbind it.
            unbindMediaPlayer(); // will unregister the possible existing binding, and reset the visual state
            // This podcast hasn't been played so far, so its associated media player is now null
            mediaPlayer = null;
        }
    }

    public Node getView() {
        return podcastContainer;
    }

    private void play() {
        // Creating the media player if not already done
        if (mediaPlayer == null)
            createMediaPlayer();
        // If another player was playing, we pause it (keeping only one player playing at one time)
        if (PLAYING_MEDIA_PLAYER != null && PLAYING_MEDIA_PLAYER != mediaPlayer)
            PLAYING_MEDIA_PLAYER.pause();
        // Memorizing the new playing player
        PLAYING_MEDIA_PLAYER = mediaPlayer;
        // Ans
        mediaPlayer.play();
    }

    private void pause() {
        if (mediaPlayer != null)
            mediaPlayer.pause();
    }

    private void seekRelative(double relativeSeconds) {
        if (mediaPlayer != null)
            mediaPlayer.seek(mediaPlayer.getCurrentTime().add(Duration.seconds(relativeSeconds)));
    }

    private void seekX(double x) {
        if (mediaPlayer != null) {
            double percentage = x / progressBar.getWidth();
            Duration seekTime = podcastDuration.multiply(percentage);
            mediaPlayer.seek(seekTime);
        }
    }

    private void createMediaPlayer() {
        String audioUrl = podcast.getAudioUrl();
        mediaPlayer = new MediaPlayer(new Media(audioUrl));
        mediaPlayer.setOnEndOfMedia(mediaPlayer::stop); // Forcing stop status (sometimes this doesn't happen automatically for any reason)
        // Memorizing this new media player for possible further reuse on subsequent view recycling
        MEDIA_PLAYERS.put(audioUrl, mediaPlayer);
        // Binding this media player with this podcast view
        bindMediaPlayer();
    }

    private void bindMediaPlayer() {
        unbindMediaPlayer(); // in case this view was previously bound with another player
        mediaPlayerBinding = FXProperties.runNowAndOnPropertiesChange(() -> {
            MediaPlayer.Status status = mediaPlayer.getStatus();
            boolean isPlaying = status == MediaPlayer.Status.PLAYING;
            updatePlayPauseButtons(isPlaying);
            if (status == null || status == MediaPlayer.Status.UNKNOWN)
                progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            else
                updateElapsedTimeAndProgressBar(isPlaying || status == MediaPlayer.Status.PAUSED ? mediaPlayer.getCurrentTime() : podcastDuration);
        }, mediaPlayer.statusProperty(), mediaPlayer.currentTimeProperty());
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
    }

    private void updateElapsedTimeAndProgressBar(Duration elapsed) {
        updateText(elapsedTimeText, formatDuration(elapsed) + " / " + formatDuration(podcastDuration));
        progressBar.setProgress(elapsed.toMillis() / podcastDuration.toMillis());
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
        int minutes = (int) duration.toMinutes();
        int seconds = ((int) duration.toSeconds()) % 60;
        return (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }
}