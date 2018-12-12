package gov.utah.dts.det.ccl.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import gov.utah.dts.det.admin.model.PickListValue;
import gov.utah.dts.det.ccl.model.enums.FacilityType;
import gov.utah.dts.det.ccl.model.view.FacilitySearchView;
import gov.utah.dts.det.ccl.util.FacilityUtil;
import gov.utah.dts.det.ccl.view.facility.FacilityStatus;
import gov.utah.dts.det.model.AbstractAuditableEntity;
import gov.utah.dts.det.service.ApplicationService;
import gov.utah.dts.det.util.CompareUtils;

@SuppressWarnings("serial")
@Entity
@Table(name = "FACILITY")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Facility extends AbstractAuditableEntity<Long> implements Serializable, ChangeLoggedEntity, Comparable<Facility> {

	@Transient
	@Autowired
	private ApplicationService applicationService;
	
	private static final String[] LOGGED_PROPERTIES = { "name", "type", "locationAddress", "mailingAddress", "primaryPhone",
		"alternatePhone", "faxPhone", "ownershiptype", "ownerName" };
	private static final String LICENSE_STATUS_ACTIVE = "Active";
	private static final String LICENSE_STATUS_IN_PROCESS = "In Process";
	private static final String LICENSE_STATUS_CLOSED = "Closed";

	@Id
	@Column(name = "ID", unique = true, nullable = false, updatable = false)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "FACILITY_SEQ")
	@SequenceGenerator(name = "FACILITY_SEQ", sequenceName = "FACILITY_SEQ")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID", insertable = false, updatable = false)
	private FacilitySearchView searchView;

	@Column(name = "FACILITYIDNO")
	private String idNumber;

	@Column(name = "FACILITYNAME")
	private String name;

	@Column(name = "SITE_NAME")
	private String siteName;

	@Column(name = "TYPE")
	@Type(type = "FacilityType")
	private FacilityType type;

	@Column(name = "STATUS")
	@Enumerated(EnumType.STRING)
	private FacilityStatus status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "REGION_ID", nullable = false)
	private Region region;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "LOCATIONADDRESSID")
	private Address locationAddress;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "MAILINGADDRESSID")
	private Address mailingAddress;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "CBS_ADDRESS_ID")
	private Address cbsAddress;
	
	@Column(name = "CBS_EMAIL")
	private String cbsEmail;

	@Embedded
	@AttributeOverride(name = "phoneNumber", column = @Column(name = "PRIMARYPHONE"))
	private Phone primaryPhone;

	@Embedded
	@AttributeOverride(name = "phoneNumber", column = @Column(name = "ALTERNATEPHONE"))
	private Phone alternatePhone;

	@Embedded
	@AttributeOverride(name = "phoneNumber", column = @Column(name = "FAX"))
	private Phone fax;

	@Column(name = "WEBSITEURL")
	private String websiteUrl;

	@Column(name = "EMAIL")
	private String email;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "OWNERSHIPTYPEID")
	private PickListValue ownershipType;

	@Column(name = "OWNERNAME")
	private String ownerName;

	@Column(name = "INDOOR_SQUARE_FOOTAGE")
	private Integer indoorSquareFootage;

	@Column(name = "OUTDOOR_SQUARE_FOOTAGE")
	private Integer outdoorSquareFootage;

	@ManyToOne
	@JoinTable(name = "FACILITY_LICENSING_SPECIALIST", joinColumns = { @JoinColumn(name = "FACILITY_ID") }, inverseJoinColumns = { @JoinColumn(name = "SPECIALIST_ID") })
	private Person licensingSpecialist;

	@Column(name = "INITIALREGULATIONDATE")
	private Date initialRegulationDate;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
	private FacilityNotes facilityNotes;

	@Column(name = "TEXT")
	private String text;

	@OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@OrderBy("licenseType ASC, licenseNumber DESC, startDate DESC, status ASC")
	private Set<License> licenses = new HashSet<License>();

	@OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<Rating> ratings = new HashSet<Rating>();

	@OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<Accreditation> accreditations = new HashSet<Accreditation>();

	@OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private Set<Exemption> exemptions = new HashSet<Exemption>();

	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<FacilityAssociation> associatedFacilities = new HashSet<FacilityAssociation>();

	@OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<FacilityPerson> people = new HashSet<FacilityPerson>();

	@OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private Set<FacilityTag> tags = new HashSet<FacilityTag>();

	@OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@OrderBy("date DESC")
	private Set<CmpTransaction> fees = new HashSet<CmpTransaction>();

	@Column(name = "RULES_CERT_RCVD")
	@Temporal(TemporalType.DATE)
	private Date rulesCertReceived;

	@Column(name = "CODE_OF_CONDUCT_RCVD")
	@Temporal(TemporalType.DATE)
	private Date codeOfConductReceived;

	@Column(name = "CONFIDENTIAL_FORM_RCVD")
	@Temporal(TemporalType.DATE)
	private Date confidentialFormReceived;

	@Column(name = "EMERGENCY_PLAN_RCVD")
	@Temporal(TemporalType.DATE)
	private Date emergencyPlanReceived;

	@Column(name = "REFERENCE_1_RCVD")
	@Temporal(TemporalType.DATE)
	private Date referenceOneReceived;

	@Column(name = "REFERENCE_2_RCVD")
	@Temporal(TemporalType.DATE)
	private Date referenceTwoReceived;

	@Column(name = "REFERENCE_3_RCVD")
	@Temporal(TemporalType.DATE)
	private Date referenceThreeReceived;
	
	@Column(name = "SAFE_PROVIDER_ID")
	private Long safeProviderId;
	
	@Column(name = "NO_PUBLIC_LISTING")
	@Type(type = "yes_no")
	private Boolean noPublicListing = false;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CBS_TECH_ID")
	private Person cbsTechnician;

	@Column(name = "ACTIVE")
	private String active = "Y";
	
	@Column(name = "CLOSED_DATE")
	@Temporal(TemporalType.DATE)
	private Date closedDate;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CLOSED_REASON_ID")
	private PickListValue closedReason;
	
	@Transient
	private Facility parent;

	@Transient
	private Facility owner;

	@Column(name = "NON_LICENSED")
	private Boolean nonLicensed;

	@Column(name = "DHS_CONTRACT_NUMBERS")
	private String dhsContractNumbers;
	
	@Transient
	private String displayName = "";
	
	public Facility() {

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

	public FacilitySearchView getSearchView() {
		return searchView;
	}

	public void setSearchView(FacilitySearchView searchView) {
		this.searchView = searchView;
	}

	public String getIdNumber() {
		return idNumber;
	}

	public void setIdNumber(String idNumber) {
		this.idNumber = idNumber;
	}

	public String getDisplayName() {
		return name;
	}

	public String getDisplayNameProgramAndSite() {
		
		final String programName = StringUtils.trimToEmpty(name);
		final StringBuilder b = new StringBuilder(StringUtils.isEmpty(programName) ? "BLANK" : programName);
		
		if (siteName != null && siteName.length() > 0) {
			b.append(" - ").append(StringUtils.trim(siteName));
		}
		
		return b.toString();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public FacilityType getType() {
		return type;
	}

	public void setType(FacilityType type) {
		this.type = type;
	}

	public FacilityStatus getStatus() {
		return status;
	}

	public void setStatus(FacilityStatus status) {
		this.status = status;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public Address getLocationAddress() {
		return locationAddress;
	}

	public void setLocationAddress(Address locationAddress) {
		this.locationAddress = locationAddress;
	}

	public Address getMailingAddress() {
		return mailingAddress;
	}

	public void setMailingAddress(Address mailingAddress) {
		this.mailingAddress = mailingAddress;
	}

	public Address getCbsAddress() {
		return cbsAddress;
	}

	public void setCbsAddress(Address cbsAddress) {
		this.cbsAddress = cbsAddress;
	}

	public String getCbsEmail() {
		return cbsEmail;
	}

	public void setCbsEmail(String cbsEmail) {
		this.cbsEmail = cbsEmail;
	}

	public Phone getPrimaryPhone() {
		return primaryPhone;
	}

	public void setPrimaryPhone(Phone primaryPhone) {
		this.primaryPhone = primaryPhone;
	}

	public Phone getAlternatePhone() {
		return alternatePhone;
	}

	public void setAlternatePhone(Phone alternatePhone) {
		this.alternatePhone = alternatePhone;
	}

	public Phone getFax() {
		return fax;
	}

	public void setFax(Phone fax) {
		this.fax = fax;
	}

	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public void setWebsiteUrl(String websiteUrl) {
		this.websiteUrl = websiteUrl;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public PickListValue getOwnershipType() {
		return ownershipType;
	}

	public void setOwnershipType(PickListValue ownershipType) {
		this.ownershipType = ownershipType;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public Integer getIndoorSquareFootage() {
		return indoorSquareFootage;
	}

	public void setIndoorSquareFootage(Integer indoorSquareFootage) {
		this.indoorSquareFootage = indoorSquareFootage;
	}

	public Integer getOutdoorSquareFootage() {
		return outdoorSquareFootage;
	}

	public void setOutdoorSquareFootage(Integer outdoorSquareFootage) {
		this.outdoorSquareFootage = outdoorSquareFootage;
	}

	public Person getLicensingSpecialist() {
		return licensingSpecialist;
	}

	public void setLicensingSpecialist(Person licensingSpecialist) {
		this.licensingSpecialist = licensingSpecialist;
	}

	public Date getInitialRegulationDate() {
		return initialRegulationDate;
	}

	public void setInitialRegulationDate(Date initialRegulationDate) {
		this.initialRegulationDate = initialRegulationDate;
	}

	protected FacilityNotes getFacilityNotes() {
		return facilityNotes;
	}

	protected void setFacilityNotes(FacilityNotes facilityNotes) {
		this.facilityNotes = facilityNotes;
	}

	public JSONObject getNotes() {
		return nullSafeGetJSONObject();
	}

	private JSONObject nullSafeGetJSONObject() {
		try {
			JSONObject obj = getFacilityNotes().getNotes();
			if (obj == null) {
				obj = new JSONObject();
				obj.put("notes", new JSONArray());
				getFacilityNotes().setNotes(obj);
			}
			return obj;
		} catch (JSONException je) {
			throw new RuntimeException(je);
		}
	}

	public void saveNote(Integer id, String note) {
		try {
			JSONArray notes = nullSafeGetJSONObject().getJSONArray("notes");
			if (id != null) {
				notes.put(id, note);
			} else {
				notes.put(note);
			}
		} catch (JSONException je) {
			throw new RuntimeException(je);
		}
	}

	public void deleteNote(int id) {
		try {
			JSONArray notes = nullSafeGetJSONObject().getJSONArray("notes");
			// TODO: When the new version of the json library at json.org is released uncomment the new way to remove an item
			// notes.remove(id);
			JSONArray notesUpdated = new JSONArray();
			for (int i = 0; i < notes.length(); i++) {
				if (i != id) {
					notesUpdated.put(notes.get(i));
				}
			}
			nullSafeGetJSONObject().put("notes", notesUpdated);
		} catch (JSONException je) {
			throw new RuntimeException(je);
		}
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Set<License> getLicenses() {
		return licenses;
	}

	public void setLicenses(Set<License> licenses) {
		this.licenses = licenses;
	}

	public void addLicense(License license) {
		if (license != null) {
			license.setFacility(this);
			licenses.add(license);
		}
	}

	public void removeLicense(License license) {
		if (license != null) {
			licenses.remove(license);
		}
	}

	public License getLatestLicense() {
		License license = null;
		Date now = new Date();
		for (License l : licenses) {
			if ((license == null && l.getStartDate().compareTo(now) <= 0)
					|| (license != null && l.getStartDate().compareTo(license.getStartDate()) > 0 && l.getStartDate().compareTo(now) <= 0)) {
				license = l;
			}
		}
		return license;
	}

	public License getLicense(Date date, boolean closestLicense) {
		Date d = date == null ? new Date() : date;
		License lic = null;
		for (License l : getLicenses()) {
			if (gov.utah.dts.det.util.DateUtils.isInDateRange(d, l.getStartDate(), l.getEndDate())) {
				return l;
			} else if (closestLicense && (lic == null || l.getStartDate().before(d))) {
				lic = l;
			}
		}
		return lic;
	}

	public List<License> getActiveLicenses() {
		final List<License> activeLicenses = new ArrayList<License>();
		for (License license : getLicenses()) {
			final PickListValue licenseStatus = license.getStatus();
			final boolean isActive = licenseStatus != null && licenseStatus.getValue().equalsIgnoreCase(LICENSE_STATUS_ACTIVE);
			if (isActive) {
				activeLicenses.add(license);
			}
		}
		return activeLicenses;
	}

	public List<License> getInProcessLicenses() {
		final List<License> list = new ArrayList<License>();
		for (License l : getLicenses()) {
			if (l.getStatus() != null && l.getStatus().getValue().equalsIgnoreCase(LICENSE_STATUS_IN_PROCESS)) {
				list.add(l);
			}
		}
		return list;
	}

	public License getLicenseById(Long id) {
		if (id != null) {
			for (License license : licenses) {
				final Long licenseId = license.getId();
				if (id.equals(licenseId)) {
					return license;
				}
			}
		}
		return null;
	}

	public License getLicenseByNumber(Long number) {
		if (number != null) {
			for (License license : licenses) {
				final Long licenseNumber = license.getLicenseNumber();
				if (number.equals(licenseNumber)) {
					return license;
				}
			}
		}

		return null;
	}

	public Set<Rating> getRatings() {
		return ratings;
	}

	public void setRatings(Set<Rating> ratings) {
		this.ratings = ratings;
	}

	public Rating getRating(Long id) {
		if (id != null) {
			for (Rating rating : ratings) {
				if (rating.getId().equals(id)) {
					return rating;
				}
			}
		}

		return null;
	}

	public void addRating(Rating rating) {
		if (rating != null) {
			rating.setFacility(this);
			ratings.add(rating);
		}
	}

	public void removeRating(Long id) {
		for (Iterator<Rating> itr = getRatings().iterator(); itr.hasNext();) {
			Rating rating = itr.next();
			if (rating.getId().equals(id)) {
				itr.remove();
				break;
			}
		}
	}

	public void removeRating(Rating rating) {
		ratings.remove(rating);
	}

	public Set<Accreditation> getAccreditations() {
		return accreditations;
	}

	public void setAccreditations(Set<Accreditation> accreditations) {
		this.accreditations = accreditations;
	}

	public Accreditation getAccreditation(Long id) {
		if (id != null) {
			for (Accreditation accreditation : accreditations) {
				if (accreditation.getId().equals(id)) {
					return accreditation;
				}
			}
		}

		return null;
	}

	public void addAccreditation(Accreditation accreditation) {
		if (accreditation != null) {
			accreditation.setFacility(this);
			accreditations.add(accreditation);
		}
	}

	public void removeAccreditation(Long id) {
		for (Iterator<Accreditation> itr = getAccreditations().iterator(); itr.hasNext();) {
			Accreditation accreditation = itr.next();
			if (accreditation.getId().equals(id)) {
				itr.remove();
				break;
			}
		}
	}

	public void removeAccreditation(Accreditation accreditation) {
		accreditations.remove(accreditation);
	}

	public Set<Exemption> getExemptions() {
		return exemptions;
	}

	public void setExemptions(Set<Exemption> exemptions) {
		this.exemptions = exemptions;
	}

	public Exemption getExemption(Long id) {
		if (id != null) {
			for (Exemption exemption : exemptions) {
				if (exemption.getId().equals(id)) {
					return exemption;
				}
			}
		}

		return null;
	}

	public List<Exemption> getActiveExemptions() {
		List<Exemption> exemptions = new ArrayList<Exemption>();
		for (Exemption e : this.exemptions) {
			if (e.isActive()) {
				exemptions.add(e);
			}
		}
		return exemptions;
	}

	public void addExemption(Exemption exemption) {
		if (exemption != null) {
			exemption.setFacility(this);
			exemptions.add(exemption);
		}
	}

	public void removeExemption(Long id) {
		for (Iterator<Exemption> itr = getExemptions().iterator(); itr.hasNext();) {
			Exemption exemption = itr.next();
			if (exemption.getId().equals(id)) {
				itr.remove();
				break;
			}
		}
	}

	public void removeExemption(Exemption exemption) {
		if (exemption != null) {
			exemptions.remove(exemption);
		}
	}

	public Set<FacilityAssociation> getAssociatedFacilities() {
		return associatedFacilities;
	}

	public void setAssociatedFacilities(Set<FacilityAssociation> associatedFacilities) {
		this.associatedFacilities = associatedFacilities;
	}

	public FacilityAssociation getAssociatedFacility(Long id) {
		if (id != null) {
			for (FacilityAssociation associatedFacility : associatedFacilities) {
				if (associatedFacility.getId().equals(id)) {
					return associatedFacility;
				}
			}
		}

		return null;
	}

	public FacilityAssociation getAssociatedFacilityWithParentChild(Long parentId, Long childId) {
		if (parentId != null && childId != null) {
			for (FacilityAssociation associatedFacility : associatedFacilities) {
				if (associatedFacility.getParent().getId().equals(parentId) && associatedFacility.getChild().getId().equals(childId)) {
					return associatedFacility;
				}
			}
		}

		return null;
	}

	public void addAssociatedFacility(FacilityAssociation associatedFacility) {
		if (associatedFacility != null) {
			associatedFacility.setParent(this);
			associatedFacilities.add(associatedFacility);
		}
	}

	public void addAssociatedFacilityOtherWay(FacilityAssociation associatedFacility) {
		if (associatedFacility != null) {
			associatedFacilities.add(associatedFacility);
		}
	}

	public void removeAssociatedFacility(Long id) {
		for (Iterator<FacilityAssociation> itr = getAssociatedFacilities().iterator(); itr.hasNext();) {
			FacilityAssociation associatedFacility = itr.next();
			if (associatedFacility.getId().equals(id)) {
				itr.remove();
				break;
			}
		}
	}

	public void removeAssociatedFacility(FacilityAssociation associatedFacility) {
		associatedFacilities.remove(associatedFacility);
	}

	public Set<FacilityPerson> getPeople() {
		return people;
	}

	public void setPeople(Set<FacilityPerson> people) {
		this.people = people;
	}

	public Set<CmpTransaction> getFees() {
		return fees;
	}

	public void setFees(Set<CmpTransaction> fees) {
		this.fees = fees;
	}
	
	public List<CmpTransaction> getNonInspectionFees(Boolean approval) {
		List<CmpTransaction> f = new ArrayList<CmpTransaction>();
		if (getFees().size() > 0) {
			for (CmpTransaction fee : fees) {
				if (fee.getInspection() == null) {
					if (approval != null) {
						if (fee.getApproval().equals(approval)) {
							f.add(fee);
						}
					} else {
						f.add(fee);
					}
				}
			}
		}
		return f;
	}

	public CmpTransaction getLatestNonInspectionFee(Boolean approval) {
		List<CmpTransaction> results = getNonInspectionFees(approval);
		if (results == null || results.size() < 1) {
			return null;
		}
		return results.get(0);
	}
	
	public CmpTransaction getFirstNonInspectionFee(Boolean approval) {
		List<CmpTransaction> results = getNonInspectionFees(approval);
		if (results == null || results.size() < 1) {
			return null;
		}
		return results.get(results.size()-1);
	}
	
	public List<CmpTransaction> getInspectionFees(Boolean approval) {
		List<CmpTransaction> f = new ArrayList<CmpTransaction>();
		if (getFees().size() > 0) {
			for (CmpTransaction fee : fees) {
				if (fee.getInspection() != null) {
					if (approval != null) {
						if (fee.getApproval().equals(approval)) {
							f.add(fee);
						}
					} else {
						f.add(fee);
					}
				}
			}
		}
		return f;
	}

	public FacilityPerson getPerson(Long id) {
		if (id != null) {
			for (FacilityPerson person : people) {
				if (person.getId().equals(id)) {
					return person;
				}
			}
		}

		return null;
	}

	public List<FacilityPerson> getPeopleOfType(PickListValue type, boolean activeOnly) {
		List<FacilityPerson> peopleOfType = new ArrayList<FacilityPerson>();
		if (type != null && type.getId() != null) {
			for (FacilityPerson person : people) {
				if (person.getType().getId().equals(type.getId()) && (!activeOnly || person.isActive())) {
					peopleOfType.add(person);
				}
			}
		}
		return peopleOfType;
	}

	public List<FacilityPerson> getPeopleOfType(String type, boolean activeOnly) {
		List<FacilityPerson> peopleOfType = new ArrayList<FacilityPerson>();
		if (StringUtils.isNotBlank(type)) {
			for (FacilityPerson person : people) {
				final PickListValue personType = person.getType();
				if (personType != null && type.equals(personType.getValue()) && (!activeOnly || person.isActive())) {
					peopleOfType.add(person);
				}
			}
		}
		return peopleOfType;
	}

	public FacilityPerson getCurrentPersonOfType(PickListValue type) {
		List<FacilityPerson> people = getPeopleOfType(type, true);
		if (!people.isEmpty()) {
			return people.get(0);
		}
		return null;
	}

	public void addPerson(FacilityPerson person) {
		if (person != null) {
			person.setFacility(this);
			if (person.getStartDate() == null) {
				person.setStartDate(new Date());
			}
			people.add(person);
		}
	}

	public void addPerson(Person person, PickListValue type, Date startDate, Date endDate) {
		if (person != null) {
			FacilityPerson fp = new FacilityPerson();
			fp.setFacility(this);
			fp.setPerson(person);
			fp.setType(type);
			fp.setStartDate(startDate == null ? new Date() : startDate);
			fp.setEndDate(endDate);

			people.add(fp);
		}
	}

	public void addSinglePerson(FacilityPerson person) {
		if (person != null) {
			person.setFacility(this);
			// default the start date to today
			if (person.getStartDate() == null) {
				person.setStartDate(DateUtils.truncate(new Date(), Calendar.DATE));
			}
			// if the person has an end date then they are just adding historical data
			// if the person does not have an end date then they are the new person in that role - deactivate the old person
			if (person.getEndDate() == null) {
				List<FacilityPerson> peopleOfType = getPeopleOfType(person.getType(), true);
				for (FacilityPerson oldPerson : peopleOfType) {
					if (oldPerson.getEndDate() == null) {
						if (oldPerson.getStartDate().getTime() == person.getStartDate().getTime()) {
							people.remove(oldPerson);
						} else {
							oldPerson.setEndDate(DateUtils.addDays(person.getStartDate(), -1));
						}
					}
				}
			}
			people.add(person);
		}
	}

	public void addSinglePerson(Person person, PickListValue type, Date startDate, Date endDate) {
		if (person != null) {
			FacilityPerson fp = new FacilityPerson();
			fp.setPerson(person);
			fp.setType(type);
			fp.setStartDate(startDate);
			fp.setEndDate(endDate);
			addSinglePerson(fp);
		}
	}

	public void removePerson(Long id) {
		FacilityPerson person = getPerson(id);
		people.remove(person);
	}

	public void removePerson(FacilityPerson person) {
		people.remove(person);
	}

	public Set<FacilityTag> getTags() {
		return tags;
	}

	public void setTags(Set<FacilityTag> tags) {
		this.tags = tags;
	}

	public List<FacilityTag> getTags(PickListValue tagType) {
		List<FacilityTag> tags = new ArrayList<FacilityTag>();
		for (FacilityTag tag : this.tags) {
			if (tag.getTag().getId().equals(tagType.getId())) {
				tags.add(tag);
			}
		}
		Collections.sort(tags);
		return tags;
	}

	public List<FacilityTag> getTags(List<PickListValue> tagTypes) {
		List<FacilityTag> tags = new ArrayList<FacilityTag>();
		for (FacilityTag tag : this.tags) {
			if (tagTypes.contains(tag.getTag())) {
				tags.add(tag);
			}
		}
		return tags;
	}

	public FacilityTag getTag(Long id) {
		if (id != null) {
			for (FacilityTag tag : tags) {
				if (tag.getId().equals(id)) {
					return tag;
				}
			}
		}

		return null;
	}

	public List<FacilityTag> getActiveTags(PickListValue tagType) {
		List<FacilityTag> tags = new ArrayList<FacilityTag>();
		for (FacilityTag tag : this.tags) {
			if (tag.isActive() && tagType != null && tag.getTag().getId().equals(tagType.getId())) {
				tags.add(tag);
			}
		}
		return tags;
	}

	public void addTag(FacilityTag tag) {
		if (tag != null) {
			tag.setFacility(this);
			tags.add(tag);
		}
	}

	public void removeTag(Long id) {
		for (Iterator<FacilityTag> itr = getTags().iterator(); itr.hasNext();) {
			FacilityTag tag = itr.next();
			if (tag.getId().equals(id)) {
				itr.remove();
				break;
			}
		}
	}

	public void removeTag(FacilityTag tag) {
		if (tag != null) {
			tags.remove(tag);
		}
	}

	public List<FacilityPerson> getProviders() {
		List<FacilityPerson> contacts = new ArrayList<FacilityPerson>();
		for (FacilityPerson p : getPeople()) {
			if (p.getType().getValue().equalsIgnoreCase("Primary Contact") || p.getType().getValue().equalsIgnoreCase("Secondary Contact")) {
				contacts.add(p);
			}
		}

		return contacts;
	}

	public List<FacilityPerson> getPrimaryContacts() {
		List<FacilityPerson> primaryContacts = new ArrayList<FacilityPerson>();
		for (FacilityPerson p : getPeople()) {
			final PickListValue personType = p.getType();
			if (personType != null) {
				final String personTypeValue = personType.getValue();
				if ("Primary Contact".equalsIgnoreCase(personTypeValue)) {
					primaryContacts.add(p);
				}
			}
		}

		return primaryContacts;
	}

	public Date getRulesCertReceived() {
		return rulesCertReceived;
	}

	public void setRulesCertReceived(Date rulesCertReceived) {
		this.rulesCertReceived = rulesCertReceived;
	}

	public Date getCodeOfConductReceived() {
		return codeOfConductReceived;
	}

	public void setCodeOfConductReceived(Date codeOfConductReceived) {
		this.codeOfConductReceived = codeOfConductReceived;
	}

	public Date getConfidentialFormReceived() {
		return confidentialFormReceived;
	}

	public void setConfidentialFormReceived(Date confidentialFormReceived) {
		this.confidentialFormReceived = confidentialFormReceived;
	}

	public Date getEmergencyPlanReceived() {
		return emergencyPlanReceived;
	}

	public void setEmergencyPlanReceived(Date emergencyPlanReceived) {
		this.emergencyPlanReceived = emergencyPlanReceived;
	}

	public Date getReferenceOneReceived() {
		return referenceOneReceived;
	}

	public void setReferenceOneReceived(Date referenceOneReceived) {
		this.referenceOneReceived = referenceOneReceived;
	}

	public Date getReferenceTwoReceived() {
		return referenceTwoReceived;
	}

	public void setReferenceTwoReceived(Date referenceTwoReceived) {
		this.referenceTwoReceived = referenceTwoReceived;
	}

	public Date getReferenceThreeReceived() {
		return referenceThreeReceived;
	}

	public void setReferenceThreeReceived(Date referenceThreeReceived) {
		this.referenceThreeReceived = referenceThreeReceived;
	}

	public Long getSafeProviderId() {
		return safeProviderId;
	}

	public void setSafeProviderId(Long safeProviderId) {
		this.safeProviderId = safeProviderId;
	}

	public boolean isNoPublicListing() {
		return noPublicListing;
	}
	
	public void setNoPublicListing(boolean noPublicListing) {
		this.noPublicListing = noPublicListing;
	}
	
	public Person getCbsTechnician() {
		return cbsTechnician;
	}
	
	public void setCbsTechnician(Person cbsTechnician) {
		this.cbsTechnician = cbsTechnician;
	}
	
	public String getActive() {
		return active;
	}

	public void setActive(String active) {
		this.active = active;
	}

	@Override
	public String[] getLoggedProperties() {
		return LOGGED_PROPERTIES;
	}

	@Override
	public String toString() {
		return getIdNumber() + " - " + getDisplayName();
	}

	@Override
	public int compareTo(Facility o) {
		if (this == o) {
			return 0;
		}
		int comp = CompareUtils.nullSafeComparableCompare(getName(), o.getName(), true);
		if (comp == 0) {
			comp = CompareUtils.nullSafeComparableCompare(getStatus(), o.getStatus(), false);
		}
		if (comp == 0) {
			comp = CompareUtils.nullSafeComparableCompare(getType(), o.getType(), false);
		}
		if (comp == 0) {
			comp = CompareUtils.nullSafeComparableCompare(getId(), o.getId(), false);
		}

		return comp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
		result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
		result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Facility)) {
			return false;
		}
		Facility other = (Facility) obj;
		if (getId() == null) {
			if (other.getId() != null) {
				return false;
			}
		} else if (!getId().equals(other.getId())) {
			return false;
		}
		if (getName() == null) {
			if (other.getName() != null) {
				return false;
			}
		} else if (!getName().equals(other.getName())) {
			return false;
		}
		if (getStatus() == null) {
			if (other.getStatus() != null) {
				return false;
			}
		} else if (!getStatus().equals(other.getStatus())) {
			return false;
		}
		if (getType() == null) {
			if (other.getType() != null) {
				return false;
			}
		} else if (!getType().equals(other.getType())) {
			return false;
		}
		return true;
	}

	public boolean isValid() {
		if (!StringUtils.isNotBlank(getName())) {
			return false;
		}
		if (getPrimaryPhone() == null || !StringUtils.isNotBlank(getPrimaryPhone().getFormattedPhoneNumber())) {
			return false;
		}
		if (!StringUtils.isNotBlank(getEmail())) {
			return false;
		}
		if (getMailingAddress() == null || 
				!StringUtils.isNotBlank(getMailingAddress().getAddressOne()) ||
				!StringUtils.isNotBlank(getMailingAddress().getZipCode()) ||
				!StringUtils.isNotBlank(getMailingAddress().getCity()) ||
				!StringUtils.isNotBlank(getMailingAddress().getState())) 
		{
			return false;
		}
		if (getType() == null) {
			return false;
		}
		if (getStatus() != null) {
			if (getStatus() == FacilityStatus.REGULATED || getStatus() == FacilityStatus.IN_PROCESS) {
				if (getStatus() == FacilityStatus.REGULATED) {
					if (getInitialRegulationDate() == null) {
						return false;
					}
				}
				if (getLicensingSpecialist() == null) {
					return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}

	public Date getClosedDate() {
		return closedDate;
	}

	public void setClosedDate(Date closedDate) {
		this.closedDate = closedDate;
	}

	public PickListValue getClosedReason() {
		return closedReason;
	}

	public void setClosedReason(PickListValue closedReason) {
		this.closedReason = closedReason;
	}

	public Facility getParent() {
		return parent;
	}

	public void setParent(Facility parent) {
		this.parent = parent;
	}

	public Facility getOwner() {
		return owner;
	}

	public void setOwner(Facility owner) {
		this.owner = owner;
	}

	public Boolean getNonLicensed() {
		return nonLicensed;
	}

	public void setNonLicensed(Boolean nonLicensed) {
		this.nonLicensed = nonLicensed;
	}

	public String getDhsContractNumbers() {
		return dhsContractNumbers;
	}

	public void setDhsContractNumbers(String dhsContractNumbers) {
		this.dhsContractNumbers = dhsContractNumbers;
	}
	
	public boolean isDspdCertified() {
		return FacilityUtil.isDSPDCertified(this);
	}

}