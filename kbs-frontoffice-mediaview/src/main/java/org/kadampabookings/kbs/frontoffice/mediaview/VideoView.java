package org.kadampabookings.kbs.frontoffice.mediaview;

import dev.webfx.extras.player.video.VideoPlayer;

public final class VideoView extends MediaInfoView {

    @Override
    protected boolean isMarkedAsFavorite() {
        return false;
    }

    @Override
    protected void toggleAsFavorite() {
        if (player instanceof VideoPlayer) {
            VideoPlayer videoPlayer = (VideoPlayer) player;
            if (videoPlayer.supportsFullscreen() && videoPlayer.isPlaying()) {
                videoPlayer.requestFullscreen();
            }
        }
    }

    /*{
        GeneralUtility.onNodeClickedWithoutScroll(e -> {
            if (e.isShiftDown()) {
                EntityPropertiesSheet.editEntity((Entity) mediaInfo,
                        "[" +
                        "'title'," +
                        "'excerpt'," +
                        "'teacher'," +
                        "'public'" +
                        "]",
                        mediaPane);
            }
        }, mediaPane);
    }*/

}