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
import dev.webfx.platform.util.Dates;
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

    private static final String[] languages = {"en", "fr"};
    private static final String NEWS_FETCH_URL = "https://kadampa.org/wp-json/wp/v2/posts";
    private static final String MEDIA_FETCH_URL = "https://kadampa.org/wp-json/wp/v2/media";
    private static final long IMPORT_PERIODICITY_MILLIS = 3600 * 1000; // every 1h
    private Scheduled importTimer;
    private final DataSourceModel dataSourceModel = DataSourceModelService.getDefaultDataSourceModel();
    private List<Topic> newsTopics;
    private LocalDateTime latestNewsDateTime;
    private int latestChannelNewsId;


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
        if (newsTopics != null)
            importLangNews("en");
        else {
            EntityStore.create(dataSourceModel).<Topic>executeQuery("select id,channelTopicId from Topic where channelTopicId!=null")
                    .onFailure(error -> Console.log("Error while reading news topics", error))
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
            EntityStore.create(dataSourceModel).<News>executeQuery("select date,channelNewsId from News where lang=? order by channelNewsId desc, date desc limit 1", lang)
                    .onFailure(error -> Console.log("Error while reading latest news", error))
                    .onSuccess(news -> {
                        if (news.isEmpty()) { // Means that there is no news in the database
                            latestNewsDateTime = LocalDate.of(2000, 1, 1).atStartOfDay(); // The web service raise an error with dates before 2000
                            latestChannelNewsId = 0;
                        } else {
                            News lastestNews = news.get(0);
                            latestNewsDateTime = lastestNews.getDate().atStartOfDay();
                            latestChannelNewsId = lastestNews.getChannelNewsId();
                        }
                        // Now that fetchAfterParameter is set, we can call importNews() again.
                        importLangNews(lang);
                    });
            return;
        }
        // Creating the final fetch url with the additional query string (note: the number of news returned by the
        // web service is 10 by default; this could be increased using &per_page=100 - 100 is the maximal value
        // authorized by the web service)
        String fetchUrl = NEWS_FETCH_URL + "?lang=" + lang + "&order=asc&after=" + Dates.formatIso(latestNewsDateTime);
        JsonFetch.fetchJsonArray(fetchUrl)
                .onFailure(error -> Console.log("Error while fetching " + fetchUrl, error))
                .onSuccess(webNewsJsonArray -> EntityStore.create(dataSourceModel).<News>executeQuery(
                                "select channelNewsId from News where lang=? and channelNewsId >= ? order by date limit ?", lang, latestChannelNewsId, webNewsJsonArray.size()
                        )
                        .onFailure(e -> Console.log("Error while reading news from database", e))
                        .onSuccess(dbNews -> {

                            // Checking if there are new news to import from the website, and collecting their media ids (will need to be fetched)
                            List<ReadOnlyAstObject> newWebsiteNews = new ArrayList<>();
                            List<String> mediaIds = new ArrayList<>();
                            for (int i = 0; i < webNewsJsonArray.size(); i++) {
                                ReadOnlyAstObject websiteNewsJson = webNewsJsonArray.getObject(i);
                                String id = websiteNewsJson.getString("id");
                                // We filter only the news not yet imported in the database
                                if (dbNews.stream().noneMatch(news -> Objects.equals(id, news.getChannelNewsId()))) {
                                    newWebsiteNews.add(websiteNewsJson);
                                    mediaIds.add(websiteNewsJson.getString("featured_media"));
                                }
                            }

                            if (newWebsiteNews.isEmpty()) {
                                Console.log("No new news to import for lang=" + lang);
                                int langIndex = Arrays.indexOf(languages, lang);
                                if (langIndex >= 0 && langIndex < languages.length - 1) {
                                    latestNewsDateTime = null;
                                    importLangNews(languages[langIndex + 1]);
                                }
                                return;
                            }

                            // Creating a batch of those media ids
                            Batch<String> mediaIdsInputBatch = new Batch<>(mediaIds.toArray(new String[0]));
                            // Executing all individual media fetches in parallel
                            mediaIdsInputBatch.executeParallel(ReadOnlyAstObject[]::new, mediaId ->
                                            mediaId == null ? Future.succeededFuture(null) : JsonFetch.fetchJsonObject(MEDIA_FETCH_URL + "/" + mediaId))
                                    .onFailure(e -> Console.log("Error while fetching news medias", e))
                                    .onSuccess(webMediasJsonBatch -> {

                                        // Preparing the update store for the database submit
                                        UpdateStore updateStore = UpdateStore.createAbove(dbNews.getStore());
                                        LocalDateTime maxNewsDateTime = latestNewsDateTime;
                                        int maxChannelNewsId = latestChannelNewsId;

                                        // Creating the new News entities to insert in the database
                                        ReadOnlyAstObject[] mediasJsonArray = webMediasJsonBatch.getArray();
                                        List<String> directLinkUrlsForVideoCheck = new ArrayList<>();
                                        for (int i = 0; i < mediasJsonArray.length; i++) {
                                            ReadOnlyAstObject newsJson = newWebsiteNews.get(i);
                                            int id = newsJson.getInteger("id");
                                            News n = updateStore.insertEntity(News.class);
                                            n.setChannel(1);
                                            n.setChannelNewsId(id);
                                            maxChannelNewsId = Math.max(maxChannelNewsId, id);
                                            n.setLang(lang);
                                            LocalDateTime dateTime = Dates.parseIsoLocalDateTime(newsJson.getString("date"));
                                            if (dateTime.isAfter(maxNewsDateTime))
                                                maxNewsDateTime = dateTime;
                                            n.setDate(LocalDate.from(dateTime));
                                            n.setImageUrl(cleanUrl(AST.lookupString(mediasJsonArray[i], "media_details.sizes.medium_large.source_url")));
                                            if (n.getImageUrl() == null)
                                                n.setImageUrl(cleanUrl(AST.lookupString(mediasJsonArray[i], "media_details.sizes.full.source_url")));
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
                                                    Topic newsTopic = newsTopics.stream().filter(t -> Objects.equals(channelTopicId, t.getChannelTopicId())).findFirst().orElse(null);
                                                    if (newsTopic != null)
                                                        n.setTopic(newsTopic);
                                                }
                                            }
                                            directLinkUrlsForVideoCheck.add(cleanUrl(newsJson.getString("link")));
                                        }

                                        LocalDateTime finalMaxNewsDateTime = maxNewsDateTime;
                                        int finalMaxChannelNewsId = maxChannelNewsId;

                                        updateStore.submitChanges()
                                                .onFailure(e -> Console.log("Error while inserting news in database", e))
                                                .onSuccess(insertBatch -> {
                                                    int newNewsCount = insertBatch.getArray().length;
                                                    Console.log(newNewsCount + " new news imported in database");
                                                    latestNewsDateTime = finalMaxNewsDateTime;
                                                    latestChannelNewsId = finalMaxChannelNewsId;
                                                    // Updating the newly imported new by setting 1) withVideos field & 2) reading translations
                                                    for (int i = 0; i < newNewsCount; i++) {
                                                        Object newsPrimaryKey = insertBatch.getArray()[i].getGeneratedKeys()[0];
                                                        // 1) Setting withVideos field
                                                        String linkUrl = directLinkUrlsForVideoCheck.get(i);
                                                        if (linkUrl != null) {
                                                            Fetch.fetch(linkUrl).compose(Response::text).onSuccess(text -> {
                                                                boolean withVideos = text != null && text.contains("wistia");
                                                                Console.log("News " + newsPrimaryKey + " has videos: " + withVideos);
                                                                if (withVideos) {
                                                                    UpdateStore updateStore2 = UpdateStore.create(dataSourceModel);
                                                                    News news = updateStore2.updateEntity(EntityId.create(News.class, newsPrimaryKey));
                                                                    news.setFieldValue("withVideos", true);
                                                                    updateStore2.submitChanges()
                                                                            .onFailure(Console::log)
                                                                            .onSuccess(x -> Console.log("News " + newsPrimaryKey + " withVideos updated"));
                                                                }
                                                            });
                                                        }
                                                    }
                                                    importLangNews(lang);
                                                });

                                    });
                        }));
    }

    private static String cleanUrl(String url) {
        return url == null ? null : WebTextUtil.unescapeUnicodes(url).replace("\\", "");
    }

}
