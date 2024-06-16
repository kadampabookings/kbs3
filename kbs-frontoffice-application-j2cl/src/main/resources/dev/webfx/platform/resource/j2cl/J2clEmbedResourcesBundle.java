// File managed by WebFX (DO NOT EDIT MANUALLY)
package dev.webfx.platform.resource.j2cl;

import org.treblereel.j2cl.processors.annotations.GWT3Resource;
import org.treblereel.j2cl.processors.common.resources.ClientBundle;
import org.treblereel.j2cl.processors.common.resources.TextResource;
import dev.webfx.platform.resource.spi.impl.j2cl.J2clResourceBundleBase;

@GWT3Resource
public interface J2clEmbedResourcesBundle extends ClientBundle {

    J2clEmbedResourcesBundle R = J2clEmbedResourcesBundleImpl.INSTANCE;

    @Source("/dev/webfx/extras/webview/pane/WebViewPane.js")
    TextResource r1();

    @Source("/dev/webfx/platform/conf/src-root.properties")
    TextResource r2();

    @Source("/dev/webfx/platform/meta/exe/exe.properties")
    TextResource r3();

    @Source("/dev/webfx/stack/i18n/en.json")
    TextResource r4();

    @Source("/dev/webfx/stack/i18n/fr.properties")
    TextResource r5();

    @Source("/one/modality/base/shared/domainmodel/DomainModelSnapshot.json")
    TextResource r6();

    @Source("/one/modality/event/frontoffice/activities/booking/map/DynamicMapView.js")
    TextResource r7();

    final class ProvidedJ2clResourceBundle extends J2clResourceBundleBase {
        public ProvidedJ2clResourceBundle() {
            registerResource("dev/webfx/extras/webview/pane/WebViewPane.js", () -> R.r1().getText());
            registerResource("dev/webfx/platform/conf/src-root.properties", () -> R.r2().getText());
            registerResource("dev/webfx/platform/meta/exe/exe.properties", () -> R.r3().getText());
            registerResource("dev/webfx/stack/i18n/en.json", () -> R.r4().getText());
            registerResource("dev/webfx/stack/i18n/fr.properties", () -> R.r5().getText());
            registerResource("one/modality/base/shared/domainmodel/DomainModelSnapshot.json", () -> R.r6().getText());
            registerResource("one/modality/event/frontoffice/activities/booking/map/DynamicMapView.js", () -> R.r7().getText());
        }
    }
}