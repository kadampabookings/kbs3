package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import one.modality.event.frontoffice.activities.booking.process.event.AbstractBookingForm;
import one.modality.event.frontoffice.activities.booking.process.event.BookEventActivity;

/**
 * @author Bruno Salmon
 */
public class OnlineFestivalBookingForm extends AbstractBookingForm {

    private static final double MAX_WIDTH = 800;

    private final NavigationBar navigationBar = new NavigationBar();

    public OnlineFestivalBookingForm(BookEventActivity activity) {
        super(activity, false, false);
    }

    @Override
    public Node buildUi() {
        navigationBar.setLabelI18nKey(OnlineFestivalI18nKeys.TeachingsOnline);
        BorderPane container = new BorderPane();
        container.setTop(navigationBar.getView());
        container.setCenter(new Rectangle(300, 300, Color.TRANSPARENT));
        container.setBottom(new PriceBar(activity.getWorkingBookingProperties()).getView());
        container.getStyleClass().add("online-festival-booking-form");
        container.setMaxWidth(MAX_WIDTH);
        return container;
    }

    @Override
    public void onWorkingBookingLoaded() {
        bookWholeEvent();
    }

}
