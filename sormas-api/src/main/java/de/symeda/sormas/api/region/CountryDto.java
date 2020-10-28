package de.symeda.sormas.api.region;

import de.symeda.sormas.api.EntityDto;
import de.symeda.sormas.api.utils.DataHelper;

public class CountryDto extends EntityDto {

	private static final long serialVersionUID = 8309822957203823162L;

	public static final String I18N_PREFIX = "Country";
	public static final String NAME = "name";
	public static final String EXTERNAL_ID = "externalID";
	public static final String ISO_CODE = "isoCode";
	public static final String UNO_CODE = "unoCode";

	private String name;
	private String externalID;
	private String isoCode;
	private String unoCode;
	private boolean archived;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExternalID() {
		return externalID;
	}

	public void setExternalID(String externalID) {
		this.externalID = externalID;
	}

	public String getIsoCode() {
		return isoCode;
	}

	public void setIsoCode(String isoCode) {
		this.isoCode = isoCode;
	}

	public String getUnoCode() {
		return unoCode;
	}

	public void setUnoCode(String unoCode) {
		this.unoCode = unoCode;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public static CountryDto build() {
		CountryDto dto = new CountryDto();
		dto.setUuid(DataHelper.createUuid());
		return dto;
	}
}
