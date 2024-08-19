package org.kadampabookings.kbs.server.jobs.newsimport;

import dev.webfx.extras.webtext.util.WebTextUtil;
import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.Fetch;
import dev.webfx.platform.fetch.Response;
import dev.webfx.platform.fetch.json.JsonFetch;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.platform.util.time.Times;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.UpdateStore;
import one.modality.base.shared.entities.News;
import one.modality.base.shared.entities.Topic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public class NewsImportJob implements ApplicationJob {

    private static final long IMPORT_PERIODICITY_MILLIS = 3600 * 1000; // every 1h
    private static final int RECHECK_LATEST_DB_NEWS_UPDATES_COUNT = 5;
    private static final String[] LANGUAGES = {"en", "fr"};
    private static final String NEWS_FETCH_URL = "https://kadampa.org/wp-json/wp/v2/posts";
    private static final String MEDIA_FETCH_URL = "https://kadampa.org/wp-json/wp/v2/media";

    private final DataSourceModel dataSourceModel = DataSourceModelService.getDefaultDataSourceModel();
    private Scheduled importTimer;
    private List<Topic> newsTopics;
    private LocalDateTime latestNewsDateTime;

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

    private void importNews() {
        if (newsTopics != null) {
            latestNewsDateTime = null;
            importLangNews(LANGUAGES[0]);
        } else {
            EntityStore.create(dataSourceModel).<Topic>executeQuery("select id,channelTopicId from Topic where channelTopicId!=null")
                .onFailure(error -> Console.log("[NEWS_IMPORT] Error while reading news topics", error))
                .onSuccess(dbNewsTopics -> {
                    newsTopics = dbNewsTopics;
                    importNews();
                });
        }
    }

    public void importLangNews(String lang) {
        // When this job starts, fetchAfterParameter is not set yet, so we initialize it with the latest news date
        // imported so far in the database.
        if (latestNewsDateTime == null) {
            EntityStore.create(dataSourceModel).<News>executeQuery("select date from News where lang=? order by date desc limit ?", lang, Math.max(RECHECK_LATEST_DB_NEWS_UPDATES_COUNT, 1))
                .onFailure(error -> Console.log("[NEWS_IMPORT] ⛔️️ Error while reading latest news", error))
                .onSuccess(news -> {
                    News lastestNews = Collections.last(news);
                    if (lastestNews == null) { // Means that there is no news in the database
                        latestNewsDateTime = LocalDate.of(2000, 1, 1).atStartOfDay(); // The web service raise an error with dates before 2000
                    } else {
                        latestNewsDateTime = lastestNews.getDate();
                    }
                    // Now that fetchAfterParameter is set, we can call importNews() again.
                    importLangNews(lang);
                });
            return;
        }
        // Creating the final fetch url with the additional query string (note: the number of news returned by the
        // web service is 10 by default; this could be increased using &per_page=100 - 100 is the maximal value
        // authorized by the web service)
        String fetchUrl = NEWS_FETCH_URL + "?lang=" + lang + "&order=asc&after=" + Times.formatIso(latestNewsDateTime);
        //Console.log("[NEWS_IMPORT] Fetching " + fetchUrl);
        JsonFetch.fetchJsonArray(fetchUrl)
            .onFailure(error -> Console.log("[NEWS_IMPORT] ⛔️️ Error while fetching " + fetchUrl, error))
            .onSuccess(webNewsJsonArray -> EntityStore.create(dataSourceModel).<News>executeQuery(
                            "select channelNewsId from News where channelNewsId in (" + getIds(webNewsJsonArray) + ")")
                .onFailure(e -> Console.log("[NEWS_IMPORT] ⛔️️ Error while reading news from database", e))
                .onSuccess(dbNews -> {

                    // Checking if there are new news to import from the website, and collecting their media ids (will need to be fetched)
                    List<ReadOnlyAstObject> newWebsiteNews = new ArrayList<>();
                    List<String> mediaIds = new ArrayList<>();
                    for (int i = 0; i < webNewsJsonArray.size(); i++) {
                        ReadOnlyAstObject websiteNewsJson = webNewsJsonArray.getObject(i);
                        Integer id = websiteNewsJson.getInteger("id");
                        // We filter only the news not yet imported in the database
                        if (RECHECK_LATEST_DB_NEWS_UPDATES_COUNT > 0 || dbNews.stream().noneMatch(news -> Objects.equals(id, news.getChannelNewsId()))) {
                            newWebsiteNews.add(websiteNewsJson);
                            String mediaId = websiteNewsJson.getString("featured_media");
                            mediaIds.add(mediaId);
                        }
                    }

                    if (newWebsiteNews.isEmpty()) {
                        Console.log("[NEWS_IMPORT] ✅  No more news to import for lang=" + lang);
                        int langIndex = Arrays.indexOf(LANGUAGES, lang);
                        latestNewsDateTime = null;
                        if (langIndex >= 0 && langIndex < LANGUAGES.length - 1)
                            importLangNews(LANGUAGES[langIndex + 1]);
                        return;
                    }

                    // Creating a batch of those media ids
                    Batch<String> mediaIdsInputBatch = new Batch<>(mediaIds.toArray(new String[0]));
                    // Executing all individual media fetches in parallel
                    mediaIdsInputBatch.executeParallel(ReadOnlyAstObject[]::new, mediaId ->
                                    mediaId == null ? Future.succeededFuture(null) : JsonFetch.fetchJsonObject(MEDIA_FETCH_URL + "/" + mediaId))
                        .onFailure(e -> Console.log("[NEWS_IMPORT] ⛔️️ Error while fetching news medias", e))
                        .onSuccess(webMediasJsonBatch -> {

                            // Preparing the update store for the database submit
                            UpdateStore updateStore = UpdateStore.createAbove(dbNews.getStore());
                            LocalDateTime maxNewsDateTime = latestNewsDateTime;

                            // Creating or updating the new News entities to insert in the database
                            ReadOnlyAstObject[] mediasJsonArray = webMediasJsonBatch.getArray();
                            List<String> mediaLinks = new ArrayList<>();
                            List<News> newsList = new ArrayList<>();
                            for (int i = 0; i < mediasJsonArray.length; i++) {
                                ReadOnlyAstObject newsJson = newWebsiteNews.get(i);
                                int id = newsJson.getInteger("id");
                                // We filter only the news not yet imported in the database
                                News dbn = dbNews.stream().filter(news -> Objects.equals(id, news.getChannelNewsId())).findFirst().orElse(null);
                                News n = dbn != null ? updateStore.updateEntity(dbn) : updateStore.insertEntity(News.class);
                                n.setChannel(1);
                                n.setLang(lang);
                                n.setChannelNewsId(id);
                                LocalDateTime dateTime = Times.parseIsoLocalDateTime(newsJson.getString("date"));
                                if (dateTime.isAfter(maxNewsDateTime))
                                    maxNewsDateTime = dateTime;
                                n.setDate(dateTime);
                                String imageUrl = AST.lookupString(mediasJsonArray[i], "media_details.sizes.medium_large.source_url");
                                if (imageUrl == null)
                                    imageUrl = AST.lookupString(mediasJsonArray[i], "media_details.sizes.full.source_url");
                                n.setImageUrl(cleanUrl(imageUrl));
                                n.setTitle(WebTextUtil.unescapeHtml(AST.lookupString(newsJson, "title.rendered")));
                                n.setExcerpt(WebTextUtil.unescapeHtml(AST.lookupString(newsJson, "excerpt.rendered")));
                                String linkUrl = cleanUrl(AST.lookupString(newsJson, "guid.rendered"));
                                if (!"en".equals(lang))
                                    linkUrl = linkUrl.replace("kadampa.org", "kadampa.org/" + lang);
                                n.setLinkUrl(linkUrl);
                                ReadOnlyAstArray projectArray = newsJson.getArray("project");
                                if (projectArray != null && !projectArray.isEmpty()) {
                                    String channelTopicId = projectArray.getString(0);
                                    if (channelTopicId != null) {
                                        newsTopics.stream()
                                                .filter(t -> Objects.equals(channelTopicId, t.getChannelTopicId()))
                                                .findFirst()
                                                .ifPresent(n::setTopic);
                                    }
                                }
                                mediaLinks.add(cleanUrl(newsJson.getString("link")));
                                newsList.add(n);
                            }

                            LocalDateTime finalMaxNewsDateTime = maxNewsDateTime;

                            updateStore.submitChanges()
                                .onFailure(e -> Console.log("[NEWS_IMPORT] ⛔️️ Error while inserting news in database", e))
                                .onSuccess(insertBatch -> {
                                    int newNewsCount = insertBatch.getArray()[0].getRowCount();
                                    latestNewsDateTime = finalMaxNewsDateTime;
                                    Console.log(newNewsCount + " new news imported in database, latestNewsDateTime = " + latestNewsDateTime);
                                    new Batch<>(mediaLinks.toArray(new String[0])).executeParallel(String[]::new,
                                                mediaLink ->Fetch.fetch(mediaLink).compose(Response::text)
                                                .onSuccess(text -> {
                                                    boolean withVideos = text != null && (text.contains("wistia") || text.contains("src=\"https://www.youtube.com/"));
                                                    News n = newsList.get(mediaLinks.indexOf(mediaLink));
                                                    Object newsPrimaryKey = n.getPrimaryKey();
                                                    //Console.log("News " + newsPrimaryKey + " has videos: " + withVideos);
                                                    if (withVideos) {
                                                        UpdateStore updateStore2 = UpdateStore.create(dataSourceModel);
                                                        News news = updateStore2.updateEntity(EntityId.create(News.class, newsPrimaryKey));
                                                        news.setFieldValue("withVideos", true);
                                                        updateStore2.submitChanges()
                                                                .onFailure(e -> Console.log("[NEWS_IMPORT] ⛔️️ Error while updating news withVideo", e))
                                                                .onSuccess(ignored -> Console.log("[NEWS_IMPORT] News " + newsPrimaryKey + " withVideos updated"));
                                                    }
                                                }))
                                        .onFailure(e -> Console.log("[NEWS_IMPORT] ⛔️️ Error while updating news withVideo", e))
                                        .onSuccess(v -> importLangNews(lang));
                                });
                        });
                }));
    }

    private static String cleanUrl(String url) {
        return url == null ? null : WebTextUtil.unescapeUnicodes(url).replace("\\", "");
    }

    private static String getIds(ReadOnlyAstArray array) {
        if (array.isEmpty())
            return "0";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.size(); i++) {
            ReadOnlyAstObject websiteNewsJson = array.getObject(i);
            Integer id = websiteNewsJson.getInteger("id");
            if (i > 0)
                sb.append(", ");
            sb.append(id);
        }
        return sb.toString();
    }

}
