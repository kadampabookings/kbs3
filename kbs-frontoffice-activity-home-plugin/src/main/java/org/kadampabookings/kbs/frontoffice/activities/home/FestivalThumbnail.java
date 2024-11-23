package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.i18n.spi.impl.I18nSubKey;
import dev.webfx.stack.orm.entity.Entities;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import one.modality.base.shared.entities.Event;

/**
 * @author Bruno Salmon
 */
final class FestivalThumbnail {

    private final Event festival;
    private final FestivalType festivalType;

    public FestivalThumbnail(Event festival) {
        this.festival = festival;
        int typeId = Numbers.toInteger(Entities.getPrimaryKey(festival.getType().getId()));
        festivalType = FestivalType.fromTypeId(typeId);
    }

    Node getView() {
        Label festivalName = I18nControls.newLabel("[" + festivalType.getI18nKey() + "] " + festival.getStartDate().getYear());
        festivalName.setWrapText(true);
        festivalName.setTextAlignment(TextAlignment.CENTER);
        festivalName.getStyleClass().setAll("festival-name");

        Button button = I18nControls.newButton("View");
        button.setPrefSize(240, 48);

        VBox container = new VBox(48,
            festivalName,
            I18n.newText(new I18nSubKey("expression: dateIntervalFormat(startDate, endDate)", festival)),
            button
        );
        container.setMaxWidth(340);
        container.setPadding(new Insets(48));
        container.setAlignment(Pos.TOP_CENTER);
        container.getStyleClass().setAll("festival-thumbnail", festivalType.getStyleClass());
        //container.setBackground(Background.fill(Color.GRAY));
        return container;
    }
}
