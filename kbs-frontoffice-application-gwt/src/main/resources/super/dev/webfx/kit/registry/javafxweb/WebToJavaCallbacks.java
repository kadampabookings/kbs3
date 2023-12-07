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
                javaInstance.consoleLog = javaInstance.@one.modality.event.frontoffice.activities.booking.views.DynamicMapView::consoleLog(Ljava/lang/String;).bind(javaInstance);
                javaInstance.consoleWarn = javaInstance.@one.modality.event.frontoffice.activities.booking.views.DynamicMapView::consoleWarn(Ljava/lang/String;).bind(javaInstance);
                javaInstance.consoleError = javaInstance.@one.modality.event.frontoffice.activities.booking.views.DynamicMapView::consoleError(Ljava/lang/String;).bind(javaInstance);
                javaInstance.onGoogleMapLoaded = javaInstance.@one.modality.event.frontoffice.activities.booking.views.DynamicMapView::onGoogleMapLoaded().bind(javaInstance);
                javaInstance.onMarkerClicked = javaInstance.@one.modality.event.frontoffice.activities.booking.views.DynamicMapView::onMarkerClicked(Lone/modality/event/frontoffice/activities/booking/views/MapMarker;).bind(javaInstance);
                break;
        }
    }-*/;


}
