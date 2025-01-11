package org.kadampabookings.kbs.client.festivaltypes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;

/**
 * @author Bruno Salmon
 */
public enum FestivalType {

    SPRING_FESTIVAL(35, FestivalTypeI18nKeys.SpringFestival, FestivalTypeI18nKeys.Spring, "spring-festival", Month.MAY, 20, 5),

    SUMMER_FESTIVAL(36, FestivalTypeI18nKeys.SummerFestival, FestivalTypeI18nKeys.Summer, "summer-festival", Month.JULY, 20, 15),

    FALL_FESTIVAL(37, FestivalTypeI18nKeys.FallFestival, FestivalTypeI18nKeys.Fall, "fall-festival", Month.OCTOBER, 1, 6);

    private static final int FESTIVAL_ITEM_PRIMARY_KEY = 37;

    private final int typeId;
    private final String longI18nKey;
    private final String shortI18nKey;
    private final String styleClass;
    private final Month usualStartMonth;
    private final int earliestMonthDay;
    private final int usualDuration;

    FestivalType(int typeId, String longI18nKey, String shortI18nKey, String styleClass, Month usualStartMonth, int earliestMonthDay, int usualDuration) {
        this.typeId = typeId;
        this.longI18nKey = longI18nKey;
        this.shortI18nKey = shortI18nKey;
        this.styleClass = styleClass;
        this.usualStartMonth = usualStartMonth;
        this.earliestMonthDay = earliestMonthDay;
        this.usualDuration = usualDuration;
    }

    public int getTypeId() {
        return typeId;
    }

    public String getLongI18nKey() {
        return longI18nKey;
    }

    public String getShortI18nKey() {
        return shortI18nKey;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public LocalDate evaluateStartDate(int year) {
        // Always start on Friday => returning the next Friday after the earliest typical date
        LocalDate date = LocalDate.of(year, usualStartMonth, earliestMonthDay);
        while (date.getDayOfWeek() != DayOfWeek.FRIDAY)
            date = date.plusDays(1);
        return date;
    }

    public LocalDate evaluateEndDate(LocalDate startDate) {
        return startDate.plusDays(usualDuration);
    }

    public static FestivalType fromTypeId(int typeId) {
        return Arrays.stream(FestivalType.values()).filter(ft -> ft.typeId == typeId).findFirst().orElse(null);
    }

    public static int getFestivalItemPrimaryKey() {
        return FESTIVAL_ITEM_PRIMARY_KEY;
    }
}
