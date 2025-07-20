package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.summary;

import dev.webfx.extras.panes.MonoPane;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import one.modality.crm.frontoffice.order.OrderDetailsView;
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
        BookingElements.createStrongLabel(BookingFormI18nKeys.SummaryTopMessage),
        BookingElements.createSecondaryLabel(BookingFormI18nKeys.SummarySubTopMessage),
        summaryContainer,
        BookingElements.twoLabels(
            BookingElements.createStrongLabel(BookingFormI18nKeys.AnyRequest),
            BookingElements.createSecondaryLabel(BookingFormI18nKeys.writeRequest)),
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
        OrderDetailsView orderDetailsView = new OrderDetailsView(workingBookingProperties.getWorkingBooking());
        summaryContainer.setContent(orderDetailsView.getView());
        validProperty.bind(workingBookingProperties.hasChangesProperty());
    }

    @Override
    public ObservableBooleanValue validProperty() {
        return validProperty;
    }
}
