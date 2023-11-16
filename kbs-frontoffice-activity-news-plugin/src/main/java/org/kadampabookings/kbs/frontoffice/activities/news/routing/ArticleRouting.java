package org.kadampabookings.kbs.frontoffice.activities.news.routing;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.client.util.routing.ModalityRoutingUtil;

/**
 * @author Bruno Salmon
 */
public final class ArticleRouting {

    private final static String PATH = "/news/:articleId";

    public static String getPath() {
        return PATH;
    }

    public static String getArticlePath(Object articleId) {
        return ModalityRoutingUtil.interpolateParamInPath(":articleId", Entities.getPrimaryKey(articleId), PATH);
    }

}
