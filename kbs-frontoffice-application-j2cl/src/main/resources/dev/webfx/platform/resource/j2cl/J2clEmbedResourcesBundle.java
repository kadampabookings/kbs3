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

    @Source("/dev/webfx/stack/i18n/ca.properties")
    TextResource r4();

    @Source("/dev/webfx/stack/i18n/de.properties")
    TextResource r5();

    @Source("/dev/webfx/stack/i18n/en.json")
    TextResource r6();

    @Source("/dev/webfx/stack/i18n/es.properties")
    TextResource r7();

    @Source("/dev/webfx/stack/i18n/fr.properties")
    TextResource r8();

    @Source("/dev/webfx/stack/i18n/pt.properties")
    TextResource r9();

    @Source("/dev/webfx/stack/i18n/sp.properties")
    TextResource r10();

    @Source("/dev/webfx/stack/i18n/vi.properties")
    TextResource r11();

    @Source("/one/modality/base/shared/domainmodel/DomainModelSnapshot.json")
    TextResource r12();

    @Source("/one/modality/event/frontoffice/activities/booking/map/DynamicMapView.js")
    TextResource r13();

    final class ProvidedJ2clResourceBundle extends J2clResourceBundleBase {
        public ProvidedJ2clResourceBundle() {
            registerResource("dev/webfx/extras/webview/pane/WebViewPane.js", () -> R.r1().getText());
            registerResource("dev/webfx/platform/conf/src-root.properties", () -> R.r2().getText());
            registerResource("dev/webfx/platform/meta/exe/exe.properties", () -> R.r3().getText());
            registerResource("dev/webfx/stack/i18n/ca.properties", () -> R.r4().getText());
            registerResource("dev/webfx/stack/i18n/de.properties", () -> R.r5().getText());
            registerResource("dev/webfx/stack/i18n/en.json", () -> R.r6().getText());
            registerResource("dev/webfx/stack/i18n/es.properties", () -> R.r7().getText());
            registerResource("dev/webfx/stack/i18n/fr.properties", () -> R.r8().getText());
            registerResource("dev/webfx/stack/i18n/pt.properties", () -> R.r9().getText());
            registerResource("dev/webfx/stack/i18n/sp.properties", () -> R.r10().getText());
            registerResource("dev/webfx/stack/i18n/vi.properties", () -> R.r11().getText());
            registerResource("one/modality/base/shared/domainmodel/DomainModelSnapshot.json", () -> R.r12().getText());
            registerResource("one/modality/event/frontoffice/activities/booking/map/DynamicMapView.js", () -> R.r13().getText());
        }
    }
}