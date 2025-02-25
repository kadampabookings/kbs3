package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.extras.panes.ScalePane;
import dev.webfx.kit.util.properties.ObservableLists;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityBase;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import one.modality.base.frontoffice.utility.page.FOPageUtil;
import org.kadampabookings.kbs.client.festivaltypes.FXFestivals;

final class FrontOfficeHomeActivity extends ViewDomainActivityBase implements OperationActionFactoryMixin {

    @Override
    protected void startLogic() {
        FXFestivals.init();
    }

    @Override
    public Node buildUi() {
        Text festivalsHeaderLabel = I18n.newText(FrontOfficeHomeI18nKeys.FestivalsHeader);
        festivalsHeaderLabel.getStyleClass().setAll("festivals-header");

        HBox festivalColumnsPane = new HBox(40);
        ObservableLists.bindConverted(festivalColumnsPane.getChildren(), FXFestivals.lastFestivals(), festival ->
            new FestivalThumbnail(festival).getView());

        /*Hyperlink moreEventsLabel = I18nControls.newHyperlink(FrontOfficeHomeI18nKeys.MoreEvents);
        moreEventsLabel.getStyleClass().setAll("more-events");*/

        VBox festivalsBox = FOPageUtil.restrictToMaxPageWidthAndApplyPageLeftRightPadding(new VBox(64,
            new ScalePane(festivalsHeaderLabel)
            , new ScalePane(festivalColumnsPane)
            //, moreEventsLabel
        ));
        festivalsBox.setAlignment(Pos.TOP_CENTER);

        //Node podcastsBox = HomePodcastsView.createView(getHistory());

        VBox container = new VBox(64,
            festivalsBox
            //, podcastsBox
        );
        container.setAlignment(Pos.TOP_CENTER);

        return FOPageUtil.applyTopBottomPagePadding(container);
    }
}
