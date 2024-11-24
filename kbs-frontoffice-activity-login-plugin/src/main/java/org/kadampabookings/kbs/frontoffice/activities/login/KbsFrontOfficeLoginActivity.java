package org.kadampabookings.kbs.frontoffice.activities.login;

import dev.webfx.extras.panes.AspectRatioPane;
import dev.webfx.extras.panes.MonoClipPane;
import dev.webfx.extras.panes.ScaleMode;
import dev.webfx.extras.player.Media;
import dev.webfx.extras.player.Player;
import dev.webfx.extras.player.StartOptionsBuilder;
import dev.webfx.extras.player.Status;
import dev.webfx.extras.player.multi.MultiPlayer;
import dev.webfx.extras.player.video.web.wistia.WistiaVideoPlayer;
import dev.webfx.extras.player.video.web.youtube.YoutubeVideoPlayer;
import dev.webfx.extras.util.animation.Animations;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.login.ui.LoginUiService;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import one.modality.base.client.icons.SvgIcons;

/**
 * @author Bruno Salmon
 */
final class KbsFrontOfficeLoginActivity extends ViewDomainActivityBase {

    private static final String DRONE_SVG_PATH = "M 4.4 1.5 c 0 -0.1 -0 -0.2 -0.1 -0.3 l -3 -1.2 c -0 -0 -0.1 -0 -0.1 0 l -1 0.4 c -0.2 0.1 -0.2 0.4 0 0.5 l 4 0.8 c 0.1 0 0.2 -0.1 0.2 -0.2 Z M 4.8 1.7 l 4 -0.6 c 0.2 -0 0.2 -0.4 0 -0.5 l -1.1 -0.5 c -0 -0 -0.1 -0 -0.1 -0 l -2.9 1.1 c -0.1 0 -0.1 0.1 -0.1 0.2 c 0 0.1 0.1 0.2 0.2 0.2 Z M 20.4 2.5 c 0 0.6 0.4 1 0.9 1.2 v 0.3 c 0 0.2 -0.1 0.3 -0.3 0.3 h -4.5 l -0.5 -1.5 h -5.5 l -0.5 1.5 H 5.4 c -0.2 0 -0.3 -0.1 -0.3 -0.3 v -0.3 c 0.5 -0.2 0.9 -0.6 0.9 -1.2 v -0.7 h -3 v 0.7 c 0 0.5 0.4 1 0.8 1.2 v 0.1 c 0 1 0.8 1.7 1.7 1.8 l 0.4 5.3 c 0 0.3 0.4 0.3 0.5 0 l 0.4 -5.3 h 3.6 l 0.5 1.1 h 0 c -0.6 0.6 -1 1.4 -1 2.3 c 0 1.8 1.4 3.2 3.2 3.2 s 3.2 -1.4 3.2 -3.2 c 0 -0.9 -0.4 -1.7 -1 -2.3 h 0 l 0.5 -1.1 h 3.3 l 0.4 5.3 c 0 0.3 0.4 0.3 0.5 0 l 0.4 -5.3 h 0.4 c 1 0 1.8 -0.8 1.8 -1.8 v -0.1 c 0.4 -0.2 0.7 -0.6 0.7 -1.1 v -0.7 h -3 v 0.7 Z m -7.2 8.3 c -1 0 -1.8 -0.8 -1.8 -1.8 s 0.8 -1.8 1.8 -1.8 s 1.8 0.8 1.8 1.8 s -0.8 1.8 -1.8 1.8 Z M 17.6 1 l 4 0.8 c 0.1 0 0.2 -0.1 0.2 -0.2 c 0 -0.1 -0 -0.2 -0.1 -0.3 l -3 -1.2 c -0 -0 -0.1 -0 -0.1 0 l -1 0.4 c -0.2 0.1 -0.2 0.4 0 0.5 Z M 25.1 0.2 l -2.9 1.1 c -0.1 0 -0.1 0.1 -0.1 0.2 c 0 0.1 0.1 0.2 0.2 0.2 l 4 -0.6 c 0.2 -0 0.2 -0.4 0 -0.5 l -1.1 -0.5 c -0 -0 -0.1 -0 -0.1 -0 Z";
    //private static final String EXIT_SVG_PATH = "M 14.7 10.5 l 5.4 -5.4 c 1.2 -1.2 1.2 -3 0 -4.2 c -1.2 -1.2 -3 -1.2 -4.2 0 L 10.5 6.3 L 5.1 0.9 c -1.2 -1.2 -3 -1.2 -4.2 0 c -1.2 1.2 -1.2 3 0 4.2 L 6.3 10.5 l -5.4 5.4 c -1.2 1.2 -1.2 3 0 4.2 C 1.5 20.7 2.3 21 3 21 s 1.5 -0.3 2.1 -0.9 l 5.4 -5.4 l 5.4 5.4 C 16.5 20.7 17.3 21 18 21 s 1.5 -0.3 2.1 -0.9 c 1.2 -1.2 1.2 -3 0 -4.2 L 14.7 10.5 z";

