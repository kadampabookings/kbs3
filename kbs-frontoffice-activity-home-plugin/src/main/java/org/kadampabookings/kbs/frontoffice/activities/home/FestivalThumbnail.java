package org.kadampabookings.kbs.frontoffice.activities.home;

import dev.webfx.extras.panes.MonoPane;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import one.modality.base.shared.entities.Event;
import one.modality.event.frontoffice.activities.booking.BookingI18nKeys;
import org.kadampabookings.kbs.client.festivaltypes.FestivalType;

/**
 * @author Bruno Salmon
 */
final class FestivalThumbnail {

    private final Event festival;
    private final FestivalType festivalType;

    public FestivalThumbnail(Event festival) {
        this.festival = festival;
        int typeId = Numbers.toInteger(Entities.getPrimaryKey(festival.getType()));
        festivalType = FestivalType.fromTypeId(typeId);
    }

    Node getView() {
        Label festivalName = I18nControls.newLabel("[" + festivalType.getLongI18nKey() + "] " + festival.getStartDate().getYear());
        festivalName.setWrapText(true);
        festivalName.setTextAlignment(TextAlignment.CENTER);
        festivalName.getStyleClass().setAll("festival-name");

        Button button = I18nControls.newButton(BookingI18nKeys.comingSoon);
        button.setPrefSize(240, 48);

        // Embedding the festival name into a growing pane so that dates and button all aligned the same at the bottom across the 3 thumbnails
        MonoPane festivalNamePane = new MonoPane(festivalName);
        festivalNamePane.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(festivalNamePane, Priority.SOMETIMES);
        festivalNamePane.setAlignment(Pos.TOP_CENTER);

        VBox container = new VBox(48,
            festivalNamePane,
            I18n.newText(new I18nSubKey("expression: dateIntervalFormat(startDate, endDate)", festival)),
            button
        );
        container.setPrefSize(360, 400);
        container.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        container.setPadding(new Insets(48, 8, 48, 8));
        container.setAlignment(Pos.TOP_CENTER);
        container.getStyleClass().setAll("festival-thumbnail", festivalType.getStyleClass());
        return container;
    }
}
