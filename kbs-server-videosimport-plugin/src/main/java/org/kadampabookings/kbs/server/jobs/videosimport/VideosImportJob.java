package org.kadampabookings.kbs.server.jobs.videosimport;

import dev.webfx.extras.webtext.util.WebTextUtil;
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
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.platform.util.collection.HashList;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.UpdateStore;
import one.modality.base.shared.entities.News;
import one.modality.base.shared.entities.Video;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * @author Bruno Salmon
 */
public class VideosImportJob implements ApplicationJob {

    private static final String NEWS_FETCH_URL = "https://kadampa.org/wp-json/wp/v2/posts";
    private static final String MEDIA_FETCH_URL = "https://kadampa.org/wp-json/wp/v2/media";
    private static final long IMPORT_PERIODICITY_MILLIS = 3600 * 1000; // every 1h
    private final DataSourceModel dataSourceModel = DataSourceModelService.getDefaultDataSourceModel();
    private Scheduled importTimer;
    private News latestImportedNews;

    @Override
    public void onStart() {
        // Waiting 2 mins before starting first import so that the news import job eventually imported some news
        Scheduler.scheduleDelay(2 * 60 * 1000, () -> {
            importNewsVideos();
            importTimer = Scheduler.schedulePeriodic(IMPORT_PERIODICITY_MILLIS, this::importNewsVideos);
        });
    }

    @Override
    public void onStop() {
        if (importTimer != null)
            importTimer.cancel();
    }

    private void importNewsVideos() {
        if (latestImportedNews != null) {
            importNewsVideosFromLatestNews();
        } else {
            EntityStore.create(dataSourceModel).<Video>executeQuery("select id,news from Video order by news.id desc limit 1")
                .onFailure(error -> Console.log("[VIDEOS_IMPORT] ⛔️️ Error while reading latest video from database", error))
                .onSuccess(dbVideos -> {
                    if (!dbVideos.isEmpty()) {
                        latestImportedNews = dbVideos.get(0).getNews();
                    }
                    importNewsVideosFromLatestNews();
                });
        }
    }

