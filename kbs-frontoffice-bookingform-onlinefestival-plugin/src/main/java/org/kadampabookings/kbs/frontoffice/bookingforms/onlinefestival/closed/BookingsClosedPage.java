package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.closed;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.extras.util.control.Controls;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import one.modality.booking.client.workingbooking.WorkingBooking;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingFormI18nKeys;
import one.modality.booking.frontoffice.bookingform.multipages.BookingFormPage;
import one.modality.event.client.lifecycle.EventLifeCycle;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.OnlineFestivalI18nKeys;

/**
 * @author Bruno Salmon
 */
public final class BookingsClosedPage implements BookingFormPage { // TODO: Move this page to Modality

    private final BooleanProperty endReachedProperty = new SimpleBooleanProperty(true);

    @Override
    public Object getTitleI18nKey() {
        return BookingFormI18nKeys.closed;
    }

    @Override
    public Node getView() {
        Label closedLabel = Bootstrap.strong(Bootstrap.textDanger(I18nControls.newLabel(OnlineFestivalI18nKeys.BookingsClosed)));
        Controls.setupTextWrapping(closedLabel, true, false);
        closedLabel.setPadding(new javafx.geometry.Insets(100, 0, 100, 0));
        return closedLabel;
    }

    @Override
    public boolean isApplicableToBooking(WorkingBooking workingBooking) {
        boolean considerAudioRecording = !workingBooking.isNewBooking();
        return EventLifeCycle.isClosed(workingBooking.getEvent(), considerAudioRecording)
               && !workingBooking.isPaymentRequestedByUser();
    }

    @Override
    public boolean isPriceBarRelevantToShow() {
        return false;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
    }

    @Override
    public ObservableBooleanValue endReachedProperty() {
        return endReachedProperty;
    }

}
