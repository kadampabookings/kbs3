package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.summary;

import dev.webfx.extras.panes.MonoPane;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import one.modality.crm.frontoffice.order.OrderDetails;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.frontoffice.bookingelements.BookingElements;
import one.modality.ecommerce.frontoffice.bookingform.BookingFormI18nKeys;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;

/**
 * @author Bruno Salmon
 */
public class FestivalSummaryPage implements BookingFormPage {

    private final MonoPane embeddedLoginContainer = new MonoPane();
    private final TextArea requestTextArea = BookingElements.createTextArea();
    private final MonoPane summaryContainer = new MonoPane();
    private final VBox container = BookingElements.createPageVBox("summary", false,
        BookingElements.createWordingLabel(BookingFormI18nKeys.SummaryTopMessage),
        BookingElements.createSecondaryWordingLabel(BookingFormI18nKeys.SummarySubTopMessage),
        summaryContainer,
        BookingElements.twoLabels(
            BookingElements.createWordingLabel(BookingFormI18nKeys.AnyRequest),
            BookingElements.createSecondaryWordingLabel(BookingFormI18nKeys.writeRequest)),
        requestTextArea,
        embeddedLoginContainer
    );
    private final BooleanProperty validProperty = new SimpleBooleanProperty();

    @Override
    public Object getTitleI18nKey() {
        return BookingFormI18nKeys.Summary;
    }

    @Override
    public Node getView() {
        return container;
    }

    @Override
    public MonoPane getEmbeddedLoginContainer() {
        return embeddedLoginContainer;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        OrderDetails orderDetails = new OrderDetails(workingBooking);
        summaryContainer.setContent(orderDetails.getView());
        validProperty.bind(workingBookingProperties.hasChangesProperty().or(workingBookingProperties.balanceProperty().greaterThan(0)));
    }

    @Override
    public ObservableBooleanValue validProperty() {
        return validProperty;
    }
}
