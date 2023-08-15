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
import one.modality.base.shared.entities.Country;
import one.modality.base.shared.entities.KdmCenter;
import one.modality.base.shared.entities.Organization;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.*;

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
                        .onSuccess(webKdmJsonArray -> EntityStore.create(dataSourceModel).<KdmCenter>executeQuery("select id,kdmId,name,type,lat,lng from KdmCenter")

                                .onFailure(error -> Console.log(error))
                                .onSuccess(kdmCenters -> {

                                        Set<Integer> kdmIds = kdmCenters.stream().map(KdmCenter::getKdmId).collect(Collectors.toSet());
                                        UpdateStore updateStore = UpdateStore.create(dataSourceModel);

                                        // @TODO - restore this
                                        for (int i = 0; i < 1; i++) {
                                        // for (int i = 0; i < webKdmJsonArray.size(); i++) {

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

                                        updateStore.submitChanges()
                                                .onSuccess(result -> synchroniseOrganisations(kdmCenters))
                                                .onFailure(Console::log);
                                })));
    }

    protected Integer getTypeIdFromKdmType(String type) {
        switch (type) {
            case "KMC":
                return 2;
            case "KBC":
                return 3;
            case "BRANCH":
                return 4;
            case "IRC":
                return 5;
            default:
                // CORP
                return 1;
        }
    }

    /*
     * Uses the Haversine formula to determine the distance between two points on the planet.
     */
    private static double calculateDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        double radius = 6371; //radius of the earth in kilometers

        double lat1Radians = Math.toRadians(latitude1);
        double lat2Radians = Math.toRadians(latitude2);
        double lon1Radians = Math.toRadians(longitude1);
        double lon2Radians = Math.toRadians(longitude2);

        double dLat = lat2Radians - lat1Radians;
        double dLon = lon2Radians - lon1Radians;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(lat1Radians) * Math.cos(lat2Radians) * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return radius * c;
    }

    protected void synchroniseOrganisations(dev.webfx.stack.orm.entity.EntityList<KdmCenter> kdmCenters) {

        UpdateStore updateStore = UpdateStore.create(dataSourceModel);
        EntityStore.create(dataSourceModel).<Country>executeQuery("select id,iso_alpha2,latitude,longitude from Country")

                .onFailure(error -> Console.log(error))
                .onSuccess(countries -> {

                    EntityStore.create(dataSourceModel).<Organization>executeQuery("select id,name,kdmCenter.id from Organization")

                            .onFailure(error -> Console.log(error))
                            .onSuccess(organizations -> {

                                for (KdmCenter kdmCenter : kdmCenters) {

                                    // Ignore all branches (these are sites rather than Organizations and so should be stored separately)
                                    if (kdmCenter.getType().equals("BRANCH")) {
                                        Console.log("KdmCenter is a branch. Ignoring...");
                                        continue;
                                    }

                                    // If the current kdmCenter is linked to an Organization, then update the Organization record
                                    String id = kdmCenter.getPrimaryKey().toString();
                                    Boolean isLinkedToOrg = false;
                                    for (Organization currentOrg : organizations) {

                                        if ((currentOrg.getKdmCenterId() != null) && (currentOrg.getKdmCenterId().getPrimaryKey().toString().equals(id))) {
                                            // @TODO - uncomment this
                                            //currentOrg = updateStore.updateEntity(currentOrg);
                                            //currentOrg.setName(kdmCenter.getName());
                                            isLinkedToOrg = true;
                                            break;
                                        }
                                    }

                                    if (isLinkedToOrg) {
                                        continue;
                                    }

                                    // @TODO - implement this
                                    Console.log("Create a new Organization record for this KdmCenter ID: " + id);
                                    Console.log("The type is: " + kdmCenter.getType());
                                    Console.log("The type ID is: " + getTypeIdFromKdmType(kdmCenter.getType()));

                                    // Use the Haversine formula to determine in which country the KDM centre is located.
                                    Country closestCountry = null;
                                    for (Country currentCountry : countries) {

                                        if (closestCountry == null) {
                                            closestCountry = currentCountry;
                                            continue;
                                        }

                                        // closestCountry.getLatitude() = 42.5;
                                        // closestCountry.getLongitude() = 1.5;
                                        // closestCountry.getIsoAlpha2() = "ad";

                                        // currentCountry.getLatitude() = 24;
                                        // currentCountry.getLongitude() = 54;
                                        // currentCountry.getIsoAlpha2() = "ae";

                                        // kdmLat = 33.66986;
                                        // kdmLng = -84.42035;
                                        // kdmCountry = ?

                                        Float lat1 = closestCountry.getLatitude();
                                        Float lon1 = closestCountry.getLongitude();

                                        Float lat2 = currentCountry.getLatitude();
                                        Float lon2 = currentCountry.getLongitude();

                                        Float lat3 = kdmCenter.getLat();
                                        Float lon3 = kdmCenter.getLng();

                                        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null || lat3 == null || lon3 == null) {
                                            continue;
                                        }

                                        double distance1 = calculateDistance(lat1, lon1, lat3, lon3);
                                        double distance2 = calculateDistance(lat2, lon2, lat3, lon3);

                                        // Console.log("DISTANCE 1----: " + distance1);
                                        // Console.log("DISTANCE 2----: " + distance2);

                                        if (distance1 < distance2) {
                                            continue;
                                        }
                                        closestCountry = currentCountry;
                                    }

                                    Console.log("The selected country is=====: " + closestCountry.getIsoAlpha2());

                                    //Organization newOrg = updateStore.insertEntity(Organization.class);
                                    //newOrg.setName(kdmCenter.getName());
                                    //newOrg.setTypeId(getTypeIdFromKdmType(kdmCenter.getType()));
                                    //newOrg.setKdmCenterId(id);
                                    // newOrg.setCountryId(closestCountry.getId());

                                }

                                if (!updateStore.hasChanges()) {
                                    Console.log("No Organizations to update");
                                } else {
                                    Console.log("Updating Organizations... ");
                                    // @TODO - uncomment this
                                    // updateStore.submitChanges().onFailure(Console::log);
                                }
                            });
                });
    }
}
