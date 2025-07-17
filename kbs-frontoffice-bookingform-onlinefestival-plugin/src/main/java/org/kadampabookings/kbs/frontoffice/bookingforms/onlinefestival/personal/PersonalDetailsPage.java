package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.personal;

import dev.webfx.extras.i18n.I18n;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.extras.util.layout.Layouts;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import one.modality.crm.client.i18n.CrmI18nKeys;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.frontoffice.bookingelements.BookingElements;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;

/**
 * @author Bruno Salmon
 */
public final class PersonalDetailsPage implements BookingFormPage {

    private final MonoPane embeddedLoginContainer = new MonoPane();
    private final Button personToBookButton = BookingElements.createPersonToBookButton(false);
    private final VBox personalDetailsVBox = new VBox(10,
        Bootstrap.strong(I18n.newText(CrmI18nKeys.PersonToBook)),
        personToBookButton
    );
    private final VBox container = BookingElements.createPageVBox("personal-details", true,
        embeddedLoginContainer,
        personalDetailsVBox
    );

    public PersonalDetailsPage() {
        personalDetailsVBox.setMaxWidth(400);
        // personalDetailsVBox is not visible when login is showing, and vice versa
        Layouts.bindManagedAndVisiblePropertiesTo(embeddedLoginContainer.visibleProperty().not(), personalDetailsVBox);
    }

    @Override
    public Object getTitleI18nKey() {
        return CrmI18nKeys.PersonalDetails;
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
        personalDetailsVBox.setDisable(!workingBookingProperties.getWorkingBooking().isNewBooking());
    }

}
