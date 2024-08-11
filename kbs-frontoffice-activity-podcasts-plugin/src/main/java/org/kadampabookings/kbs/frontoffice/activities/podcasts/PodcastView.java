package org.kadampabookings.kbs.frontoffice.activities.podcasts;

import one.modality.base.shared.entities.Podcast;
import org.kadampabookings.kbs.frontoffice.mediaview.MediaInfoView;

public final class PodcastView extends MediaInfoView {

    @Override
    protected boolean isMarkedAsFavorite() {
        return FXFavoritePodcasts.isPodcastMarkedAsFavorite((Podcast) mediaInfo);
    }

    @Override
    protected void toggleAsFavorite() {
        FXFavoritePodcasts.togglePodcastAsFavorite((Podcast) mediaInfo);
    }

}