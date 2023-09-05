package org.kadampabookings.kbs.server.jobs.organisationupdate;

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.UpdateStore;
import one.modality.base.shared.entities.Country;
import one.modality.base.shared.entities.KdmCenter;
import one.modality.base.shared.entities.Organization;

/**
 * @author bjvickers
 */
public class OrganisationUpdateJob implements ApplicationJob {

    private final DataSourceModel dataSourceModel = DataSourceModelService.getDefaultDataSourceModel();
    private static final long UPDATE_PERIODICITY_MILLIS = 1000 * 3600 * 24 * 3; // 3x day
    private Scheduled importTimer;
    private boolean isDebugMode = true;

    @Override
    public void onStart() {
        updateOrganisations();

        // @TODO
        // The following creates an exception - Bruno to investigate
        //importTimer = Scheduler.schedulePeriodic(UPDATE_PERIODICITY_MILLIS, this::updateOrganisations);
    }

    @Override
    public void onStop() {
        if (importTimer != null)
            importTimer.cancel();
    }

    protected void updateOrganisations() {

        UpdateStore updateStore = UpdateStore.create(dataSourceModel);
        EntityStore.create(dataSourceModel).<Country>executeQuery("select id,iso_alpha2,latitude,longitude,north,south,east,west from Country")

                .onFailure(Console::log)
                .onSuccess(countries -> {

                    EntityStore.create(dataSourceModel).<Organization>executeQuery("select id,name,importIssue,kdmCenter.id from Organization")

                            .onFailure(Console::log)
                            .onSuccess(organizations -> {

                                EntityStore.create(dataSourceModel).<KdmCenter>executeQuery("select id,kdmId,name,type,lat,lng from KdmCenter")

                                        .onFailure(Console::log)
                                        .onSuccess(kdmCenters -> {

                                            // @TODO - uncomment ==================
                                            for (KdmCenter kdmCenter : kdmCenters) {

                                                // Ignore all branches (these are sites rather than Organizations and so should be stored separately)
                                                if (kdmCenter.getType().equals("BRANCH")) {
                                                    continue;
                                                }

                                                // If the current kdmCenter is linked to an Organization, then update the Organization record
                                                String id = kdmCenter.getPrimaryKey().toString();
                                                Boolean isLinkedToOrganisation = false;
                                                for (Organization current : organizations) {

                                                    if ((current.getKdmCenterId() != null) && (current.getKdmCenterId().getPrimaryKey().toString().equals(id))) {
                                                        current = updateStore.updateEntity(current);
                                                        current.setName(kdmCenter.getName());
                                                        current.setType(getTypeIdFromKdmType(kdmCenter.getType()));
                                                        current.setLatitude(kdmCenter.getLat());
                                                        current.setLongitude(kdmCenter.getLng());
                                                        isLinkedToOrganisation = true;
                                                        break;
                                                    }
                                                }

                                                if (isLinkedToOrganisation) {
                                                    continue;
                                                }

                                                if (isDebugMode) {
                                                    Console.log("Creating a new Organization record for KdmCenter ID: " + id);
                                                    Console.log("Organization name: " + kdmCenter.getName());
                                                    Console.log("Organization type is: " + getTypeIdFromKdmType(kdmCenter.getType()));
                                                }

                                                Country enclosingCountry = null;
                                                StringBuilder multipleMatchingCountries = new StringBuilder();
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

                                                            noOfMatches++;
                                                            multipleMatchingCountries.append(currentCountry.getIsoAlpha2()).append(",");
                                                            sizeOfCurrentRectangleSurface = getSurfaceOfRectangle(
                                                                    currentCountry.getNorth(),
                                                                    currentCountry.getSouth(),
                                                                    currentCountry.getEast(),
                                                                    currentCountry.getWest());

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

                                                Organization newOrganisation = updateStore.insertEntity(Organization.class);
                                                newOrganisation.setName(kdmCenter.getName());
                                                newOrganisation.setType(getTypeIdFromKdmType(kdmCenter.getType()));
                                                newOrganisation.setKdmCenter(kdmCenter);
                                                newOrganisation.setLatitude(kdmCenter.getLat());
                                                newOrganisation.setLongitude(kdmCenter.getLng());
                                                newOrganisation.setCountry(enclosingCountry);

                                                if (noOfMatches > 1) {
                                                    newOrganisation.setImportIssue("Multiple matching countries found: " + multipleMatchingCountries);
                                                }
                                            }

                                            if (updateStore.hasChanges()) {
                                                updateStore.submitChanges().onFailure(Console::log);
                                            }
                                        });
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
