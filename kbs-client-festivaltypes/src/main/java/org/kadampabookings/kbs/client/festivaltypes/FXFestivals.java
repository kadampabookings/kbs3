package org.kadampabookings.kbs.client.festivaltypes;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.cache.client.LocalStorageCache;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.entity.EntityList;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.EntityStoreQuery;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import one.modality.base.shared.entities.Event;

import java.time.LocalDate;
import java.time.Year;


/**
 * @author Bruno Salmon
 */
public final class FXFestivals {

    private static final ObservableList<Event> LAST_FESTIVALS = FXCollections.observableArrayList(); // Will hold last Spring, Summer & Fall festivals

    // Last in-person Festivals
    private static final ObjectProperty<Event> LAST_SPRING_FESTIVAL_PROPERTY = new SimpleObjectProperty<>();
    private static final ObjectProperty<Event> LAST_SUMMER_FESTIVAL_PROPERTY = new SimpleObjectProperty<>();
    private static final ObjectProperty<Event> LAST_FALL_FESTIVAL_PROPERTY = new SimpleObjectProperty<>();

    // Last online Festivals
    private static final ObjectProperty<Event> LAST_ONLINE_SPRING_FESTIVAL_PROPERTY = new SimpleObjectProperty<>();
    private static final ObjectProperty<Event> LAST_ONLINE_SUMMER_FESTIVAL_PROPERTY = new SimpleObjectProperty<>();
    private static final ObjectProperty<Event> LAST_ONLINE_FALL_FESTIVAL_PROPERTY = new SimpleObjectProperty<>();

    public static ObservableList<Event> lastFestivals() {
        return LAST_FESTIVALS;
    }

    public static ReadOnlyObjectProperty<Event> lastSpringFestivalProperty() {
        return LAST_SPRING_FESTIVAL_PROPERTY;
    }

    public static Event getLastSpringFestival() {
        return LAST_SPRING_FESTIVAL_PROPERTY.get();
    }

    public static ReadOnlyObjectProperty<Event> lastSummerFestivalProperty() {
        return LAST_SUMMER_FESTIVAL_PROPERTY;
    }

    public static Event getLastSummerFestival() {
        return LAST_SUMMER_FESTIVAL_PROPERTY.get();
    }

    public static ReadOnlyObjectProperty<Event> lastFallFestivalProperty() {
        return LAST_FALL_FESTIVAL_PROPERTY;
    }

    public static Event getLastFallFestival() {
        return LAST_FALL_FESTIVAL_PROPERTY.get();
    }

    public static ReadOnlyObjectProperty<Event> lastOnlineSpringFestivalProperty() {
        return LAST_ONLINE_SPRING_FESTIVAL_PROPERTY;
    }

    public static Event getLastOnlineSpringFestival() {
        return LAST_ONLINE_SPRING_FESTIVAL_PROPERTY.get();
    }

    public static ReadOnlyObjectProperty<Event> lastOnlineSummerFestivalProperty() {
        return LAST_ONLINE_SUMMER_FESTIVAL_PROPERTY;
    }

    public static Event getLastOnlineSummerFestival() {
        return LAST_ONLINE_SUMMER_FESTIVAL_PROPERTY.get();
    }

    public static ReadOnlyObjectProperty<Event> lastOnlineFallFestivalProperty() {
        return LAST_ONLINE_FALL_FESTIVAL_PROPERTY;
    }

    public static Event getLastOnlineFallFestival() {
        return LAST_ONLINE_FALL_FESTIVAL_PROPERTY.get();
    }

    public static ReadOnlyObjectProperty<Event> lastFestivalProperty(FestivalType festivalType, boolean online) {
        if (festivalType != null) {
            switch (festivalType) {
                case SPRING_FESTIVAL: return online ? lastOnlineSpringFestivalProperty() : lastSpringFestivalProperty();
                case SUMMER_FESTIVAL: return online ? lastOnlineSummerFestivalProperty() : lastSummerFestivalProperty();
                case FALL_FESTIVAL  : return online ? lastOnlineFallFestivalProperty()   : lastFallFestivalProperty();
            }
        }
        return null;
    }

    private static void loadLastFestivals() {
        LocalDate nextYear = Year.of(LocalDate.now().getYear() + 1).atDay(1);
        EntityStore entityStore = EntityStore.create(DataSourceModelService.getDefaultDataSourceModel());
        String select = "select name,type.name,startDate,endDate,kbs3,state,live,openingDate,bookingFormUrl from Event where type in (?) and startDate < ? order by startDate desc, name like '%Online%' ? 1 : 0 limit 2";
        entityStore.executeCachedQueryBatch(
                LocalStorageCache.get().getCacheEntry("cache-last-festivals"), FXFestivals::onLastFestivalsLoaded,
                new EntityStoreQuery(select, new Object[]{FestivalType.SPRING_FESTIVAL.getTypeId(), nextYear}),
                new EntityStoreQuery(select, new Object[]{FestivalType.SUMMER_FESTIVAL.getTypeId(), nextYear}),
                new EntityStoreQuery(select, new Object[]{FestivalType.FALL_FESTIVAL.getTypeId(), nextYear}))
            .onFailure(Console::log)
            .onSuccess(FXFestivals::onLastFestivalsLoaded);
    }

    private static void onLastFestivalsLoaded(EntityList[] festivalsLists) {
        UiScheduler.runInUiThread(() -> {
            LAST_SPRING_FESTIVAL_PROPERTY.set((Event) Collections.get(festivalsLists[0], 0)); // Spring Festival
            LAST_SUMMER_FESTIVAL_PROPERTY.set((Event) Collections.get(festivalsLists[1], 0)); // Summer Festival
            LAST_FALL_FESTIVAL_PROPERTY.set((Event) Collections.get(festivalsLists[2], 0)); // Fall Festival
            LAST_FESTIVALS.setAll(getLastSpringFestival(), getLastSummerFestival(), getLastFallFestival());
            LAST_ONLINE_SPRING_FESTIVAL_PROPERTY.set((Event) Collections.get(festivalsLists[0], 1)); // Spring Festival
            LAST_ONLINE_SUMMER_FESTIVAL_PROPERTY.set((Event) Collections.get(festivalsLists[1], 1)); // Summer Festival
            LAST_ONLINE_FALL_FESTIVAL_PROPERTY.set((Event) Collections.get(festivalsLists[2], 1));  // Fall Festival
            LAST_FESTIVALS.setAll(getLastSpringFestival(), getLastSummerFestival(), getLastFallFestival());
        });
    }

    public static void init() {
    }

    static {
        loadLastFestivals();
    }
}
