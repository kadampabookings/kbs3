package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.orm.entity.EntityStore;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import one.modality.base.shared.entities.News;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class FXArticle {

    private final static ObjectProperty<News> articleProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            setArticleId(Entities.getPrimaryKey(get()));
        }
    };

    private final static ObjectProperty<Object> articleIdProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            // Getting the articleId that just changed
            Object articleId = getArticleId();
            // Synchronizing article to match that new article id (articleId => Article)
            if (!Objects.equals(articleId, Entities.getPrimaryKey(getArticle()))) { // Sync only if ids differ.
                // If the new article id is null, we set the article to null
                if (Entities.getPrimaryKey(articleId) == null)
                    setArticle(null);
                else {
                    // Otherwise, we request the server to load that organization from that id
                    EntityStore.create(dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService.getDefaultDataSourceModel())
                                .<News>executeQuery("select linkUrl from News where id=?", articleId)
                                .onFailure(System.out::println)
                                .onSuccess(list -> // on successfully receiving the list (should be a singleton list)
                                        setArticle(list.isEmpty() ? null : list.get(0))); // we finally set FXOrganization
                }
            }
        }
    };

    public static News getArticle() {
        return articleProperty.get();
    }

    public static ObjectProperty<News> articleProperty() {
        return articleProperty;
    }

    public static void setArticle(News article) {
        articleProperty.set(article);
    }

    public static Object getArticleId() {
        return articleIdProperty.get();
    }

    public static ObjectProperty<Object> articleIdProperty() {
        return articleIdProperty;
    }

    public static void setArticleId(Object articleId) {
        if (!Objects.equals(Numbers.toInteger(articleId), Numbers.toInteger(getArticleId())))
            articleIdProperty.set(articleId);
    }
}
