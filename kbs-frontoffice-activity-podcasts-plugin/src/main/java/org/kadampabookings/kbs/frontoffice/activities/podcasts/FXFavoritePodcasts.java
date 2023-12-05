package org.kadampabookings.kbs.frontoffice.activities.podcasts;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstArray;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.storage.LocalStorage;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import one.modality.base.shared.entities.Podcast;

/**
 * @author Bruno Salmon
 */
public final class FXFavoritePodcasts {

    private static final String LOCAL_STORAGE_KEY = "favoritePodcasts";

    private static final ObservableList<Object> favoritePodcastIds = FXCollections.observableArrayList();

    static {
        ReadOnlyAstArray storedFavoritePodcastIds = Json.parseArraySilently(LocalStorage.getItem(LOCAL_STORAGE_KEY));
        if (storedFavoritePodcastIds != null) {
            storedFavoritePodcastIds.forEach(favoritePodcastIds::add);
        }
        favoritePodcastIds.addListener((InvalidationListener) observable -> {
            AstArray newIds = AST.createArray();
            favoritePodcastIds.forEach(newIds::push);
            LocalStorage.setItem(LOCAL_STORAGE_KEY, Json.formatArray(newIds));
        });
    }

    public static void markPodcastAsFavorite(Podcast podcast) {
        if (podcast != null)
            favoritePodcastIds.add(podcast.getPrimaryKey());
    }

    public static void unmarkPodcastAsFavorite(Podcast podcast) {
        if (podcast != null)
            favoritePodcastIds.remove(podcast.getPrimaryKey());
    }

    public static void togglePodcastAsFavorite(Podcast Podcast) {
        if (isPodcastMarkedAsFavorite(Podcast))
            unmarkPodcastAsFavorite(Podcast);
        else
            markPodcastAsFavorite(Podcast);
    }

    public static boolean isPodcastMarkedAsFavorite(Podcast podcast) {
        if (podcast == null)
            return false;
        Object primaryKey = podcast.getPrimaryKey();
        return favoritePodcastIds.contains(primaryKey);
    }


    public static ObservableList<Object> getFavoritePodcastIds() {
        return favoritePodcastIds;
    }

}
