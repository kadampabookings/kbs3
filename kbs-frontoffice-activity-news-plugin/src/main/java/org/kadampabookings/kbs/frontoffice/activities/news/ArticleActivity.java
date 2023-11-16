package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import one.modality.base.shared.entities.News;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public class ArticleActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin {

    private final WebView webView = new WebView();
    private String url;

    @Override
    public Node buildUi() {
        FXProperties.runNowAndOnPropertiesChange(() -> {
            News article = FXArticle.getArticle();
            String url = article == null ? null : article.getLinkUrl();
            if (!Objects.equals(url, this.url)) {
                dev.webfx.platform.console.Console.log("Changing url from '" + this.url + "' to '" + url + "'");
                webView.getEngine().load(this.url = url);
            }

        }, FXArticle.articleProperty());
        return webView;
    }

    @Override
    protected void updateModelFromContextParameters() {
        FXArticle.setArticleId(getParameter("articleId"));
    }
}
