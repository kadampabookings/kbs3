package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import one.modality.ecommerce.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.client.workingbooking.WorkingBookingProperties;
import one.modality.event.frontoffice.activities.booking.process.event.AbstractBookingForm;
import one.modality.event.frontoffice.activities.booking.process.event.BookEventActivity;

/**
 * @author Bruno Salmon
 */
final class OnlineFestivalBookingForm extends AbstractBookingForm {

    private static final double MAX_WIDTH = 800;

    private final NavigationBar navigationBar = new NavigationBar();

    public OnlineFestivalBookingForm(BookEventActivity activity) {
        super(activity, false, false);
    }

    @Override
    public Node buildUi() {
        WorkingBookingProperties workingBookingProperties = activity.getWorkingBookingProperties();
        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        navigationBar.setLabelI18nKey(OnlineFestivalI18nKeys.TeachingsOnline);
        BorderPane container = new BorderPane();
        container.setTop(navigationBar.getView());
        container.setCenter(new TeachingOptionsView(workingBooking).getView());
        container.setBottom(new PriceBar(workingBookingProperties).getView());
        container.getStyleClass().add("online-festival-booking-form");
        container.setMaxWidth(MAX_WIDTH);
        navigationBar.getBackButton().setDisable(true);
        navigationBar.getNextButton().setOnMouseClicked(e -> {
            activity.displayCheckoutSlide();
        });
        return container;
    }

    @Override
    public void onWorkingBookingLoaded() {
        //bookWholeEvent();
    }

}
