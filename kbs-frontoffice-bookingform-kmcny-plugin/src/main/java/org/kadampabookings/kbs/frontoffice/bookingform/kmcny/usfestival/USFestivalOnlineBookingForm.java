package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import dev.webfx.extras.i18n.controls.I18nControls;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormActivityCallback;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingform.BookingFormSettings;
import one.modality.booking.frontoffice.bookingpage.BookingPageI18nKeys;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;

/**
 * US Festival Online Booking Form - Placeholder for online registration.
 *
 * <p>This form is currently a placeholder displaying "Coming Soon" message.
 * It will be expanded in the future to support online-only registration with
 * a simplified flow (no accommodation, different meal options, etc.).</p>
 *
 * @author Bruno Salmon
 * @see USFestivalEntryForm
 */
public final class USFestivalOnlineBookingForm implements BookingForm {

    private final HasWorkingBookingProperties activity;
    private final EventBookingFormSettings settings;
    private final BookingFormEntryPoint entryPoint;
    private BookingFormActivityCallback activityCallback;

    private final VBox container;

    public USFestivalOnlineBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings, BookingFormEntryPoint entryPoint) {
        this.activity = activity;
        this.settings = settings;
        this.entryPoint = entryPoint;

        // Create a simple "Coming Soon" placeholder UI
        container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(60, 40, 60, 40));

        Label comingSoonLabel = I18nControls.newLabel(BookingPageI18nKeys.ComingSoon);
        comingSoonLabel.getStyleClass().addAll("bookingpage-text-xl", "bookingpage-font-semibold", "bookingpage-text-dark");

        Label descriptionLabel = I18nControls.newLabel(BookingPageI18nKeys.OnlineDescription);
        descriptionLabel.getStyleClass().addAll("bookingpage-text-md", "bookingpage-text-muted");
        descriptionLabel.setWrapText(true);

        container.getChildren().addAll(comingSoonLabel, descriptionLabel);
    }

    // ========================================
    // BookingForm Interface Implementation
    // ========================================

    @Override
    public BookingFormSettings getSettings() {
        return settings;
    }

    @Override
    public Node buildUi() {
        return container;
    }

    @Override
    public void onWorkingBookingLoaded() {
        // No-op for placeholder
    }

    @Override
    public void setActivityCallback(BookingFormActivityCallback activityCallback) {
        this.activityCallback = activityCallback;
    }

    @Override
    public BookingFormActivityCallback getActivityCallback() {
        return activityCallback;
    }
}
