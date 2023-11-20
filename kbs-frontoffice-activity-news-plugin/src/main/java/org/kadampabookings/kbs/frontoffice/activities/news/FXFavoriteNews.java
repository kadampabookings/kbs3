package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstArray;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.storage.LocalStorage;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import one.modality.base.shared.entities.News;

/**
 * @author Bruno Salmon
 */
public final class FXFavoriteNews {

    private static final String LOCAL_STORAGE_KEY = "favoriteNews";

    private static final ObservableList<Object> favoriteNewsIds = FXCollections.observableArrayList();

    static {
        ReadOnlyAstArray storedFavoriteNewsIds = Json.parseArraySilently(LocalStorage.getItem(LOCAL_STORAGE_KEY));
        if (storedFavoriteNewsIds != null) {
            storedFavoriteNewsIds.forEach(favoriteNewsIds::add);
        }
        favoriteNewsIds.addListener((InvalidationListener) observable -> {
            AstArray newIds = AST.createArray();
            favoriteNewsIds.forEach(newIds::push);
            LocalStorage.setItem(LOCAL_STORAGE_KEY, Json.formatArray(newIds));
        });
    }

    public static void markNewsAsFavorite(News news) {
        if (news != null)
            favoriteNewsIds.add(news.getPrimaryKey());
    }

    public static void unmarkNewsAsFavorites(News news) {
        if (news != null)
            favoriteNewsIds.remove(news.getPrimaryKey());
    }

    public static void toggleNewsAsFavorite(News news) {
        if (isNewsMarkedAsFavorite(news))
            unmarkNewsAsFavorites(news);
        else
            markNewsAsFavorite(news);
    }

    public static boolean isNewsMarkedAsFavorite(News news) {
        if (news == null)
            return false;
        Object primaryKey = news.getPrimaryKey();
        return favoriteNewsIds.contains(primaryKey);
    }


    public static ObservableList<Object> getFavoriteNewsIds() {
        return favoriteNewsIds;
    }

}
