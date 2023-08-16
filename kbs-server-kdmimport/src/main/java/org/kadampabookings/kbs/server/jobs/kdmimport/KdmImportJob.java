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

    protected void synchroniseOrganisations(dev.webfx.stack.orm.entity.EntityList<KdmCenter> kdmCenters) {

        UpdateStore updateStore = UpdateStore.create(dataSourceModel);
        EntityStore.create(dataSourceModel).<Country>executeQuery("select id,iso_alpha2,latitude,longitude,north,south,east,west from Country")

                .onFailure(error -> Console.log(error))
                .onSuccess(countries -> {

                    EntityStore.create(dataSourceModel).<Organization>executeQuery("select id,name,kdmCenter.id from Organization")

                            .onFailure(error -> Console.log(error))
                            .onSuccess(organizations -> {

                                for (KdmCenter kdmCenter : kdmCenters) {

                                    // @TODO remove this
                                    /*
                                    if (!kdmCenter.getPrimaryKey().toString().equals("2629")) {
                                        continue;
                                    }
                                    Console.log("PROCESSING THE CHILE CENTER........");
                                    */


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

                                    Console.log("Create a new Organization record for this KdmCenter ID: " + id);
                                    Console.log("The name is: " + kdmCenter.getName());
                                    Console.log("The type is: " + kdmCenter.getType());
                                    Console.log("The type ID is: " + getTypeIdFromKdmType(kdmCenter.getType()));

                                    Country enclosingCountry = null;
                                    int noOfMatches = 0;
                                    double smallestRectangleSurface = 0.0;
                                    double sizeOfCurrentRectangleSurface = 0.0;

                                    if (kdmCenter.getLat() != null && kdmCenter.getLng() != null) {

                                        for (Country currentCountry : countries) {
                                            boolean isEnclosed = isPointInRectangle(
                                                    kdmCenter.getLat(),
                                                    kdmCenter.getLng(),
                                                    currentCountry.getNorth(),
                                                    currentCountry.getSouth(),
                                                    currentCountry.getEast(),
                                                    currentCountry.getWest());

                                            if (isEnclosed) {

                                                Console.log("ENCLOSURE FOUND=======");

                                                noOfMatches++;
                                                sizeOfCurrentRectangleSurface = getSurfaceOfRectangle(
                                                        currentCountry.getNorth(),
                                                        currentCountry.getSouth(),
                                                        currentCountry.getEast(),
                                                        currentCountry.getWest());

                                                Console.log("SIZE OF ENCLOSURE =======" + sizeOfCurrentRectangleSurface);
                                                Console.log("NAME OF ENCLOSURE =======" + currentCountry.getIsoAlpha2());

                                                if (smallestRectangleSurface == 0.0) {
                                                    smallestRectangleSurface = sizeOfCurrentRectangleSurface;
                                                    enclosingCountry = currentCountry;
                                                } else if (sizeOfCurrentRectangleSurface < smallestRectangleSurface) {
                                                    smallestRectangleSurface = sizeOfCurrentRectangleSurface;
                                                    enclosingCountry = currentCountry;
                                                }
                                            }
                                        }
                                    }

                                    if (noOfMatches > 1) {
                                        Console.log("The selected KdmCenter has multiple country matches ===========");
                                        //continue;
                                    }

                                    Console.log("The selected country is=====: " + (enclosingCountry == null ? "null" : enclosingCountry.getIsoAlpha2()));

                                    // @TODO - uncomment this
                                    //Organization newOrg = updateStore.insertEntity(Organization.class);
                                    //newOrg.setName(kdmCenter.getName());
                                    //newOrg.setTypeId(getTypeIdFromKdmType(kdmCenter.getType()));
                                    //newOrg.setKdmCenter(kdmCenter);
                                    //newOrg.setCountry(enclosingCountry);
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

    private boolean isPointInRectangle(double latitude, double longitude, double north, double south, double east, double west) {
        if (south <= latitude && latitude <= north) {
            if (west <= east) {
                if (west <= longitude && longitude <= east) {
                    return true;
                }
            } else {
                if ((west <= longitude && longitude <= 180) || (-180 <= longitude && longitude <= east)){
                    return true;
                }
            }
        }
        return false;
    }

    private double getSurfaceOfRectangle(double north, double south, double east, double west) {
        double radius = 6378137.0;
        double height = radius * 2 * Math.asin(
                Math.sqrt(
                        Math.sin((north - south) * Math.PI / 180.0) * Math.sin((north - south) * Math.PI / 180.0)
                                + Math.cos(north * Math.PI / 180.0) * Math.cos(south * Math.PI / 180.0)
                                * Math.sin((east - west) * Math.PI / 180.0) * Math.sin((east - west) * Math.PI / 180.0)));
        double width = radius * 2 * Math.asin(
                Math.sqrt(
                        Math.sin((east - west) * Math.PI / 180.0) * Math.sin((east - west) * Math.PI / 180.0)
                                + Math.cos(east * Math.PI / 180.0) * Math.cos(west * Math.PI / 180.0)
                                * Math.sin((south - north) * Math.PI / 180.0) * Math.sin((south - north) * Math.PI / 180.0)));
        return height * width;
    }

    private Integer getTypeIdFromKdmType(String type) {
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
}