    private final Node loginUI = LoginUiService.createLoginUI();
    private final MonoClipPane cornerButton = new MonoClipPane(true, SvgIcons.createSVGPath(DRONE_SVG_PATH));
    private final Player player = new MultiPlayer(new YoutubeVideoPlayer(), new WistiaVideoPlayer());
    private final Media droneMedia = player.acceptMedia("youtube:sYYuOC2Wxy8");
    private final AspectRatioPane aspectRatioPane = new AspectRatioPane(16d / 9);
    private Scheduled showingDroneScheduled;

    @Override
    public Node buildUi() {
        displayDroneVideo();
        player.setPlayerGroup(null);
        aspectRatioPane.setContent(player.getMediaView());
        aspectRatioPane.setMinHeight(Region.USE_PREF_SIZE);
        aspectRatioPane.setMaxHeight(Region.USE_PREF_SIZE);

        cornerButton.setBackground(Background.fill(Color.WHITE));
        cornerButton.setOpacity(0.75);
        SvgIcons.armButton(cornerButton, () -> showVideo(loginUI.getOpacity() == 1));
        cornerButton.setMinSize(50, 50);
        cornerButton.setPrefSize(50, 50);
        cornerButton.setMaxSize(50, 50);
        StackPane.setMargin(cornerButton, new Insets(10));
        StackPane.setAlignment(cornerButton, Pos.TOP_RIGHT);

        StackPane stackPane = new StackPane(aspectRatioPane, loginUI, cornerButton);

        aspectRatioPane.prefHeightProperty().bind(stackPane.minHeightProperty());

        stackPane.setOnMouseClicked(e -> {
            if (player.getMedia() == droneMedia)
                showVideo(false);
        });

        return stackPane;
    }

    private void displayDroneVideo() {
        player.stop();
        player.setMedia(droneMedia);
        player.setStartOptions(new StartOptionsBuilder().setMuted(true).setAutoplay(true).setLoop(true).build());
        player.displayVideo();
        FXProperties.runOrUnregisterOnPropertyChange((thisListener, o, status) -> {
            if (status == Status.PLAYING) {
                player.pause();
                thisListener.unregister();
            }
        }, player.statusProperty());
        aspectRatioPane.setMouseTransparent(true);
        aspectRatioPane.setFitMode(ScaleMode.BEST_ZOOM);
    }

    private void showVideo(boolean show) {
        if (show) {
            player.play();
            if (showingDroneScheduled == null || showingDroneScheduled.isFinished()) {
                showingDroneScheduled = UiScheduler.scheduleDelay(300, () -> {
                    //cornerButton.setContent(SvgIcons.createSVGPath(EXIT_SVG_PATH));
                    cornerButton.setVisible(false);
                    Animations.animateProperty(loginUI.opacityProperty(), 0);
                });
            }
        } else/* if (loginUI.getOpacity() == 0)*/ {
            cornerButton.setContent(SvgIcons.createSVGPath(DRONE_SVG_PATH));
            cornerButton.setVisible(true);
            Animations.setOrCallOnTimelineFinished(Animations.animateProperty(loginUI.opacityProperty(), 1), e2 ->
                player.pause());
        }
        loginUI.setMouseTransparent(show);
        loginUI.getParent().setCursor(show ? Cursor.HAND : null);
    }

}
