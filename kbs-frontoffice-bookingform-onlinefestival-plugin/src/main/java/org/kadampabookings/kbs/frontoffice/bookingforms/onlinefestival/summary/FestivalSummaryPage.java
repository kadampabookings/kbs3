package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.summary;

import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.event.frontoffice.activities.booking.BookingI18nKeys;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.multipages.BookingFormPage;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.util.BookingFormUtil;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.OnlineFestivalI18nKeys;

/**
 * @author Bruno Salmon
 */
public class FestivalSummaryPage implements BookingFormPage {

    private final MonoPane embeddedLoginContainer = new MonoPane();
    private final TextArea requestTextArea = BookingFormUtil.createTextArea();
    private final VBox container = BookingFormUtil.createPageVBox("summary", false,
        BookingFormUtil.createStrongLabel(OnlineFestivalI18nKeys.SummaryTopMessage),
        BookingFormUtil.createSecondaryLabel(OnlineFestivalI18nKeys.SummarySubTopMessage),
        Bootstrap.strong(new Text("TODO")),
        BookingFormUtil.twoLabels(
            BookingFormUtil.createStrongLabel(OnlineFestivalI18nKeys.AnyRequest),
            BookingFormUtil.createSecondaryLabel(OnlineFestivalI18nKeys.writeRequest)),
        requestTextArea,
        embeddedLoginContainer
    );

    @Override
    public Object getTitleI18nKey() {
        return BookingI18nKeys.Summary;
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
    }
}
