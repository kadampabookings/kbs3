// File managed by WebFX (DO NOT EDIT MANUALLY)
package dev.webfx.kit.registry.javafxweb;

import jsinterop.annotations.JsFunction;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

public class WebToJavaCallbacks {

    public static void bindCallbackMethods(Object javaInstance) {
        JsPropertyMap<Object> pm = Js.asPropertyMap(javaInstance);
        if (javaInstance instanceof one.modality.ecommerce.payment.client.WebPaymentForm) {
            one.modality.ecommerce.payment.client.WebPaymentForm castedInstance = (one.modality.ecommerce.payment.client.WebPaymentForm) javaInstance;
            registerClearFn(pm, "onGatewayInitSuccess", (JsVoidFn0Arg) castedInstance::onGatewayInitSuccess);
            registerClearFn(pm, "onGatewayInitFailure", (JsVoidFn1Arg<java.lang.String>) castedInstance::onGatewayInitFailure);
            registerClearFn(pm, "onGatewayCardVerificationFailure", (JsVoidFn1Arg<java.lang.String>) castedInstance::onGatewayCardVerificationFailure);
            registerClearFn(pm, "onGatewayBuyerVerificationFailure", (JsVoidFn1Arg<java.lang.String>) castedInstance::onGatewayBuyerVerificationFailure);
            registerClearFn(pm, "onGatewayPaymentVerificationSuccess", (JsVoidFn1Arg<java.lang.String>) castedInstance::onGatewayPaymentVerificationSuccess);
            registerClearFn(pm, "pay", (JsVoidFn0Arg) castedInstance::pay);
        } else if (javaInstance instanceof dev.webfx.extras.webview.pane.WebViewPane) {
            dev.webfx.extras.webview.pane.WebViewPane castedInstance = (dev.webfx.extras.webview.pane.WebViewPane) javaInstance;
            registerClearFn(pm, "consoleLog", (JsVoidFn1Arg<java.lang.String>) castedInstance::consoleLog);
            registerClearFn(pm, "consoleWarn", (JsVoidFn1Arg<java.lang.String>) castedInstance::consoleWarn);
            registerClearFn(pm, "consoleError", (JsVoidFn1Arg<java.lang.String>) castedInstance::consoleError);
        } else if (javaInstance instanceof dev.webfx.extras.player.video.web.WebVideoPlayerBase) {
            dev.webfx.extras.player.video.web.WebVideoPlayerBase castedInstance = (dev.webfx.extras.player.video.web.WebVideoPlayerBase) javaInstance;
            registerClearFn(pm, "onReady", (JsVoidFn0Arg) castedInstance::onReady);
            registerClearFn(pm, "onPlay", (JsVoidFn0Arg) castedInstance::onPlay);
            registerClearFn(pm, "onPause", (JsVoidFn0Arg) castedInstance::onPause);
            registerClearFn(pm, "onEnd", (JsVoidFn0Arg) castedInstance::onEnd);
        }
    }

    private static void registerClearFn(JsPropertyMap<Object> pm, String name, Object fn) {
        if (!pm.has(name)) { // Skipping when not obfuscated (ex: draft compile pretty) to prevent infinite loop
            pm.set(name, fn);
        }
    }


    @JsFunction
    public interface JsVoidFn0Arg {
        void apply();
    }

    @JsFunction
    public interface JsVoidFn1Arg<T1> {
        void apply(T1 arg1);
    }

}