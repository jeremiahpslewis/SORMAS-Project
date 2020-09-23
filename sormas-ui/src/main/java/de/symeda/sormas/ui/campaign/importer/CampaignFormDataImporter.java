package de.symeda.sormas.ui.campaign.importer;

import com.opencsv.exceptions.CsvValidationException;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.UI;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaReferenceDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.importexport.InvalidColumnException;
import de.symeda.sormas.api.region.CommunityReferenceDto;
import de.symeda.sormas.api.region.DistrictReferenceDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.ui.importer.DataImporter;
import de.symeda.sormas.ui.importer.ImportErrorException;
import de.symeda.sormas.ui.importer.ImportLineResult;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CampaignFormDataImporter extends DataImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignFormDataImporter.class);

    private UI currentUI;
    private String campaignFormMetaUUID;

    public CampaignFormDataImporter(File inputFile, boolean hasEntityClassRow, UserReferenceDto currentUser, String campaignUUID) {
        super(inputFile, hasEntityClassRow, currentUser);
        this.campaignFormMetaUUID = campaignUUID;
    }

    @Override
    public void startImport(Consumer<StreamResource> addErrorReportToLayoutCallback, UI currentUI, boolean duplicatesPossible) throws IOException, CsvValidationException {

        this.currentUI = currentUI;
        super.startImport(addErrorReportToLayoutCallback, currentUI, duplicatesPossible);
    }

    @Override
    protected ImportLineResult importDataFromCsvLine(String[] values, String[] entityClasses, String[] entityProperties, String[][] entityPropertyPaths, boolean firstLine) throws IOException, InvalidColumnException, InterruptedException {
        if (values.length > entityProperties.length) {
            writeImportError(values, I18nProperties.getValidationError(Validations.importLineTooLong));
            return ImportLineResult.ERROR;
        }
        CampaignFormDataDto campaignFormData = CampaignFormDataDto.build();


        try {
            campaignFormData = insertColumnEntryIntoData(campaignFormData, values, entityProperties);
            CampaignFormMetaDto campaginMetaDto = FacadeProvider.getCampaignFormMetaFacade().getCampaignFormMetaByUuid(campaignFormMetaUUID);
            campaignFormData.setCampaignFormMeta(new CampaignFormMetaReferenceDto(campaignFormMetaUUID, campaginMetaDto.getFormName()));
            FacadeProvider.getCampaignFormDataFacade().saveCampaignFormData(campaignFormData);
            return ImportLineResult.SUCCESS;
        } catch (ImportErrorException e) {
            e.printStackTrace();
        }

        return null;
    }

    private CampaignFormDataDto insertColumnEntryIntoData(CampaignFormDataDto campaignFormData, String[] entry, String[] entryHeaderPath) throws InvalidColumnException, ImportErrorException {
        CampaignFormDataDto currentElement = campaignFormData;
        for (int i = 0; i < entry.length; i++) {
            if (Objects.isNull(currentElement.getCommunity())) {
                try {
                    PropertyDescriptor propertyDescriptor = null;
                    propertyDescriptor = new PropertyDescriptor(entryHeaderPath[i], currentElement.getClass());
                    Class<?> propertyType = propertyDescriptor.getPropertyType();
                    if (!executeDefaultInvokings(propertyDescriptor, currentElement, entry[i], entryHeaderPath)) {
                        if (propertyType.isAssignableFrom(CampaignReferenceDto.class)) {
                            CampaignDto campaign = FacadeProvider.getCampaignFacade().getByUuid(entry[i]);
                            if (Objects.nonNull(campaign)) {
                                propertyDescriptor.getWriteMethod().invoke(currentElement, new CampaignReferenceDto(campaign.getUuid(), campaign.getName()));
                            }
                        } else if (propertyType.isAssignableFrom(DistrictReferenceDto.class)) {
                            List<DistrictReferenceDto> district = FacadeProvider.getDistrictFacade().getByName(entry[i], currentElement.getRegion(), true);
                            if (district.isEmpty()) {
                                throw new ImportErrorException(
                                        I18nProperties
                                                .getValidationError(Validations.importEntryDoesNotExistDbOrRegion, entry, buildEntityProperty(entryHeaderPath)));
                            } else if (district.size() > 1) {
                                throw new ImportErrorException(
                                        I18nProperties.getValidationError(Validations.importDistrictNotUnique, entry, buildEntityProperty(entryHeaderPath)));
                            } else {
                                propertyDescriptor.getWriteMethod().invoke(currentElement, district.get(0));
                            }
                        } else if (propertyType.isAssignableFrom(CommunityReferenceDto.class)) {
                            List<CommunityReferenceDto> community = FacadeProvider.getCommunityFacade().getByName(entry[i], currentElement.getDistrict(), true);
                            if (community.isEmpty()) {
                                throw new ImportErrorException(
                                        I18nProperties.getValidationError(
                                                Validations.importEntryDoesNotExistDbOrDistrict,
                                                entry,
                                                buildEntityProperty(entryHeaderPath)));
                            } else if (community.size() > 1) {
                                throw new ImportErrorException(
                                        I18nProperties.getValidationError(Validations.importCommunityNotUnique, entry, buildEntityProperty(entryHeaderPath)));
                            } else {
                                propertyDescriptor.getWriteMethod().invoke(currentElement, community.get(0));
                            }
                        }
                    }
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IntrospectionException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                CampaignFormDataEntry formEntry = new CampaignFormDataEntry(entryHeaderPath[i], entry[i]);
                if (Objects.nonNull(currentElement.getFormValues())) {
                    List<CampaignFormDataEntry> currentElementFormValues = currentElement.getFormValues();
                    currentElementFormValues.add(formEntry);
                    currentElement.setFormValues(currentElementFormValues);
                } else {
                    List formValues = new LinkedList();
                    formValues.add(formEntry);
                    currentElement.setFormValues(formValues);
                }
            }
        }
        return currentElement;
    }
}