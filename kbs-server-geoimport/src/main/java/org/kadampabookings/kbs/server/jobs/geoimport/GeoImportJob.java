package org.kadampabookings.kbs.server.jobs.geoimport;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.Fetch;
import dev.webfx.platform.fetch.Response;
import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.JsonArray;
import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.json.ReadOnlyJsonObject;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.util.Dates;
import dev.webfx.extras.webtext.util.WebTextUtil;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.UpdateStore;
import one.modality.base.shared.entities.Country;
import one.modality.base.shared.entities.News;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author bjvickers
 */
public class GeoImportJob implements ApplicationJob {

    private static final String GEO_FETCH_URL = "http://api.geonames.org/countryInfoJSON?featureCode=PCLI&username=modality.one";
    private final DataSourceModel dataSourceModel = DataSourceModelService.getDefaultDataSourceModel();

    @Override
    public void onStart() {
        importGeo();
    }

    public void importGeo() {

        Console.log("IMPORTING GEO=============");

        // When this job starts, there is no fetchAfterParameter, so we initialize it with the latest podcast date
        // imported so far in the database.
        /*
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
        */

        EntityStore.create(dataSourceModel).<Country>executeQuery("select id,iso_alpha2,latitude,longitude from Country")

                .onFailure(error -> Console.log(error))
                .onSuccess(countries -> Fetch.fetch(GEO_FETCH_URL)

                                .onFailure(error -> Console.log("Error while fetching " + GEO_FETCH_URL, error))
                                .onSuccess(response -> response.jsonObject()

                                        .onFailure(error -> Console.log("Error while parsing json object from " + GEO_FETCH_URL, error))
                                        .onSuccess(geoJsonObject -> {

                                            JsonArray geonames = (JsonArray) geoJsonObject.getArray("geonames");
                                            for (int i = 0; i < geonames.size(); i++) {
                                                JsonObject geonameCountry = (JsonObject) geonames.getObject(i);
                                                String countryName = geonameCountry.getString("countryName");
                                                String geonameId = geonameCountry.getString("geonameId");

                                                Double north = geonameCountry.getDouble("north");
                                                Double south = geonameCountry.getDouble("south");
                                                Double east = geonameCountry.getDouble("east");
                                                Double west = geonameCountry.getDouble("west");

                                                Console.log("name=" + countryName + ", geonameId=" + geonameId + ", " + east);
                                            }

                                        })));

        // Creating the final fetch url with the additional query string (note: the number of podcasts returned by the
        // web service is 10 by default; this could be increased using &per_page=100 - 100 is the maximal value
        // authorized by the web service)
        /*
        Fetch.fetch(GEO_FETCH_URL)
                .onFailure(error -> Console.log("Error while fetching " + fetchUrl, error))
                .onSuccess(response -> response.jsonArray()
                        .onFailure(error -> Console.log("Error while parsing json array from " + fetchUrl, error))
                        // Fetching the latest news from the database in order to determine those that are not yet imported
                        .onSuccess(webNewsJsonArray -> EntityStore.create(dataSourceModel).<News>executeQuery(
                                        "select channelNewsId from News where date >= ? order by date limit ?", fetchAfterParameter, webNewsJsonArray.size()
                                )
                                .onFailure(e -> Console.log("Error while reading news from database", e))
                                .onSuccess(dbNews -> {

                                    // Collecting the new news and their media ids (will need to be fetched)
                                    List<ReadOnlyJsonObject> newWebNews = new ArrayList<>();
                                    List<String> mediaIds = new ArrayList<>();
                                    for (int i = 0; i < webNewsJsonArray.size(); i++) {
                                        ReadOnlyJsonObject webNewsJson = webNewsJsonArray.getObject(i);
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
                                    mediaIdsInputBatch.executeParallel(JsonObject[]::new, mediaId ->
                                                    mediaId == null ? Future.succeededFuture(null) : Fetch.fetch(MEDIA_FETCH_URL + "/" + mediaId).compose(Response::jsonObject))
                                            .onFailure(e -> Console.log("Error while fetching news medias" , e))
                                            .onSuccess(webMediasJsonBatch -> {

                                                // Preparing the update store for the database submit
                                                UpdateStore updateStore = UpdateStore.createAbove(dbNews.getStore());
                                                LocalDateTime maxNewsDate = fetchAfterParameter;

                                                // Creating the new News entities to insert in the database
                                                JsonObject[] mediasJson = webMediasJsonBatch.getArray();
                                                for (int i = 0; i < mediasJson.length; i++) {
                                                    ReadOnlyJsonObject newsJson = newWebNews.get(i);
                                                    String id = newsJson.getString("id");
                                                    News n = updateStore.insertEntity(News.class);
                                                    n.setChannel(1);
                                                    n.setChannelNewsId(id);
                                                    n.setTitle(WebTextUtil.unescapeHtml(Json.lookupString(newsJson, "title.rendered")));
                                                    n.setExcerpt(WebTextUtil.unescapeHtml(Json.lookupString(newsJson, "excerpt.rendered")));
                                                    LocalDateTime dateTime = Dates.parseIsoLocalDateTime(newsJson.getString("date"));
                                                    if (dateTime.isAfter(maxNewsDate))
                                                        maxNewsDate = dateTime;
                                                    n.setDate(LocalDate.from(dateTime));
                                                    n.setLinkUrl(cleanUrl(Json.lookupString(newsJson, "guid.rendered")));
                                                    n.setImageUrl(cleanUrl(Json.lookupString(mediasJson[i], "media_details.sizes.medium.source_url")));
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
                                })));
         */
    }

    private static String cleanUrl(String url) {
        return url == null ? null : url.replace("\\", "");
    }

}
