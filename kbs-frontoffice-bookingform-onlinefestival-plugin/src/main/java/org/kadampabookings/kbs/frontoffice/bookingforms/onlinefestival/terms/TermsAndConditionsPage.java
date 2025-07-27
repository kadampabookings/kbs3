package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.terms;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.panes.CollapsePane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.extras.util.control.Controls;
import dev.webfx.extras.webtext.HtmlText;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.util.collection.Collections;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import one.modality.base.client.i18n.I18nEntities;
import one.modality.base.shared.entities.Event;
import one.modality.base.shared.entities.Letter;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.ecommerce.frontoffice.bookingelements.BookingElements;
import one.modality.ecommerce.frontoffice.bookingform.BookingForm;
import one.modality.ecommerce.frontoffice.bookingform.BookingFormI18nKeys;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;

/**
 * @author Bruno Salmon
 */
public final class TermsAndConditionsPage implements BookingFormPage {

    private final ObjectProperty<Letter> termsLetterProperty = new SimpleObjectProperty<>();
    private final HtmlText termsHtmlText = new HtmlText();
    private final CollapsePane termsCollapsePane = new CollapsePane(termsHtmlText);
    private final CheckBox agree = Bootstrap.strong(I18nControls.newCheckBox(BookingFormI18nKeys.AgreeTermsAndConditions));
    private final VBox container = BookingElements.createFormPageVBox(true,
        termsCollapsePane,
        agree
    );

    public TermsAndConditionsPage(BookingForm bookingForm) {
        Event event = ((EventBookingFormSettings) bookingForm.getSettings()).event();
        event.getStore().<Letter>executeQuery("select <frontend_loadEvent> from Letter where event=? and type.terms limit 1", event)
            .onSuccess(letters -> UiScheduler.runInUiThread(() -> {
                Letter termsLetter = Collections.first(letters);
                termsLetterProperty.set(termsLetter);
                if (termsLetter == null) {
                    Console.log("No terms and conditions found for event " + event.getId());
                } else {
                    I18nEntities.bindExpressionTextProperty(termsHtmlText.textProperty(), termsLetter, "i18n(this)");
                }
            }));
        container.setAlignment(Pos.TOP_LEFT);
        Controls.setupTextWrapping(agree, true, false);
        termsCollapsePane.collapsedProperty().bind(agree.selectedProperty());
        container.spacingProperty().bind(termsCollapsePane.collapsedProperty().map(selected -> selected ? 0 : 24));
        agree.setCursor(Cursor.HAND);
    }

    @Override
    public Object getTitleI18nKey() {
        return BookingFormI18nKeys.TermsAndConditions;
    }

    @Override
    public Node getView() {
        return container;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
    }

    @Override
    public ObservableBooleanValue validProperty() {
        return agree.selectedProperty();
    }

    @Override
    public boolean isApplicableToBooking(WorkingBooking workingBooking) {
        return termsLetterProperty.get() != null;
    }
}
