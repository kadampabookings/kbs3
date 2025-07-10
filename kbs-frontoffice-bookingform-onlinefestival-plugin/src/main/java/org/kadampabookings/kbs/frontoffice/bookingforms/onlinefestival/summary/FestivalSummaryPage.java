package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.summary;

import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.text.Text;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.event.frontoffice.activities.booking.BookingI18nKeys;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.multipages.BookingFormPage;

/**
 * @author Bruno Salmon
 */
public class FestivalSummaryPage implements BookingFormPage {

    MonoPane monoPane = new MonoPane(Bootstrap.strong(new Text("TODO")));

    public FestivalSummaryPage() {
        monoPane.setPadding(new Insets(48, 0, 48, 0));
    }

    @Override
    public Object getTitleI18nKey() {
        return BookingI18nKeys.Summary;
    }

    @Override
    public Node getView() {
        return monoPane;
    }

    @Override
    public void setWorkingBooking(WorkingBooking workingBooking) {

    }
}
