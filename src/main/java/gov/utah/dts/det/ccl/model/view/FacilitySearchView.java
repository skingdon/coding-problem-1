package gov.utah.dts.det.ccl.model.view;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.annotations.Type;

import gov.utah.dts.det.admin.model.PickListValue;
import gov.utah.dts.det.ccl.model.Facility;
import gov.utah.dts.det.ccl.model.License;
import gov.utah.dts.det.ccl.model.Person;
import gov.utah.dts.det.ccl.model.Phone;
import gov.utah.dts.det.ccl.model.Region;
import gov.utah.dts.det.ccl.model.enums.FacilityType;
import gov.utah.dts.det.ccl.view.Address;
import gov.utah.dts.det.ccl.view.MailingLabel;
import gov.utah.dts.det.model.AbstractBaseEntity;

@SuppressWarnings("serial")
@Entity
@Table(name = "FACILITY_SEARCH_VIEW")
@Immutable
public class FacilitySearchView extends AbstractBaseEntity<Long> implements Serializable, MailingLabel {

	@Id
	@Column(name = "ID", unique = true, nullable = false, insertable = false, updatable = false)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID", insertable = false, updatable = false)
	private Facility facility;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "REGION_ID", insertable = false, updatable = false)
	private Region region;
	
	@Column(name = "LICENSING_SPECIALIST_ID")
	private Long licensingSpecialistId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "LICENSING_SPECIALIST_ID", insertable = false, updatable = false)
	private Person licensingSpecialist;
	
	@Column(name = "FACILITY_NAME")
	private String facilityName;

	@Column(name = "FACILITY_NAME_UPPER")
	private String facilityNameUpper;			// Only used by sort order to make case insensitive sort

	@Column(name = "SITE_NAME")
	private String siteName;

    @Column(name = "SITE_NAME_UPPER")
    private String siteNameUpper;           // Only used by sort order to make case insensitive sort
	
	@Column(name = "STATUS")
	private Character status;
	
	@Column(name = "INITIAL_REGULATION_DATE")
	@Temporal(TemporalType.DATE)
	private Date initialRegulationDate;
	
	@Column(name = "FACILITY_TYPE")
	@Type(type = "FacilityType")
	private FacilityType facilityType;
	
	@Embedded
	@AttributeOverride(name = "phoneNumber", column = @Column(name = "FACILITY_PRIMARY_PHONE"))
	private Phone primaryPhone;
	
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "addressOne", column = @Column(name = "FACILITY_LOC_ADDRESS_ONE")),
		@AttributeOverride(name = "addressTwo", column = @Column(name = "FACILITY_LOC_ADDRESS_TWO")),
		@AttributeOverride(name = "city", column = @Column(name = "FACILITY_LOC_CITY")),
		@AttributeOverride(name = "state", column = @Column(name = "FACILITY_LOC_STATE")),
		@AttributeOverride(name = "zipCode", column = @Column(name = "FACILITY_LOC_ZIP_CODE")),
		@AttributeOverride(name = "county", column = @Column(name = "FACILITY_LOC_COUNTY"))
	})
	private EmbeddableAddress locationAddress;
	
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "addressOne", column = @Column(name = "FACILITY_MAIL_ADDRESS_ONE")),
		@AttributeOverride(name = "addressTwo", column = @Column(name = "FACILITY_MAIL_ADDRESS_TWO")),
		@AttributeOverride(name = "city", column = @Column(name = "FACILITY_MAIL_CITY")),
		@AttributeOverride(name = "state", column = @Column(name = "FACILITY_MAIL_STATE")),
		@AttributeOverride(name = "zipCode", column = @Column(name = "FACILITY_MAIL_ZIP_CODE")),
		@AttributeOverride(name = "county", column = @Column(name = "FACILITY_MAIL_COUNTY"))
	})
	private EmbeddableAddress mailingAddress;
	
	@Column(name = "DIRECTOR_NAME")
	private String directorName;
	
	@Column(name = "OWNER_NAME")
	private String ownerName;
	
	@Column(name = "IS_CONDITIONAL")
	@Type(type = "yes_no")
	private Boolean conditional;
	
	@Column(name = "EXEMPTIONS")
	private String exemptions;
	
	@Column(name = "EXEMPTION_EXPIRATION_DATE")
	@Temporal(TemporalType.DATE)
	private Date exemptionExpirationDate;
	
	@Column(name = "INACTIVE_REASON_ID")
	private Long inactiveReasonId;
	
	@Column(name = "INACTIVE_REASON_NAME")
	private String inactiveReason;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "INACTIVE_REASON_ID", insertable = false, updatable = false)
	private PickListValue inactiveReasonPlv;
	
	@Column(name = "CLOSURE_DATE")
	@Temporal(TemporalType.DATE)
	private Date closureDate;
	
	@OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	@Sort(type = SortType.NATURAL)
	private SortedSet<License> licenses = new TreeSet<License>();
	
	@Column(name = "IS_OWNER")
	@Type(type = "yes_no")
	private Boolean isOwner;
	
	@Column(name = "IS_ADMINISTRATOR")
	@Type(type = "yes_no")
	private Boolean isAdministrator;
	
	public FacilitySearchView() {
		
	}

	@Override
	public Long getPk() {
		return id;
	}

	@Override
	public void setPk(Long pk) {
		this.id = pk;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public Facility getFacility() {
		return facility;
	}

	public void setFacility(Facility facility) {
		this.facility = facility;
	}

	public Region getRegion() {
		return region;
	}
	
	public void setRegion(Region region) {
		this.region = region;
	}

	public Long getLicensingSpecialistId() {
		return licensingSpecialistId;
	}

	public void setLicensingSpecialistId(Long licensingSpecialistId) {
		this.licensingSpecialistId = licensingSpecialistId;
	}
	
	public Person getLicensingSpecialist() {
		return licensingSpecialist;
	}
	
	public void setLicensingSpecialist(Person licensingSpecialist) {
		this.licensingSpecialist = licensingSpecialist;
	}
	
	public String getFacilityName() {
		return facilityName;
	}

	public void setFacilityName(String facilityName) {
		this.facilityName = facilityName;
	}

	public String getFacilityNameUpper() {
		return facilityNameUpper;
	}

	public void setFacilityNameUpper(String facilityNameUpper) {
		this.facilityNameUpper = facilityNameUpper;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public Character getStatus() {
		return status;
	}
	
	public void setStatus(Character status) {
		this.status = status;
	}

	public Date getInitialRegulationDate() {
		return initialRegulationDate;
	}

	public void setInitialRegulationDate(Date initialRegulationDate) {
		this.initialRegulationDate = initialRegulationDate;
	}
	
	public FacilityType getFacilityType() {
		return facilityType;
	}
	
	public void setFacilityType(FacilityType facilityType) {
		this.facilityType = facilityType;
	}
	
	public Phone getPrimaryPhone() {
		return primaryPhone;
	}

	public void setPrimaryPhone(Phone primaryPhone) {
		this.primaryPhone = primaryPhone;
	}

	public EmbeddableAddress getLocationAddress() {
		return locationAddress;
	}

	public void setLocationAddress(EmbeddableAddress locationAddress) {
		this.locationAddress = locationAddress;
	}
	
	public EmbeddableAddress getMailingAddress() {
		return mailingAddress;
	}
	
	public void setMailingAddress(EmbeddableAddress mailingAddress) {
		this.mailingAddress = mailingAddress;
	}

	public String getDirectorName() {
		return directorName;
	}

	public void setDirectorName(String directorName) {
		this.directorName = directorName;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public Boolean getConditional() {
		return conditional;
	}
	
	public void setConditional(Boolean conditional) {
		this.conditional = conditional;
	}
	
	public String getExemptions() {
		return exemptions;
	}
	
	public void setExemptions(String exemptions) {
		this.exemptions = exemptions;
	}
	
	public Date getExemptionExpirationDate() {
		return exemptionExpirationDate;
	}
	
	public void setExemptionExpirationDate(Date exemptionExpirationDate) {
		this.exemptionExpirationDate = exemptionExpirationDate;
	}
	
	public Long getInactiveReasonId() {
		return inactiveReasonId;
	}
	
	public void setInactiveReasonId(Long inactiveReasonId) {
		this.inactiveReasonId = inactiveReasonId;
	}
	
	public String getInactiveReason() {
		return inactiveReason;
	}
	
	public void setInactiveReason(String inactiveReason) {
		this.inactiveReason = inactiveReason;
	}
	
	public PickListValue getInactiveReasonPlv() {
		return inactiveReasonPlv;
	}
	
	public void setInactiveReasonPlv(PickListValue inactiveReasonPlv) {
		this.inactiveReasonPlv = inactiveReasonPlv;
	}
	
	public Date getClosureDate() {
		return closureDate;
	}
	
	public void setClosureDate(Date closureDate) {
		this.closureDate = closureDate;
	}
	
	@Override
	public String getName() {
		return facilityName;
	}
	
	@Override
	public Address getAddress() {
		return mailingAddress;
	}

	public SortedSet<License> getLicenses() {
		return licenses;
	}

	public void setLicenses(SortedSet<License> licenses) {
		this.licenses = licenses;
	}

	public List<License> getActiveLicenses() {
		return facility.getActiveLicenses();
	}

	public Boolean getIsOwner() {
		return isOwner;
	}

	public void setIsOwner(Boolean isOwner) {
		this.isOwner = isOwner;
	}

	public Boolean getIsAdministrator() {
		return isAdministrator;
	}

	public void setIsAdministrator(Boolean isAdministrator) {
		this.isAdministrator = isAdministrator;
	}

}