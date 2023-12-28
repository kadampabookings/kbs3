package org.kadampabookings.kbs.server.jobs.podcastsimport;

import dev.webfx.extras.webtext.util.WebTextUtil;
import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.json.JsonFetch;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.util.Dates;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.UpdateStore;
import one.modality.base.shared.entities.Podcast;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public class PodcastsImportJob implements ApplicationJob {

    private static final String PODCAST_FETCH_URL = "https://kadampa.org/wp-json/wp/v2/podcast";
    private static final long IMPORT_PERIODICITY_MILLIS = 3600 * 1000; // 1h
    private Scheduled importTimer;
    private final DataSourceModel dataSourceModel = DataSourceModelService.getDefaultDataSourceModel();
    private LocalDateTime fetchAfterParameter;
    private final Map<Integer, Integer> seriesToTeacher = Map.of(
            426, 2, // Gen-la Dekyong
            455, 3, // Gen-la Kunsang
            443, 4, // Gen-la Khyenrab
            450, 29, // Gen-la Jampa
            425, 79, // Gen-la Thubten
            427, 84, // Gen Rabten
            551, 98 // Kadam Morten
    );

    @Override
    public void onStart() {
        importPodcasts();
        importTimer = Scheduler.schedulePeriodic(IMPORT_PERIODICITY_MILLIS, this::importPodcasts);
    }

    @Override
    public void onStop() {
        if (importTimer != null)
            importTimer.cancel();
    }

    public void importPodcasts() {
        // When this job starts, there is no fetchAfterParameter, so we initialize it with the latest podcast date
        // imported so far in the database.
        if (fetchAfterParameter == null) {
            EntityStore.create(dataSourceModel).<Podcast>executeQuery("select id,date from Podcast order by date desc limit 1")
                    .onFailure(error -> Console.log("Error while reading latest podcast", error))
                    .onSuccess(podcasts -> {
                        if (podcasts.isEmpty()) // Means that there is no podcast in the database
                            fetchAfterParameter = LocalDate.of(2000, 1, 1).atStartOfDay(); // The web service raise an error with dates before 2000
                        else
                            fetchAfterParameter = podcasts.get(0).getDate().atStartOfDay();
                        // Now that fetchAfterParameter is set, we can call importPodcasts() again.
                        importPodcasts();
                    });
            return;
        }
        // Creating the final fetch url with the additional query string (note: the number of podcasts returned by the
        // web service is 10 by default; this could be increased using &per_page=100 - 100 is the maximal value
        // authorized by the web service)
        String fetchUrl = PODCAST_FETCH_URL + "?order=asc&after=" + Dates.formatIso(fetchAfterParameter);
        JsonFetch.fetchJsonArray(fetchUrl)
                .onFailure(error -> Console.log("Error while fetching " + fetchUrl, error))
                // Fetching the latest podcasts from the database in order to determine those that are not yet imported
                .onSuccess(webPodcastsJsonArray -> EntityStore.create(dataSourceModel).<Podcast>executeQuery(
                                "select channelPodcastId from Podcast where date >= ? order by date limit ?", fetchAfterParameter, webPodcastsJsonArray.size()
                        )
                        .onFailure(e -> Console.log("Error while reading podcasts from database", e))
                        .onSuccess(dbPodcasts -> {

                            UpdateStore updateStore = UpdateStore.createAbove(dbPodcasts.getStore());
                            LocalDateTime maxPodcastDate = fetchAfterParameter;

                            for (int i = 0; i < webPodcastsJsonArray.size(); i++) {
                                ReadOnlyAstObject podcastJson = webPodcastsJsonArray.getObject(i);
                                int id = podcastJson.getInteger("id");

                                // Skipping the podcasts already present in the database
                                if (dbPodcasts.stream().anyMatch(p -> Objects.equals(id, p.getChannelPodcastId())))
                                    continue;

                                Podcast p = updateStore.insertEntity(Podcast.class);
                                p.setChannel(2);
                                p.setChannelPodcastId(id);
                                String title = AST.lookupString(podcastJson, "title.rendered");
                                p.setTitle(WebTextUtil.unescapeHtml(title));
                                String excerpt = AST.lookupString(podcastJson, "excerpt.rendered");
                                p.setExcerpt(WebTextUtil.unescapeHtml(excerpt));
                                LocalDateTime dateTime = Dates.parseIsoLocalDateTime(podcastJson.getString("date"));
                                if (dateTime.isAfter(maxPodcastDate))
                                    maxPodcastDate = dateTime;
                                p.setDate(LocalDate.from(dateTime));
                                String imageUrl = podcastJson.getString("episode_featured_image");
                                if (imageUrl == null || !imageUrl.startsWith("http"))
                                    imageUrl = podcastJson.getString("episode_player_image");
                                p.setImageUrl(cleanUrl(imageUrl));
                                String audioUrl = AST.lookupString(podcastJson, "meta.audio_file");
                                p.setAudioUrl(cleanUrl(audioUrl));
                                try {
                                    String durationString = AST.lookupString(podcastJson, "meta.duration");
                                    Duration duration = Duration.between(LocalTime.MIN, LocalTime.parse(durationString));
                                    p.setDurationMillis(duration.toMillis());
                                } catch (Exception e) {
                                    Console.log("WARNING: No or wrong duration for podcast " + id);
                                }
                                ReadOnlyAstArray series = podcastJson.getArray("series");
                                for (Object s : series) {
                                    if (s instanceof Number) {
                                        Integer teacherId = seriesToTeacher.get(((Number) s).intValue());
                                        if (teacherId != null) {
                                            p.setTeacher(teacherId);
                                            break;
                                        }
                                    }
                                }
                            }

                            LocalDateTime finalMaxPodcastDate = maxPodcastDate;

                            if (!updateStore.hasChanges())
                                Console.log("No new podcasts to import");
                            else
                                updateStore.submitChanges()
                                        .onFailure(e -> Console.log("Error while inserting podcasts in database", e))
                                        .onSuccess(insertBatch -> {
                                            int newPodcastsCount = insertBatch.getArray().length;
                                            Console.log(newPodcastsCount + " new podcasts imported in database");
                                            fetchAfterParameter = finalMaxPodcastDate;
                                            importPodcasts();
                                        });
                        }));
    }

    private static String cleanUrl(String url) {
        return url == null ? null : WebTextUtil.unescapeUnicodes(url).replace("\\", "");
    }

}
