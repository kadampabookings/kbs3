package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.stack.i18n.controls.I18nControls;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import one.modality.base.client.i18n.BaseI18nKeys;

/**
 * @author Bruno Salmon
 */
final class NavigationBar {

    private final BorderPane container = new BorderPane();
    private final ObjectProperty<Object> titleI18nKeyProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            Label titleLabel = Bootstrap.strong(Bootstrap.textPrimary(I18nControls.newLabel(get())));
            titleLabel.setAlignment(Pos.CENTER);
            titleLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            titleLabel.getStyleClass().add("title");
            container.setCenter(titleLabel);
        }
    };

    public NavigationBar() {
        container.getStyleClass().add("top-bar");
        container.setLeft(createNavigationButton(BaseI18nKeys.Back));
        container.setRight(createNavigationButton(BaseI18nKeys.Next));
        container.setMinHeight(51);
    }

    public void setLabelI18nKey(Object labelI18nKey) {
        titleI18nKeyProperty.set(labelI18nKey);
    }

    public BorderPane getView() {
        return container;
    }

    private static Node createNavigationButton(Object i18nKey) {
        Label button = Bootstrap.textSecondary(I18nControls.newLabel(i18nKey));
        button.setAlignment(Pos.CENTER);
        button.setMinWidth(170);
        button.setMaxHeight(Double.MAX_VALUE);
        button.setCursor(Cursor.HAND);
        BorderPane.setAlignment(button, Pos.CENTER);
        return button;
    }
}