    public void importNewsVideosFromLatestNews() {
        // Reading the next 10 news from the database that needs to be checked for videos import
        EntityStore entityStore = EntityStore.create(dataSourceModel);
        entityStore.<News>executeQuery("select channelNewsId,title,excerpt,lang from News where id>=? order by id limit 10", latestImportedNews == null ? 0 : latestImportedNews.getPrimaryKey())
                .onFailure(error -> Console.log("[VIDEOS_IMPORT] ⛔️️ Error while reading latest news from database", error))
                .onSuccess(dbNews -> {
                    News latestNews = dbNews.get(dbNews.size() - 1);
                    // Fetching the news json info from WordPress
                    new Batch<>(dbNews.toArray(new News[0])).executeParallel(ReadOnlyAstObject[]::new,
                                    news -> JsonFetch.fetchJsonObject(NEWS_FETCH_URL + "/" + news.getChannelNewsId()))
                            .onFailure(Console::log)
                            .onSuccess(newsJsonBatch -> { // Note: indexes match dbNews
                                // Extracting the mediaIds and links of these news
                                List<String> mediaIds = new ArrayList<>();
                                List<String> mediaLinks = new ArrayList<>();
                                for (int i = 0; i < newsJsonBatch.length(); i++) {
                                    ReadOnlyAstObject websiteNewsJson = newsJsonBatch.get(i);
                                    mediaIds.add(websiteNewsJson.getString("featured_media"));
                                    mediaLinks.add(cleanUrl(websiteNewsJson.getString("link")));
                                }

                                if (mediaIds.isEmpty()) {
                                    Console.log("[VIDEOS_IMPORT] No more medias to check");
                                    return;
                                }

                                // Fetching the media json info from WordPress
                                new Batch<>(mediaIds.toArray(new String[0])).executeParallel(ReadOnlyAstObject[]::new,
                                                mediaId -> mediaId == null ? Future.succeededFuture(null) :
                                                        JsonFetch.fetchJsonObject(MEDIA_FETCH_URL + "/" + mediaId))
                                        .onFailure(e -> Console.log("[VIDEOS_IMPORT] ⛔️️ Error while fetching news medias", e))
                                        .onSuccess(mediasJsonBatch -> { // Note: indexes match dbNews
                                            // Fetching the mediaLink (the page that contains the media) from WordPress
                                            Map<String, News> wistiaIdsQueue = new HashMap<>();
                                            new Batch<>(mediaLinks.toArray(new String[0])).executeParallel(String[]::new, mediaLink -> mediaLink == null ? Future.succeededFuture("") :
                                                            Fetch.fetch(mediaLink).compose(Response::text)
                                                                    .onSuccess(text -> {
                                                                        List<String> wistiaIds = findWistiaIds(text);
                                                                        synchronized (wistiaIdsQueue) {
                                                                            wistiaIds.forEach(wistiaId -> wistiaIdsQueue.put(wistiaId, dbNews.get(mediaLinks.indexOf(mediaLink))));
                                                                        }
                                                                    }))
                                                    .onFailure(e -> Console.log("[VIDEOS_IMPORT] ⛔️️ Error while fetching media links", e))
                                                    .onSuccess(ignored -> {
                                                        if (wistiaIdsQueue.isEmpty()) {
                                                            continueImport(latestNews);
                                                            return;
                                                        }
                                                        UpdateStore updateStore = UpdateStore.createAbove(entityStore);
                                                        entityStore.<Video>executeQuery(
                                                                        "select wistiaVideoId from Video where wistiaVideoId in (" + Collections.toString(Collections.map(wistiaIdsQueue.keySet(), wistiaId -> "'" + wistiaId + "'"), false, false) + ")")
                                                                .onFailure(e -> Console.log("[VIDEOS_IMPORT] ⛔️️ Error while reading videos from database", e))
                                                                .compose(existingVideos -> {
                                                                    List<String> existingWistiaIds = Collections.map(existingVideos, Video::getWistiaVideoId);
                                                                    List<String> newWistiaIds = Collections.filter(wistiaIdsQueue.keySet(), wistiaId -> !existingWistiaIds.contains(wistiaId));
                                                                    return new Batch<>(newWistiaIds.toArray(new String[0]))
                                                                            .executeParallel(ReadOnlyAstObject[]::new, wistiaId -> JsonFetch.fetchJsonObject("https://fast.wistia.com/embed/medias/" + wistiaId + ".json")
                                                                                    .onFailure(error -> Console.log("[VIDEOS_IMPORT] ⛔️️ Error while fetching wistia media", error))
                                                                                    .onSuccess(wistiaJson -> {
                                                                                        // Reading wistia info
                                                                                        ReadOnlyAstObject media = wistiaJson.getObject("media");
                                                                                        ReadOnlyAstArray assets = media.getArray("assets");
                                                                                        ReadOnlyAstObject originalAsset = findAssetOfType(assets, "original");
                                                                                        if (originalAsset == null)
                                                                                            return;
                                                                                        ReadOnlyAstObject stillImageAsset = findAssetOfType(assets, "still_image");
                                                                                        if (stillImageAsset == null)
                                                                                            stillImageAsset = originalAsset;
                                                                                        News n = wistiaIdsQueue.get(wistiaId);
                                                                                        // Creating video podcast
                                                                                        Video v = updateStore.insertEntity(Video.class);
                                                                                        v.setTitle(media.getString("name"));
                                                                                        v.setNews(n);
                                                                                        String excerpt = n.getTitle() + ". " + n.getExcerpt();
                                                                                        v.setLang(n.getLang());
                                                                                        v.setExcerpt(excerpt);
                                                                                        v.setDate(LocalDateTime.ofEpochSecond(media.getLong("createdAt"), 0, ZoneOffset.UTC));
                                                                                        v.setDurationMillis((long) (media.getDouble("duration") * 1000));
                                                                                        v.setImageUrl(cleanUrl(stillImageAsset.getString("url").replace(".bin", ".jpg")));
                                                                                        v.setWistiaVideoId(wistiaId);
                                                                                        v.setWidth(originalAsset.getInteger("width"));
                                                                                        v.setHeight(originalAsset.getInteger("height"));
                                                                                        int index = dbNews.indexOf(n);
                                                                                        if (index != -1) {
                                                                                            v.setMediaId(mediaIds.get(index));
                                                                                        }
                                                                                    }));
                                                                })
                                                                .onSuccess(ignored3 -> {
                                                                    if (!updateStore.hasChanges()) {
                                                                        continueImport(latestNews);
                                                                    } else {
                                                                        updateStore.submitChanges()
                                                                                .onFailure(error -> Console.log("[VIDEOS_IMPORT] ⛔️️ Error while inserting video in database", error))
                                                                                .onSuccess(insertBatch -> {
                                                                                    int newVideosCount = insertBatch.getArray()[0].getRowCount();
                                                                                    Console.log("[VIDEOS_IMPORT] " + newVideosCount + " new videos imported in database");
                                                                                    continueImport(latestNews);
                                                                                });
                                                                    }
                                                                });
                                                    });
                                        });

                            });

                });
    }

    private void continueImport(News latestNews) {
        if (Entities.sameId(latestNews, latestImportedNews)) {
            Console.log("[VIDEOS_IMPORT] ✅  No more news to import videos from");
        } else {
            latestImportedNews = latestNews;
            importNewsVideosFromLatestNews();
        }
    }

    private static String cleanUrl(String url) {
        return url == null ? null : WebTextUtil.unescapeUnicodes(url).replace("\\", "");
    }

    private static ReadOnlyAstObject findAssetOfType(ReadOnlyAstArray assets, String type) {
        for (int i = 0; i < assets.size(); i++) {
            ReadOnlyAstObject asset = assets.getObject(i);
            if (Objects.equals(asset.getString("type"), type))
                return asset;
        }
        return null;
    }

    private static List<String> findWistiaIds(String text) {
        if (text == null || text.isEmpty())
            return java.util.Collections.emptyList();
        List<String> wistiaIds = new HashList<>();
        int index = 0;
        while (true) {
            index = text.indexOf("wistia_async_", index);
            if (index == -1)
                break;
            index += 13;
            String wistiaId = text.substring(index, text.indexOf(' ', index));
            wistiaIds.add(wistiaId);
        }
        index = 0;
        while (true) {
            index = text.indexOf("https://fast.wistia.net/embed/iframe/", index);
            if (index == -1)
                break;
            index += 37;
            int p = text.length();
            int q = text.indexOf('?', index);
            if (q > index && q < p)
                p = q;
            q = text.indexOf('"', index);
            if (q > index && q < p)
                p = q;
            q = text.indexOf('\'', index);
            if (q > index && q < p)
                p = q;
            String wistiaId = text.substring(index, p);
            wistiaIds.add(wistiaId);
        }
        return wistiaIds;
    }
}
