package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.audiorecording;

import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import one.modality.base.shared.entities.Item;
import one.modality.base.shared.entities.ScheduledItem;
import one.modality.base.shared.entities.markers.EntityHasItem;
import one.modality.base.shared.knownitems.KnownItemFamily;
import one.modality.base.shared.knownitems.KnownItemI18nKeys;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.document.service.PolicyAggregate;
import one.modality.ecommerce.frontoffice.bookingelements.BookingElements;
import one.modality.ecommerce.frontoffice.bookingform.BookingFormI18nKeys;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.OnlineFestivalI18nKeys;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Bruno Salmon
 */
public final class AudioRecordingPage implements BookingFormPage {

    private final GridPane gridPane = BookingElements.createOptionsGridPane(false);
    private final VBox container = BookingElements.createPageVBox("recording-options", true,
        BookingElements.createWordingLabel(OnlineFestivalI18nKeys.AudioRecordingTopMessage),
        BookingElements.createSecondaryWordingLabel(BookingFormI18nKeys.BookingOptions),
        gridPane
    );
    private WorkingBooking lastWorkingBooking;

    @Override
    public Object getTitleI18nKey() {
        return KnownItemI18nKeys.AudioRecordings;
    }

    @Override
    public Node getView() {
        return container;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        if (workingBooking == lastWorkingBooking)
            return;
        lastWorkingBooking = workingBooking;
        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        Map<Item, List<ScheduledItem>> audioRecordingScheduledItemMap = policyAggregate.getFamilyScheduledItemsStream(KnownItemFamily.AUDIO_RECORDING)
            .collect(Collectors.groupingBy(EntityHasItem::getItem,
                () -> new TreeMap<>(Comparator.comparing(Item::getOrd)),
                Collectors.toList()));
        gridPane.getChildren().clear();
        for (Map.Entry<Item, List<ScheduledItem>> entry : audioRecordingScheduledItemMap.entrySet()) {
            AudioRecordingLanguageAndPeriodOption teachingPeriodOptionView = new AudioRecordingLanguageAndPeriodOption(entry.getKey(), entry.getValue(), workingBooking);
            ButtonBase bookButton = teachingPeriodOptionView.getBookButton();
            int rowIndex = gridPane.getRowCount();
            gridPane.add(bookButton, 0, rowIndex);
            gridPane.add(teachingPeriodOptionView.getPeriodLabel(), 1, rowIndex);
            gridPane.add(teachingPeriodOptionView.getPriceLabel(), 2, rowIndex);
        }
    }

    @Override
    public boolean isApplicableToBooking(WorkingBooking workingBooking) {
        return !workingBooking.isPaymentRequestedByUser();
    }
}
