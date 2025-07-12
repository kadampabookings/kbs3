package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.audiorecording;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.GridPane;
import one.modality.base.shared.entities.Item;
import one.modality.base.shared.entities.ScheduledItem;
import one.modality.base.shared.entities.markers.EntityHasItem;
import one.modality.base.shared.knownitems.KnownItemFamily;
import one.modality.base.shared.knownitems.KnownItemI18nKeys;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.document.service.PolicyAggregate;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.multipages.BookingFormPage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Bruno Salmon
 */
public final class AudioRecordingPage implements BookingFormPage {

    private final GridPane gridPane = new GridPane(20, 20);

    public AudioRecordingPage() {
        gridPane.setAlignment(Pos.CENTER);
        gridPane.getStyleClass().addAll("booking-options", "recording-options");
        gridPane.setPadding(new Insets(48, 0, 48, 0));
        //gridPane.setMaxWidth(Region.USE_PREF_SIZE);
    }

    @Override
    public Object getTitleI18nKey() {
        return KnownItemI18nKeys.AudioRecordings;
    }

    @Override
    public Node getView() {
        return gridPane;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
        gridPane.getChildren().clear();
        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        Map<Item, List<ScheduledItem>> audioRecordingScheduledItemMap = policyAggregate.getFamilyScheduledItemsStream(KnownItemFamily.AUDIO_RECORDING)
            .collect(Collectors.groupingBy(EntityHasItem::getItem,
                () -> new TreeMap<>(Comparator.comparing(Item::getOrd)),
                Collectors.toList()));
        for (Map.Entry<Item, List<ScheduledItem>> entry : audioRecordingScheduledItemMap.entrySet()) {
            AudioRecordingLanguagePeriodOption teachingPeriodOptionView = new AudioRecordingLanguagePeriodOption(entry.getKey(), entry.getValue(), workingBookingProperties.getWorkingBooking());
            ButtonBase bookButton = teachingPeriodOptionView.getBookButton();
            int rowIndex = gridPane.getRowCount();
            gridPane.add(bookButton, 0, rowIndex);
            gridPane.add(teachingPeriodOptionView.getPeriodLabel(), 1, rowIndex);
            gridPane.add(teachingPeriodOptionView.getPriceLabel(), 2, rowIndex);
        }
    }

}
