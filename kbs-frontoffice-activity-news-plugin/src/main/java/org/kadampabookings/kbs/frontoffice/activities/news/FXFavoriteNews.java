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
        ReadOnlyAstArray storedFavoriteNews = Json.parseArraySilently(LocalStorage.getItem(LOCAL_STORAGE_KEY));
        if (storedFavoriteNews != null) {
            storedFavoriteNews.forEach(favoriteNewsIds::add);
        }
        favoriteNewsIds.addListener((InvalidationListener) observable -> {
            AstArray storedFavoriteNews1 = AST.createArray();
            favoriteNewsIds.forEach(storedFavoriteNews1::push);
            LocalStorage.setItem(LOCAL_STORAGE_KEY, Json.formatArray(storedFavoriteNews1));
        });
    }

    public static void addFavoriteNews(News favoriteNews) {
        if (favoriteNews != null)
            favoriteNewsIds.add(favoriteNews.getPrimaryKey());
    }

    public static void removeFavoriteNews(News favoriteNews) {
        if (favoriteNews != null)
            favoriteNewsIds.remove(favoriteNews.getPrimaryKey());
    }

    public static void toggleFavoriteNews(News news) {
        if (isFavoriteNews(news))
            removeFavoriteNews(news);
        else
            addFavoriteNews(news);
    }

    public static boolean isFavoriteNews(News news) {
        if (news == null)
            return false;
        Object primaryKey = news.getPrimaryKey();
        return favoriteNewsIds.contains(primaryKey);
    }


    public static ObservableList<Object> getFavoriteNewsIds() {
        return favoriteNewsIds;
    }

}
