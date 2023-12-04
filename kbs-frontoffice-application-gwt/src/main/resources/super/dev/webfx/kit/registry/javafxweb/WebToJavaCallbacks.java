package dev.webfx.kit.registry.javafxweb;

/**
 * Temporarily hardcoded. TODO: implement callback feature in WebFX CLI from webfx.xml declaration.
 *
 * @author Bruno Salmon
 */
public class WebToJavaCallbacks {

    public native static void bindCallbackMethods(Object javaInstance, String javaClassName) /*-{
        switch (javaClassName) {
            case 'one.modality.event.frontoffice.activities.booking.views.DynamicMapView':
                javaInstance.onGoogleMapLoaded = javaInstance.@one.modality.event.frontoffice.activities.booking.views.DynamicMapView::onGoogleMapLoaded().bind(javaInstance);
                javaInstance.onMarkerClicked = javaInstance.@one.modality.event.frontoffice.activities.booking.views.DynamicMapView::onMarkerClicked(Lone/modality/event/frontoffice/activities/booking/views/MapMarker;).bind(javaInstance);
                break;
        }
    }-*/;


}
