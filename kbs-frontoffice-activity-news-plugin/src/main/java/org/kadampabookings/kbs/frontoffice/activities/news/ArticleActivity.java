package org.kadampabookings.kbs.frontoffice.activities.news;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import one.modality.base.frontoffice.mainframe.fx.FXBackgroundNode;
import one.modality.base.shared.entities.News;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public class ArticleActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin {

    // The article will be displayed in a WebView that will be set as the front-office background node in order to
    // preserve its state in the web version (otherwise the iFrame will be reloaded each time the user navigates back).
    private final WebView webView = new WebView();
    private String url;

    public ArticleActivity() {
        // We use the "replace" webfx loading mode for the web version (iFrame) to not interfere with the main navigation history
        webView.getProperties().put("webfx-loadingMode", "replace");
        // We keep the WebView synchronized with the article to display (held by FXArticle)
        FXProperties.runNowAndOnPropertiesChange(() -> {
            News article = FXDisplayedArticle.getDisplayedArticle();
            String url = article == null ? null : article.getLinkUrl();
            if (!Objects.equals(url, this.url)) {
                webView.getEngine().load(this.url = url);
            }
        }, FXDisplayedArticle.displayedArticleProperty());
        // We set the front-office background node to that WebView.
        FXBackgroundNode.setBackgroundNode(webView);
    }

    @Override
    public Node buildUi() {
        // We return null as an indication that we want to display the background node (i.e. the WebView) instead.
        return null; // Checkout ModalityFrontOfficeMainFrameActivity to see how it treats this null value to display the background node.
    }

    @Override
    protected void updateModelFromContextParameters() {
        // Normally FXArticle is already set when the user clicks on the News page, but this is important to consider it
        // again here in case the user refreshes the browser. Then setting the articleId will cause FXArticle to load
        // the article url again from the database, which will then make the WebView react and display that url content.
        FXDisplayedArticle.setDisplayedArticleId(getParameter("articleId"));
    }
}
