package org.kadampabookings.kbs.server.jobs.kdmimport;

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.Fetch;
import dev.webfx.platform.json.ReadOnlyJsonObject;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.UpdateStore;
import one.modality.base.shared.entities.KdmCenter;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author bjvickers
 */
public class KdmImportJob implements ApplicationJob {

    private static final String KDM_FETCH_URL = "https://kdm.kadampaweb.org/index.php/business/json";
    private final DataSourceModel dataSourceModel = DataSourceModelService.getDefaultDataSourceModel();
    private static final long IMPORT_PERIODICITY_MILLIS = 1000 * 3600 * 24 * 28; // 4x weeks
    private Scheduled importTimer;

    @Override
    public void onStart() {
        importKdm();
        // @TODO
        // The following creates an exception - Bruno to investigate
        //importTimer = Scheduler.schedulePeriodic(IMPORT_PERIODICITY_MILLIS, this::importKdm);
    }

    @Override
    public void onStop() {
        if (importTimer != null)
            importTimer.cancel();
    }

    public void importKdm() {

        Fetch.fetch(KDM_FETCH_URL)

                .onFailure(error -> Console.log("Error while fetching " + KDM_FETCH_URL, error))
                .onSuccess(response -> response.jsonArray()

                        .onFailure(error -> Console.log("Error while parsing json array from " + KDM_FETCH_URL, error))
                        .onSuccess(webKdmJsonArray -> EntityStore.create(dataSourceModel).<KdmCenter>executeQuery("select id,kdmId from KdmCenter")

                                .onFailure(error -> Console.log(error))
                                .onSuccess(kdmCenters -> {

                                        Set<Integer> kdmIds = kdmCenters.stream().map(KdmCenter::getKdmId).collect(Collectors.toSet());
                                        UpdateStore updateStore = UpdateStore.create(dataSourceModel);

                                        for (int i = 0; i < webKdmJsonArray.size(); i++) {

                                            ReadOnlyJsonObject kdmJson = webKdmJsonArray.getObject(i);
                                            int kdmId = kdmJson.getInteger("id");
                                            if (kdmIds.contains(kdmId)) {

                                                KdmCenter kdmCenter = kdmCenters.stream().filter(center -> center.getKdmId() == kdmId).findFirst().get();
                                                kdmCenter = updateStore.updateEntity(kdmCenter);

                                                kdmCenter.setKdmId(kdmJson.getInteger("id"));
                                                kdmCenter.setName(kdmJson.getString("name"));
                                                if (kdmJson.getDouble("lat") != null) {
                                                    kdmCenter.setLat(kdmJson.getDouble("lat").floatValue());
                                                }
                                                if (kdmJson.getDouble("lng") != null) {
                                                    kdmCenter.setLng(kdmJson.getDouble("lng").floatValue());
                                                }
                                                kdmCenter.setType(kdmJson.getString("type"));
                                                kdmCenter.setMothercenter(kdmJson.getBoolean("mothercenter"));
                                                kdmCenter.setAddress(kdmJson.getString("address"));
                                                kdmCenter.setAddress2(kdmJson.getString("address2"));
                                                kdmCenter.setAddress3(kdmJson.getString("address3"));
                                                kdmCenter.setCity(kdmJson.getString("city"));
                                                kdmCenter.setState(kdmJson.getString("state"));
                                                kdmCenter.setPostal(kdmJson.getString("postal"));
                                                kdmCenter.setEmail(kdmJson.getString("email"));
                                                kdmCenter.setPhone(kdmJson.getString("phone"));
                                                kdmCenter.setPhoto(kdmJson.getString("photo"));
                                                kdmCenter.setWeb(kdmJson.getString("web"));
                                                continue;
                                            }

                                            KdmCenter kdmCenter = updateStore.insertEntity(KdmCenter.class);
                                            kdmCenter.setKdmId(kdmJson.getInteger("id"));
                                            kdmCenter.setName(kdmJson.getString("name"));
                                            if (kdmJson.getDouble("lat") != null) {
                                                kdmCenter.setLat(kdmJson.getDouble("lat").floatValue());
                                            }
                                            if (kdmJson.getDouble("lng") != null) {
                                                kdmCenter.setLng(kdmJson.getDouble("lng").floatValue());
                                            }
                                            kdmCenter.setType(kdmJson.getString("type"));
                                            kdmCenter.setMothercenter(kdmJson.getBoolean("mothercenter"));
                                            kdmCenter.setAddress(kdmJson.getString("address"));
                                            kdmCenter.setAddress2(kdmJson.getString("address2"));
                                            kdmCenter.setAddress3(kdmJson.getString("address3"));
                                            kdmCenter.setCity(kdmJson.getString("city"));
                                            kdmCenter.setState(kdmJson.getString("state"));
                                            kdmCenter.setPostal(kdmJson.getString("postal"));
                                            kdmCenter.setEmail(kdmJson.getString("email"));
                                            kdmCenter.setPhone(kdmJson.getString("phone"));
                                            kdmCenter.setPhoto(kdmJson.getString("photo"));
                                            kdmCenter.setWeb(kdmJson.getString("web"));
                                        }
                                        updateStore.submitChanges().onFailure(Console::log);
                                })));
    }
}
