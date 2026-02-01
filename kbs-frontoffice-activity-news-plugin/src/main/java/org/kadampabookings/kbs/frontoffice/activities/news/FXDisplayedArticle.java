package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.orm.entity.EntityStore;
import javafx.beans.property.ObjectProperty;
import one.modality.base.shared.entities.News;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class FXDisplayedArticle {

    private final static ObjectProperty<News> displayedArticleProperty = FXProperties.newObjectProperty(news ->
        setDisplayedArticleId(Entities.getPrimaryKey(news))
    );

    private final static ObjectProperty<Object> displayedArticleIdProperty = FXProperties.newObjectProperty(articleId -> {
        // Synchronizing article to match that new article id (articleId => Article)
        if (!Objects.equals(articleId, Entities.getPrimaryKey(getDisplayedArticle()))) { // Sync only if ids differ.
            // If the new article id is null, we set the article to null
            if (Entities.getPrimaryKey(articleId) == null)
                setDisplayedArticle(null);
            else {
                // Otherwise, we request the server to load that organization from that id
                EntityStore.create()
                    .<News>executeQuery("select linkUrl from News where id=$1", articleId)
                    .onFailure(Console::error)
                    .onSuccess(list -> // on successfully receiving the list (should be a singleton list)
                        setDisplayedArticle(list.isEmpty() ? null : list.get(0))); // we finally set FXOrganization
            }
        }
    });

    public static News getDisplayedArticle() {
        return displayedArticleProperty.get();
    }

    public static ObjectProperty<News> displayedArticleProperty() {
        return displayedArticleProperty;
    }

    public static void setDisplayedArticle(News article) {
        displayedArticleProperty.set(article);
    }

    public static Object getDisplayedArticleId() {
        return displayedArticleIdProperty.get();
    }

    public static ObjectProperty<Object> displayedArticleIdProperty() {
        return displayedArticleIdProperty;
    }

    public static void setDisplayedArticleId(Object articleId) {
        if (!Objects.equals(Numbers.toInteger(articleId), Numbers.toInteger(getDisplayedArticleId())))
            displayedArticleIdProperty.set(articleId);
    }
}
