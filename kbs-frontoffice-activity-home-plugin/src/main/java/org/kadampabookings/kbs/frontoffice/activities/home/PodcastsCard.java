package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.stack.i18n.controls.I18nControls;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * @author Bruno Salmon
 */
final class PodcastsCard {

    public static Node createCard(String titleI18nKey, String contentI18nKey) {
        Label titleLabel = I18nControls.newLabel(titleI18nKey);
        titleLabel.setGraphicTextGap(15);
        titleLabel.getStyleClass().setAll("card-title");
        Label contentLabel = I18nControls.newLabel(contentI18nKey);
        contentLabel.getStyleClass().setAll("card-content");
        contentLabel.setWrapText(true);
        VBox vBox = new VBox(16,
            titleLabel,
            contentLabel
        );
        vBox.getStyleClass().setAll("podcasts-card");
        vBox.setMaxWidth(275);
        vBox.setPadding(new Insets(18));
        return vBox;
    }

}
