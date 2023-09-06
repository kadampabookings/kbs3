package org.kadampabookings.kbs.server.jobs.kdmimport;

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.json.JsonFetch;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.EntityList;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.UpdateStore;
import one.modality.base.shared.entities.KdmCenter;
import dev.webfx.extras.webtext.util.WebTextUtil;
import one.modality.base.shared.entities.Organization;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author bjvickers
 */
public class KdmImportJob implements ApplicationJob {

    private static final String KDM_FETCH_URL = "https://kdm.kadampaweb.org/index.php/business/json";
    private final DataSourceModel dataSourceModel = DataSourceModelService.getDefaultDataSourceModel();
    private static final long IMPORT_PERIODICITY_MILLIS = 1000 * 3600 * 24 * 3; // 3x day
    private Scheduled importTimer;

    @Override
    public void onStart() {
        importKdm();

        // @TODO
        // The following creates an exception - Bruno to investigate
        // importTimer = Scheduler.schedulePeriodic(IMPORT_PERIODICITY_MILLIS, this::importKdm);
    }

    @Override
    public void onStop() {
        if (importTimer != null)
            importTimer.cancel();
    }

    public void importKdm() {

        JsonFetch.fetchJsonArray(KDM_FETCH_URL)

                .onFailure(error -> Console.log("Error while fetching " + KDM_FETCH_URL, error))
                .onSuccess(webKdmJsonArray -> EntityStore.create(dataSourceModel).<KdmCenter>executeQuery("select id,kdmId,name,type,lat,lng from KdmCenter")

                        .onFailure(Console::log)
                        .onSuccess(kdmCenters -> {

                                Set<Integer> kdmIds = kdmCenters.stream().map(KdmCenter::getKdmId).collect(Collectors.toSet());
                                UpdateStore updateStore = UpdateStore.create(dataSourceModel);

                                for (int i = 0; i < webKdmJsonArray.size(); i++) {

                                    ReadOnlyAstObject kdmJson = webKdmJsonArray.getObject(i);
                                    int kdmId = kdmJson.getInteger("id");
                                    if (kdmIds.contains(kdmId)) {

                                        // Update an existing KdmCenter record
                                        KdmCenter kdmCenter = kdmCenters.stream().filter(center -> center.getKdmId() == kdmId).findFirst().get();
                                        kdmCenter = updateStore.updateEntity(kdmCenter);
                                        kdmCenter.setKdmId(kdmJson.getInteger("id"));
                                        kdmCenter.setName(WebTextUtil.unescapeHtml(kdmJson.getString("name")));
                                        if (kdmJson.getDouble("lat") != null) {
                                            kdmCenter.setLat(kdmJson.getDouble("lat").floatValue());
                                        }
                                        if (kdmJson.getDouble("lng") != null) {
                                            kdmCenter.setLng(kdmJson.getDouble("lng").floatValue());
                                        }
                                        kdmCenter.setType(kdmJson.getString("type"));
                                        kdmCenter.setMothercenter(kdmJson.getBoolean("mothercenter"));
                                        kdmCenter.setAddress(WebTextUtil.unescapeHtml(kdmJson.getString("address")));
                                        kdmCenter.setAddress2(WebTextUtil.unescapeHtml(kdmJson.getString("address2")));
                                        kdmCenter.setAddress3(WebTextUtil.unescapeHtml(kdmJson.getString("address3")));
                                        kdmCenter.setCity(WebTextUtil.unescapeHtml(kdmJson.getString("city")));
                                        kdmCenter.setState(WebTextUtil.unescapeHtml(kdmJson.getString("state")));
                                        kdmCenter.setPostal(WebTextUtil.unescapeHtml(kdmJson.getString("postal")));
                                        kdmCenter.setEmail(kdmJson.getString("email"));
                                        kdmCenter.setPhone(kdmJson.getString("phone"));
                                        kdmCenter.setPhoto(kdmJson.getString("photo"));
                                        kdmCenter.setWeb(cleanUrl(kdmJson.getString("web")));
                                        continue;
                                    }

                                    // Create a new KdmCenter record
                                    KdmCenter kdmCenter = updateStore.insertEntity(KdmCenter.class);
                                    kdmCenter.setKdmId(kdmJson.getInteger("id"));
                                    kdmCenter.setName(WebTextUtil.unescapeHtml(kdmJson.getString("name")));
                                    if (kdmJson.getDouble("lat") != null) {
                                        kdmCenter.setLat(kdmJson.getDouble("lat").floatValue());
                                    }
                                    if (kdmJson.getDouble("lng") != null) {
                                        kdmCenter.setLng(kdmJson.getDouble("lng").floatValue());
                                    }
                                    kdmCenter.setType(kdmJson.getString("type"));
                                    kdmCenter.setMothercenter(kdmJson.getBoolean("mothercenter"));
                                    kdmCenter.setAddress(WebTextUtil.unescapeHtml(kdmJson.getString("address")));
                                    kdmCenter.setAddress2(WebTextUtil.unescapeHtml(kdmJson.getString("address2")));
                                    kdmCenter.setAddress3(WebTextUtil.unescapeHtml(kdmJson.getString("address3")));
                                    kdmCenter.setCity(WebTextUtil.unescapeHtml(kdmJson.getString("city")));
                                    kdmCenter.setState(WebTextUtil.unescapeHtml(kdmJson.getString("state")));
                                    kdmCenter.setPostal(WebTextUtil.unescapeHtml(kdmJson.getString("postal")));
                                    kdmCenter.setEmail(kdmJson.getString("email"));
                                    kdmCenter.setPhone(kdmJson.getString("phone"));
                                    kdmCenter.setPhoto(kdmJson.getString("photo"));
                                    kdmCenter.setWeb(cleanUrl(kdmJson.getString("web")));
                                }

                                updateStore.submitChanges()

                                        .onFailure(Console::log)
                                        .onSuccess(result -> processClosedCentres(webKdmJsonArray, kdmCenters));
                        }));
    }

