// File managed by WebFX (DO NOT EDIT MANUALLY)
package dev.webfx.platform.resource.gwt;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import dev.webfx.platform.resource.spi.impl.gwt.GwtResourceBundleBase;

public interface GwtEmbedResourcesBundle extends ClientBundle {

    GwtEmbedResourcesBundle R = GWT.create(GwtEmbedResourcesBundle.class);
    @Source("dev/webfx/extras/i18n/de.properties")
    TextResource r1();

    @Source("dev/webfx/extras/i18n/en.json")
    TextResource r2();

    @Source("dev/webfx/extras/i18n/es.properties")
    TextResource r3();

    @Source("dev/webfx/extras/i18n/fr.properties")
    TextResource r4();

    @Source("dev/webfx/extras/i18n/pt.properties")
    TextResource r5();

    @Source("dev/webfx/extras/i18n/vi.properties")
    TextResource r6();

    @Source("dev/webfx/extras/i18n/zh.properties")
    TextResource r7();

    @Source("dev/webfx/extras/player/video/web/videojs/videojs.js")
    TextResource r8();

    @Source("dev/webfx/extras/webview/pane/WebViewPane.js")
    TextResource r9();

    @Source("dev/webfx/platform/conf/src-root.properties")
    TextResource r10();

    @Source("dev/webfx/platform/meta/exe/exe.properties")
    TextResource r11();

    @Source("one/modality/base/shared/domainmodel/DomainModelSnapshot.json")
    TextResource r12();

    @Source("one/modality/event/frontoffice/activities/booking/map/DynamicMapView.js")
    TextResource r13();



    final class ProvidedGwtResourceBundle extends GwtResourceBundleBase {
        public ProvidedGwtResourceBundle() {
            registerResource("dev/webfx/extras/i18n/de.properties", R.r1());
            registerResource("dev/webfx/extras/i18n/en.json", R.r2());
            registerResource("dev/webfx/extras/i18n/es.properties", R.r3());
            registerResource("dev/webfx/extras/i18n/fr.properties", R.r4());
            registerResource("dev/webfx/extras/i18n/pt.properties", R.r5());
            registerResource("dev/webfx/extras/i18n/vi.properties", R.r6());
            registerResource("dev/webfx/extras/i18n/zh.properties", R.r7());
            registerResource("dev/webfx/extras/player/video/web/videojs/videojs.js", R.r8());
            registerResource("dev/webfx/extras/webview/pane/WebViewPane.js", R.r9());
            registerResource("dev/webfx/platform/conf/src-root.properties", R.r10());
            registerResource("dev/webfx/platform/meta/exe/exe.properties", R.r11());
            registerResource("one/modality/base/shared/domainmodel/DomainModelSnapshot.json", R.r12());
            registerResource("one/modality/event/frontoffice/activities/booking/map/DynamicMapView.js", R.r13());

        }
    }
}
