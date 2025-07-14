package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.summary;

import dev.webfx.extras.panes.MonoPane;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import one.modality.crm.frontoffice.activities.orders.BookingSummaryView;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.frontoffice.bookingform.BookingFormI18nKeys;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;
import one.modality.ecommerce.frontoffice.bookingform.util.BookingFormUtil;

/**
 * @author Bruno Salmon
 */
public class FestivalSummaryPage implements BookingFormPage {

    private final MonoPane embeddedLoginContainer = new MonoPane();
    private final TextArea requestTextArea = BookingFormUtil.createTextArea();
    private final MonoPane summaryContainer = new MonoPane();
    private final VBox container = BookingFormUtil.createPageVBox("summary", false,
        BookingFormUtil.createStrongLabel(BookingFormI18nKeys.SummaryTopMessage),
        BookingFormUtil.createSecondaryLabel(BookingFormI18nKeys.SummarySubTopMessage),
        summaryContainer,
        BookingFormUtil.twoLabels(
            BookingFormUtil.createStrongLabel(BookingFormI18nKeys.AnyRequest),
            BookingFormUtil.createSecondaryLabel(BookingFormI18nKeys.writeRequest)),
        requestTextArea,
        embeddedLoginContainer
    );

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
        BookingSummaryView bookingSummaryView = new BookingSummaryView(workingBookingProperties.getWorkingBooking());
        summaryContainer.setContent(bookingSummaryView.getView());
    }
}
