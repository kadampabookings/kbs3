package org.kadampabookings.kbs.server.jobs.newsimport;

import dev.webfx.extras.webtext.util.WebTextUtil;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.json.JsonFetch;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.util.Dates;
import dev.webfx.platform.util.keyobject.AST;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.UpdateStore;
import one.modality.base.shared.entities.News;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public class NewsImportJob implements ApplicationJob {

    private static final String NEWS_FETCH_URL = "https://kadampa.org/wp-json/wp/v2/posts";
    private static final String MEDIA_FETCH_URL = "https://kadampa.org/wp-json/wp/v2/media";
    private static final long IMPORT_PERIODICITY_MILLIS = 3600 * 1000; // every 1h
    private Scheduled importTimer;
    private final DataSourceModel dataSourceModel = DataSourceModelService.getDefaultDataSourceModel();
    private LocalDateTime fetchAfterParameter;


    @Override
    public void onStart() {
        importNews();
        importTimer = Scheduler.schedulePeriodic(IMPORT_PERIODICITY_MILLIS, this::importNews);
    }

    @Override
    public void onStop() {
        if (importTimer != null)
            importTimer.cancel();
    }

    public void importNews() {
        // When this job starts, there is no fetchAfterParameter, so we initialize it with the latest podcast date
        // imported so far in the database.
        if (fetchAfterParameter == null) {
            EntityStore.create(dataSourceModel).<News>executeQuery("select id,date from News order by date desc limit 1")
                    .onFailure(error -> Console.log("Error while reading latest podcast", error))
                    .onSuccess(news -> {
                        if (news.isEmpty()) // Means that there is no podcast in the database
                            fetchAfterParameter = LocalDate.of(2000, 1, 1).atStartOfDay(); // The web service raise an error with dates before 2000
                        else
                            fetchAfterParameter = news.get(0).getDate().atStartOfDay();
                        // Now that fetchAfterParameter is set, we can call importNews() again.
                        importNews();
                    });
            return;
        }
        // Creating the final fetch url with the additional query string (note: the number of podcasts returned by the
        // web service is 10 by default; this could be increased using &per_page=100 - 100 is the maximal value
        // authorized by the web service)
        String fetchUrl = NEWS_FETCH_URL + "?order=asc&after=" + Dates.formatIso(fetchAfterParameter);
        JsonFetch.fetchJsonArray(fetchUrl)
                .onFailure(error -> Console.log("Error while fetching " + fetchUrl, error))
                .onSuccess(webNewsJsonArray -> EntityStore.create(dataSourceModel).<News>executeQuery(
                                "select channelNewsId from News where date >= ? order by date limit ?", fetchAfterParameter, webNewsJsonArray.size()
                        )
                        .onFailure(e -> Console.log("Error while reading news from database", e))
                        .onSuccess(dbNews -> {

                            // Collecting the new news and their media ids (will need to be fetched)
                            List<ReadOnlyKeyObject> newWebNews = new ArrayList<>();
                            List<String> mediaIds = new ArrayList<>();
                            for (int i = 0; i < webNewsJsonArray.size(); i++) {
                                ReadOnlyKeyObject webNewsJson = webNewsJsonArray.getObject(i);
                                String id = webNewsJson.getString("id");
                                // Ignoring the news
                                if (dbNews.stream().noneMatch(news -> Objects.equals(id, news.getChannelNewsId()))) {
                                    newWebNews.add(webNewsJson);
                                    mediaIds.add(webNewsJson.getString("featured_media"));
                                }
                            }

                            if (newWebNews.isEmpty()) {
                                Console.log("No new news to import");
                                return;
                            }

                            // Creating a batch of those media ids
                            Batch<String> mediaIdsInputBatch = new Batch<>(mediaIds.toArray(new String[0]));
                            // Execute all individual fetches in parallel
                            mediaIdsInputBatch.executeParallel(ReadOnlyKeyObject[]::new, mediaId ->
                                            mediaId == null ? Future.succeededFuture(null) : JsonFetch.fetchJsonObject(MEDIA_FETCH_URL + "/" + mediaId))
                                    .onFailure(e -> Console.log("Error while fetching news medias", e))
                                    .onSuccess(webMediasJsonBatch -> {

                                        // Preparing the update store for the database submit
                                        UpdateStore updateStore = UpdateStore.createAbove(dbNews.getStore());
                                        LocalDateTime maxNewsDate = fetchAfterParameter;

                                        // Creating the new News entities to insert in the database
                                        ReadOnlyKeyObject[] mediasJson = webMediasJsonBatch.getArray();
                                        for (int i = 0; i < mediasJson.length; i++) {
                                            ReadOnlyKeyObject newsJson = newWebNews.get(i);
                                            String id = newsJson.getString("id");
                                            News n = updateStore.insertEntity(News.class);
                                            n.setChannel(1);
                                            n.setChannelNewsId(id);
                                            n.setTitle(WebTextUtil.unescapeHtml(AST.lookupString(newsJson, "title.rendered")));
                                            n.setExcerpt(WebTextUtil.unescapeHtml(AST.lookupString(newsJson, "excerpt.rendered")));
                                            LocalDateTime dateTime = Dates.parseIsoLocalDateTime(newsJson.getString("date"));
                                            if (dateTime.isAfter(maxNewsDate))
                                                maxNewsDate = dateTime;
                                            n.setDate(LocalDate.from(dateTime));
                                            n.setLinkUrl(cleanUrl(AST.lookupString(newsJson, "guid.rendered")));
                                            n.setImageUrl(cleanUrl(AST.lookupString(mediasJson[i], "media_details.sizes.medium.source_url")));
                                        }

                                        LocalDateTime finalMaxNewsDate = maxNewsDate;

                                        updateStore.submitChanges()
                                                .onFailure(e -> Console.log("Error while inserting news in database", e))
                                                .onSuccess(insertBatch -> {
                                                    int newNewsCount = insertBatch.getArray().length;
                                                    Console.log(newNewsCount + " new news imported in database");
                                                    fetchAfterParameter = finalMaxNewsDate;
                                                    importNews();
                                                });

                                    });
                        }));
    }

    private static String cleanUrl(String url) {
        return url == null ? null : url.replace("\\", "");
    }

}
