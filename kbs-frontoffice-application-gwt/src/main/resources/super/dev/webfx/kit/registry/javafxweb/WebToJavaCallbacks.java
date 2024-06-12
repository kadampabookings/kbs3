package dev.webfx.kit.registry.javafxweb;

import dev.webfx.extras.webview.pane.WebViewPane;
import jsinterop.annotations.JsFunction;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import one.modality.ecommerce.payment.ui.PaymentUI;
import one.modality.event.frontoffice.activities.booking.map.DynamicMapView;
import one.modality.event.frontoffice.activities.booking.map.MapMarker;

/**
 * Temporarily hardcoded. TODO: implement callback feature in WebFX CLI from webfx.xml declaration.
 *
 * @author Bruno Salmon
 */
public class WebToJavaCallbacks {

    public static void bindCallbackMethods(Object javaInstance) {
        JsPropertyMap<Object> pm = Js.asPropertyMap(javaInstance);
        if (javaInstance instanceof DynamicMapView) {
            DynamicMapView dynamicMapView = (DynamicMapView) javaInstance;
            pm.set("onGoogleMapLoaded", (JsVoidFn0Arg) dynamicMapView::onGoogleMapLoaded);
            pm.set("onMarkerClicked", (JsVoidFn1Arg<MapMarker>) dynamicMapView::onMarkerClicked);
        } else if (javaInstance instanceof WebViewPane) {
            WebViewPane webViewPane = (WebViewPane) javaInstance;
            pm.set("consoleLog", (JsVoidFn1Arg<String>) webViewPane::consoleLog);
            pm.set("consoleError", (JsVoidFn1Arg<String>) webViewPane::consoleError);
            pm.set("consoleWarn", (JsVoidFn1Arg<String>) webViewPane::consoleWarn);
        } else if (javaInstance instanceof PaymentUI) {
            PaymentUI paymentUI = (PaymentUI) javaInstance;
            pm.set("onGatewaySuccess", (JsVoidFn0Arg) paymentUI::onGatewaySuccess);
            pm.set("onGatewayFailure", (JsVoidFn1Arg<String>) paymentUI::onGatewayFailure);
        }
    }

    @JsFunction
    public interface JsVoidFn0Arg {
        void apply();
    }

    @JsFunction
    public interface JsVoidFn1Arg<T> {
        void apply(T a1);
    }


    /*-{
        switch (javaClassName) {
            case 'one.modality.event.frontoffice.activities.booking.map.DynamicMapView':
                javaInstance.consoleLog = javaInstance.@one.modality.event.frontoffice.activities.booking.map.DynamicMapView::consoleLog(Ljava/lang/String;).bind(javaInstance);
                javaInstance.consoleWarn = javaInstance.@one.modality.event.frontoffice.activities.booking.map.DynamicMapView::consoleWarn(Ljava/lang/String;).bind(javaInstance);
                javaInstance.consoleError = javaInstance.@one.modality.event.frontoffice.activities.booking.map.DynamicMapView::consoleError(Ljava/lang/String;).bind(javaInstance);
                javaInstance.onGoogleMapLoaded = javaInstance.@one.modality.event.frontoffice.activities.booking.map.DynamicMapView::onGoogleMapLoaded().bind(javaInstance);
                javaInstance.onMarkerClicked = javaInstance.@one.modality.event.frontoffice.activities.booking.map.DynamicMapView::onMarkerClicked(Lone/modality/event/frontoffice/activities/booking/map/MapMarker;).bind(javaInstance);
                break;
        }
    }-*/;


}