    private void processClosedCentres(ReadOnlyAstArray latestCentresList, EntityList<KdmCenter> currentCentresList) {

        List<Integer> closedCentreIds = getClosedCentreIds(latestCentresList, currentCentresList);
        if (!closedCentreIds.isEmpty()) {

            String closedCentreIdsString = closedCentreIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            EntityStore.create(dataSourceModel).<Organization>executeQuery("select id,kdmCenter.id from Organization where kdmCenter in (" + closedCentreIdsString + ")")

                    .onFailure(error -> Console.log(error))
                    .onSuccess(organizations -> {

                        UpdateStore updateStore = UpdateStore.create(dataSourceModel);
                        for(Organization organization : organizations) {
                            organization = updateStore.updateEntity(organization);
                            organization.setClosed(true);
                        }
                        updateStore.submitChanges().onFailure(Console::log);
                    });
        }
    }

    private List<Integer> getClosedCentreIds(ReadOnlyAstArray latestCentresList, EntityList<KdmCenter> currentCentresList) {

        List<Integer> closedCentreIds = new ArrayList<>();
        for (KdmCenter currentCentre: currentCentresList) {

            boolean isClosed = true;
            for (int i = 0; i < latestCentresList.size(); i++) {

                ReadOnlyAstObject latestCentre = latestCentresList.getObject(i);
                if (currentCentre.getKdmId().equals(latestCentre.getInteger("id"))) {
                    isClosed = false;
                    break;
                }
            }

            if (isClosed) {
                Console.log("Found a closed centre with ID: " + currentCentre.getKdmId());
                closedCentreIds.add(currentCentre.getKdmId());
            }
        }
        return closedCentreIds;
    }

    private static String cleanUrl(String url) {
        return url == null ? null : url.replace("\\", "");
    }
}
