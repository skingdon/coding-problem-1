/**
 * 
 */
package gov.utah.dts.det.ccl.service.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import gov.utah.dts.det.admin.model.PickListValue;
import gov.utah.dts.det.admin.service.PickListService;
import gov.utah.dts.det.ccl.dao.FacilityAttachmentDao;
import gov.utah.dts.det.ccl.dao.FacilityDao;
import gov.utah.dts.det.ccl.dao.FacilityPersonDao;
import gov.utah.dts.det.ccl.dao.LicenseTermDao;
import gov.utah.dts.det.ccl.dao.TagMapDao;
import gov.utah.dts.det.ccl.exceptions.CclServiceException;
import gov.utah.dts.det.ccl.model.ActionLog;
import gov.utah.dts.det.ccl.model.ApplicationProperty;
import gov.utah.dts.det.ccl.model.Complaint;
import gov.utah.dts.det.ccl.model.Exemption;
import gov.utah.dts.det.ccl.model.Facility;
import gov.utah.dts.det.ccl.model.FacilityAttachment;
import gov.utah.dts.det.ccl.model.FacilityPerson;
import gov.utah.dts.det.ccl.model.FacilityTag;
import gov.utah.dts.det.ccl.model.License;
import gov.utah.dts.det.ccl.model.LicenseTerm;
import gov.utah.dts.det.ccl.model.Person;
import gov.utah.dts.det.ccl.model.Region;
import gov.utah.dts.det.ccl.model.Review;
import gov.utah.dts.det.ccl.model.Tag;
import gov.utah.dts.det.ccl.model.TagMap;
import gov.utah.dts.det.ccl.model.UnlicensedComplaint;
import gov.utah.dts.det.ccl.model.enums.ApplicationPropertyKey;
import gov.utah.dts.det.ccl.model.enums.FacilityEventType;
import gov.utah.dts.det.ccl.model.enums.RoleType;
import gov.utah.dts.det.ccl.model.view.AccreditationExpiringView;
import gov.utah.dts.det.ccl.model.view.AlertFollowUpsNeededView;
import gov.utah.dts.det.ccl.model.view.AnnouncedInspectionNeededView;
import gov.utah.dts.det.ccl.model.view.BasicFacilityInformation;
import gov.utah.dts.det.ccl.model.view.BasicFacilityView;
import gov.utah.dts.det.ccl.model.view.ConditionalFacilityView;
import gov.utah.dts.det.ccl.model.view.ErepView;
import gov.utah.dts.det.ccl.model.view.ExemptVerificationView;
import gov.utah.dts.det.ccl.model.view.ExpiredAndExpiringLicenseView;
import gov.utah.dts.det.ccl.model.view.FacilityCaseloadView;
import gov.utah.dts.det.ccl.model.view.FacilityChecksReceivedView;
import gov.utah.dts.det.ccl.model.view.FacilityContactView;
import gov.utah.dts.det.ccl.model.view.FacilityEventView;
import gov.utah.dts.det.ccl.model.view.FacilityLicenseView;
import gov.utah.dts.det.ccl.model.view.FacilityLookupView;
import gov.utah.dts.det.ccl.model.view.FacilitySearchView;
import gov.utah.dts.det.ccl.model.view.FileCheckView;
import gov.utah.dts.det.ccl.model.view.NewApplicationPendingDeadlineView;
import gov.utah.dts.det.ccl.model.view.OpenReviewView;
import gov.utah.dts.det.ccl.model.view.OutstandingCmpView;
import gov.utah.dts.det.ccl.model.view.SortableFacilityView;
import gov.utah.dts.det.ccl.model.view.TrackingRecordScreeningApprovalsView;
import gov.utah.dts.det.ccl.model.view.UnannouncedInspectionNeededView;
import gov.utah.dts.det.ccl.model.view.WorkInProgressView;
import gov.utah.dts.det.ccl.service.ActionLogService;
import gov.utah.dts.det.ccl.service.ComplaintService;
import gov.utah.dts.det.ccl.service.FacilityLookupCriteria;
import gov.utah.dts.det.ccl.service.FacilitySearchCriteria;
import gov.utah.dts.det.ccl.service.FacilityService;
import gov.utah.dts.det.ccl.service.RegionService;
import gov.utah.dts.det.ccl.service.UserCaseloadCount;
import gov.utah.dts.det.ccl.util.FacilityUtil;
import gov.utah.dts.det.ccl.view.FacilityResultView;
import gov.utah.dts.det.ccl.view.KeyValuePair;
import gov.utah.dts.det.ccl.view.ListRange;
import gov.utah.dts.det.ccl.view.facility.FacilityStatus;
import gov.utah.dts.det.query.SortBy;
import gov.utah.dts.det.service.ApplicationService;
import gov.utah.dts.det.util.ScpUtil;

/**
 * @author DOLSEN
 * 
 */
@Service("facilityService")
public class FacilityServiceImpl implements FacilityService {

	private static final Logger logger = LoggerFactory.getLogger(FacilityServiceImpl.class);

	private static final String PERSON_TYPE_PICK_LIST_NAME = "Facility Person Type";
	private static final String FIRST_DIRECTOR_PERSON_TYPE_NAME = "First Director";
	private static final String SECOND_DIRECTOR_PERSON_TYPE_NAME = "Second Director";
	private static final String PRIMARY_OWNER_PERSON_TYPE_NAME = "Primary Owner";
	private static final String SECONDARY_OWNER_PERSON_TYPE_NAME = "Secondary Owner";
	private static final String BOARD_MEMBER_PERSON_TYPE_NAME = "Board Member";
	private static final String SECONDARY_CONTACT_PERSON_TYPE_NAME = "Secondary Contact";
	private static final String PRIMARY_CONTACT_PERSON_TYPE_NAME = "Primary Contact";
	private static final String LICENSE_STATUS_IN_PROCESS = "In Process";

	private static final String CSV_FIELD_START = ",\"";
	private static final String CSV_FIELD_END = "\"";

	@Autowired
	private FacilityDao facilityDao;
	
	@Autowired
	private TagMapDao tagMapDao;
	
	@Autowired
	private FacilityPersonDao facilityPersonDao;
	@Autowired
	private ApplicationService applicationService;
	@Autowired
	private PickListService pickListService;
	@Autowired
	private ActionLogService actionLogService;
	@Autowired
	private ComplaintService complaintService;
	@Autowired
	private RegionService regionService;
	@Autowired
	private TransactionTemplate sharedTransactionTemplate;
	@Autowired
	private FacilityAttachmentDao facilityAttachmentDao;
	@Autowired
	private LicenseTermDao licenseTermDao;

	@Override
	public Facility loadById(Long id) {
		return facilityDao.getFacility(id);
	}

	@Override
	public Facility loadFacilityByIdNumber(String idNumber) {
		return facilityDao.loadFacilityByIdNumber(idNumber);
	}

	@Override
	public License loadLicenseById(Long licenseId) {
		return facilityDao.loadLicenseById(licenseId);
	}

	@Override
	public Facility createLicensedFacility(Facility facility, Person licensingSpecialist) throws CclServiceException {
		
		if (facility == null) {
			throw new IllegalArgumentException("Facility must not be null");
		}
		if (licensingSpecialist == null) {
			throw new IllegalArgumentException("Licensing specialist must not be null");
		}

		setLicensingSpecialist(facility, licensingSpecialist);

		facility.setStatus(FacilityStatus.IN_PROCESS);

		final boolean isLicensedFacility = (!facility.getNonLicensed());
		if (isLicensedFacility) {
			final License license = createInitialLicense();
			facility.addLicense(license);
		}

		if (FacilityUtil.isFosterProvider(facility)) {
			createPrimaryProvider(facility);
		}
		
		facility = facilityDao.save(facility);
		setFacilityId(facility);

		return facility;
	}
	
	private void createPrimaryProvider(Facility facility) {
		
		final FacilityPerson provider = new FacilityPerson();
		provider.setFacility(facility);
		
		final PickListValue role = pickListService.getPickListValueByValue("Provider", "Foster Care Person Roles");
		provider.setRole(role);
		
		final Person person = new Person();
		person.setAddress(facility.getLocationAddress());
		person.setEmail(facility.getEmail());
		person.setPrimaryPhone(facility.getPrimaryPhone());
		person.setAlternatePhone(facility.getAlternatePhone());	
		
		final String name = facility.getName();
		final String[] names = name.split(", ");
		
		if (names.length > 0) {
			final String lastName = names[0];
			person.setLastName(lastName);
		}
		if (names.length > 1) {
			final String firstName = names[1];
			person.setFirstName(firstName);
		}
		provider.setPerson(person);
		
		final Set<FacilityPerson> people = new HashSet<>();
		people.add(provider);
		facility.setPeople(people);
	}
	
	private License createInitialLicense() {
		
		final License license = new License();
		
		final Date today = DateUtils.truncate(new Date(), Calendar.DATE);
		license.setStartDate(today);
		license.setExpirationDate(License.DEFAULT_LICENSE_END_DATE);
		license.setCalculatesAlerts(false);
		
		final String inProcessStatusKey = ApplicationPropertyKey.IN_PROCESS_LICENSE_STATUS.getKey();
		final PickListValue licenseStatusInProcess = applicationService.getPickListValueForApplicationProperty(inProcessStatusKey);
		license.setStatus(licenseStatusInProcess);
		
		return license;
	}

	@Override
	public Facility createUnlicensedFacility(Facility facility, Person owner, UnlicensedComplaint complaint) {
		if (facility == null) {
			throw new IllegalArgumentException("Facility must not be null");
		}
		if (complaint == null) {
			throw new IllegalArgumentException("Complaint must not be null");
		}

		if (owner != null) {
			PickListValue ownerType = applicationService
					.getPickListValueForApplicationProperty(ApplicationPropertyKey.FACILITY_OWNER_TYPE.getKey());
			facility.addPerson(owner, ownerType, DateUtils.truncate(new Date(), Calendar.DATE), null);
		}

		facility.setMailingAddress(facility.getLocationAddress());

		facility.setStatus(FacilityStatus.INACTIVE);

		// set the region for the facility
		setRegionByAddress(facility);

		facility = facilityDao.save(facility);
		setFacilityId(facility);

		// save the complaint now
		complaint.setFacility(facility);
		complaintService.createComplaint(complaint);

		return facility;
	}

	private void setFacilityId(Facility facility) {
		facility.setIdNumber("F" + String.valueOf(Calendar.getInstance().get(Calendar.YEAR)).substring(2, 4) + "-"
				+ facility.getId());
		facility = facilityDao.save(facility);
	}

	@Override
	public Facility saveFacility(Facility facility) {
		// update the region based on the address if there is no region or the
		// facility is not a regulated facility.
		// we never want to update the region on a regulated facility to the one
		// given by the address because the region is set by the
		// licensor.
		if (facility.getRegion() == null || facility.getStatus() != FacilityStatus.REGULATED) {
			setRegionByAddress(facility);
		}

		return facilityDao.save(facility);
	}

	@Override
	public Facility activateRegulatedFacility(Facility facility, Person licensingSpecialist)
			throws CclServiceException {
		if (facility == null) {
			throw new IllegalArgumentException("Facility must not be null");
		}
		if (facility.getStatus() != FacilityStatus.REGULATED) {
			List<String> errors = new ArrayList<String>();
			// make sure there is a specialist
			if (licensingSpecialist == null && facility.getLicensingSpecialist() == null) {
				errors.add("You must provide a licensing specialist in order to activate this facility");
			}

			// set the licensing specialist - this also sets the region
			setLicensingSpecialist(facility, licensingSpecialist);

			facility.setStatus(FacilityStatus.REGULATED);

			if (!errors.isEmpty()) {
				throw new CclServiceException("Unable to activate regulated facility", errors);
			}

			facility = facilityDao.save(facility);

			ActionLog statusChangeLog = new ActionLog();
			statusChangeLog.setFacility(facility);
			statusChangeLog.setActionDate(new Date());
			statusChangeLog.setActionType(applicationService.getPickListValueForApplicationProperty(
					ApplicationPropertyKey.FACILITY_ACTIVATION_ACTION.getKey()));

			actionLogService.saveActionLog(statusChangeLog, null);
		}

		return facility;
	}

	@Override
	public Facility updateLicensingSpecialist(Facility facility, Person licensingSpecialist) {
		setLicensingSpecialist(facility, licensingSpecialist);
		return facilityDao.save(facility);
	}

	@Override
	public Facility updateInitialRegulationDate(Facility facility, Date initialRegulationDate) {
		if (facility == null) {
			throw new IllegalArgumentException("Facility must not be null");
		}
		if (initialRegulationDate != null && !initialRegulationDate.equals(facility.getInitialRegulationDate())) {
			facility.setInitialRegulationDate(initialRegulationDate);

			facility = facilityDao.save(facility);
		}

		return facility;
	}

	@Override
	public Facility activateExemptFacility(Facility facility) throws CclServiceException {
		if (facility == null) {
			throw new IllegalArgumentException("Facility must not be null.");
		}
		if (facility.getStatus() != FacilityStatus.EXEMPT) {
			if (facility.getActiveExemptions().isEmpty()) {
				throw new CclServiceException("Unable to activate exempt facility",
						"error.facility.activate.no-current-exemptions");
			}

			facility.setStatus(FacilityStatus.EXEMPT);
			facility = facilityDao.save(facility);

			ActionLog statusChangeLog = new ActionLog();
			statusChangeLog.setFacility(facility);
			statusChangeLog.setActionDate(new Date());
			statusChangeLog.setActionType(applicationService.getPickListValueForApplicationProperty(
					ApplicationPropertyKey.FACILITY_ACTIVATION_ACTION.getKey()));

			actionLogService.saveActionLog(statusChangeLog, null);
		}

		return facility;
	}

	@Override
	public Facility setFacilityInProcess(Facility facility, Person licensingSpecialist) throws CclServiceException {
		if (facility == null) {
			throw new IllegalArgumentException("Facility must not be null.");
		}
		if (facility.getStatus() == FacilityStatus.INACTIVE) {
			// make sure there is a specialist
			if (licensingSpecialist == null && facility.getLicensingSpecialist() == null) {
				throw new CclServiceException("Unable to change facility status",
						"You must provide a licensing specialist in order to activate this facility");
			}

			setLicensingSpecialist(facility, licensingSpecialist);

			facility.setStatus(FacilityStatus.IN_PROCESS);
			facilityDao.save(facility);
		}

		return facility;
	}

	@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN','ROLE_ADMIN_MANAGER','ROLE_OFFICE_SPECIALIST')")
	protected void setLicensingSpecialist(Facility facility, Person licensingSpecialist) {
		if (facility != null && licensingSpecialist != null) {
			// only perform the change if there is no detail, no specialist, or
			// the specialists are not the same
			if (facility.getLicensingSpecialist() == null
					|| !licensingSpecialist.equals(facility.getLicensingSpecialist())) {
				facility.setLicensingSpecialist(licensingSpecialist);

				// set the region for the facility
				Region r = regionService.getRegionForSpecialist(licensingSpecialist.getId());

				// only set the region if it is not null because it may have
				// already been set before the facility was deactivated
				if (r != null) {
					facility.setRegion(r);
				}
			}
		}
	}

	@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN','ROLE_ADMIN_MANAGER','ROLE_OFFICE_SPECIALIST')")
	protected void setCbsTechnician(Facility facility, Person cbsTechnician) {
		if (facility != null && cbsTechnician != null) {
			// only perform the change if there is no detail, no specialist, or
			// the specialists are not the same
			if (facility.getCbsTechnician() == null || !cbsTechnician.equals(facility.getCbsTechnician())) {
				facility.setCbsTechnician(cbsTechnician);
			}
		}
	}

	protected void setRegionByLicensingSpecialist(Facility facility, Person licensingSpecialist) {
		if (facility != null && licensingSpecialist != null) {
			Region r = regionService.getRegionForSpecialist(licensingSpecialist.getId());
			if (r != null) {
				facility.setRegion(r);
			}
		}
	}

	protected void setRegionByAddress(Facility facility) {
		if (facility != null) {
			Region r = regionService.getRegionForAddress(facility.getLocationAddress());
			if (r != null) {
				facility.setRegion(r);
			}
		}
	}

	@Override
	public Facility deactivateFacility(Facility facility, PickListValue reason, Date effectiveDate) throws CclServiceException {
		
		if (facility != null && facility.getStatus() != FacilityStatus.INACTIVE) {
			if (facility.getStatus() != FacilityStatus.IN_PROCESS && reason == null) {
				throw new IllegalArgumentException("Reason must not be null");
			}

			if (facility.getStatus() == FacilityStatus.IN_PROCESS) {
				// delete all in process licenses
				for (Iterator<License> itr = facility.getLicenses().iterator(); itr.hasNext();) {
					License lic = itr.next();
					if (lic.getStatus() != null
							&& lic.getStatus().getValue().equalsIgnoreCase(LICENSE_STATUS_IN_PROCESS)) {
						itr.remove();
					}
				}
				facility.setStatus(FacilityStatus.INACTIVE);
				facility = facilityDao.save(facility);
			} else {
				Date now = new Date();

				Date eDate = effectiveDate == null ? now : effectiveDate;

				// make sure the deactivation can't happen too far in the past
				if (eDate.compareTo(DateUtils.addDays(now, -60)) < 0) {
					throw new CclServiceException("Unable to deactivate facility",
							"error.facility.deactivate.past-retroactive-deadline");
				}

				facility = performDeactivation(facility, reason, eDate);
			}
		}

		return facility;
	}

	protected Facility performDeactivation(Facility facility, PickListValue reason, Date effectiveDate) {
		List<PickListValue> deactivationReasons = pickListService.getValuesForPickList("Facility Deactivation Reasons",
				true);
		if (effectiveDate.compareTo(new Date()) > 0) {
			List<FacilityTag> deactTags = facility.getTags(deactivationReasons);
			if (deactTags.isEmpty()) {
				// set the effective date
				FacilityTag deact = new FacilityTag();
				deact.setTag(reason);
				deact.setStartDate(effectiveDate);
				deact.setExpirationDate(effectiveDate);
				facility.addTag(deact);
			} else {
				FacilityTag deact = deactTags.get(0);
				deact.setTag(reason);
				deact.setStartDate(effectiveDate);
				deact.setExpirationDate(effectiveDate);
			}
		} else {
			if (facility.getStatus() == FacilityStatus.INACTIVE) {
				return facility;
			}

			Date eDate = DateUtils.truncate(effectiveDate, Calendar.DATE);

			// any licenses and exemptions that start after the closure date
			// should be deleted
			for (Iterator<License> itr = facility.getLicenses().iterator(); itr.hasNext();) {
				
				final License license = itr.next();
				final Date startDate = license.getStartDate();
				
				final int startDateCompare = startDate.compareTo(effectiveDate);
				if (startDateCompare >= 0) {
					itr.remove();
				} else {
					final Date expirationDate = license.getExpirationDate();
					if (expirationDate != null && expirationDate.compareTo(effectiveDate) > 0) {
						license.setExpirationDate(eDate);
					}
				}
			}
			
			for (Iterator<Exemption> itr = facility.getExemptions().iterator(); itr.hasNext();) {
				Exemption ex = itr.next();
				if (ex.getStartDate().compareTo(effectiveDate) >= 0) {
					itr.remove();
				} else if (ex.getStartDate().compareTo(effectiveDate) < 0
						&& ex.getExpirationDate().compareTo(effectiveDate) > 0) {
					ex.setExpirationDate(effectiveDate);
				}
			}

			PickListValue condTag = applicationService
					.getPickListValueForApplicationProperty(ApplicationPropertyKey.TAG_CONDITIONAL.getKey());
			for (Iterator<FacilityTag> itr = facility.getTags().iterator(); itr.hasNext();) {
				FacilityTag tag = itr.next();
				if (tag.getTag().equals(condTag)) {
					if (tag.getStartDate().compareTo(effectiveDate) >= 0) {
						itr.remove();
					} else if (tag.getStartDate().compareTo(effectiveDate) < 0
							&& tag.getExpirationDate().compareTo(effectiveDate) > 0) {
						tag.setExpirationDate(effectiveDate);
					}
				} else if (deactivationReasons.contains(tag.getTag())) {
					// remove pending deactivation if it exists because we are
					// deactivating now
					itr.remove();
				}
			}

			ActionLog statusChangeLog = new ActionLog();
			statusChangeLog.setFacility(facility);
			statusChangeLog.setActionDate(effectiveDate);
			statusChangeLog.setActionType(reason);

			actionLogService.saveActionLog(statusChangeLog, null);

			facility.setStatus(FacilityStatus.INACTIVE);
		}

		return facilityDao.save(facility);
	}

	@Override
	public Facility cancelFacilityDeactivation(Facility facility) {
		List<PickListValue> deactivationReasons = pickListService.getValuesForPickList("Facility Deactivation Reasons",
				true);
		for (Iterator<FacilityTag> itr = facility.getTags().iterator(); itr.hasNext();) {
			FacilityTag tag = itr.next();
			if (deactivationReasons.contains(tag.getTag())) {
				itr.remove();
			}
		}
		return facilityDao.save(facility);
	}

	@Override
	public List<PickListValue> getStatusReasons(boolean active) {
		if (active) {
			return pickListService.getValuesForPickList("Facility Activation Reasons", true);
		} else {
			return pickListService.getValuesForPickList("Facility Deactivation Reasons", true);
		}
	}

	@Override
	public void saveLicense(Facility facility, License license) throws CclServiceException {
		boolean isInProcess = applicationService.propertyContainsPickListValue(license.getStatus(),
				ApplicationPropertyKey.IN_PROCESS_LICENSE_STATUS.getKey());

		if (isInProcess) {
			license.setExpirationDate(License.DEFAULT_LICENSE_END_DATE);
		}

		/*
		 * RM #25806. It is now ok to have overlapping licenses so
		 * validateLicense call is no longer necessary.
		 */
		// validateLicense(facility, license);

		final PickListValue licenseTermType = license.getSubtype();
		if (licenseTermType != null) {
			license.setCalculatesAlerts(applicationService.propertyContainsPickListValue(licenseTermType, ApplicationPropertyKey.DOES_NOT_CALCULATE_ALERTS_LICENSE_SUBTYPE.getKey()));
		}
		else {
			license.setCalculatesAlerts(false);
		}
		
		// add the license and license holder to the facility
		if (license.getFacility() == null) {
			facility.addLicense(license);
		}

		// if the initial regulation date has never been set, and this is a
		// license with a valid subtype, set it now.
		if (facility.getInitialRegulationDate() == null && !isInProcess) {
			facility.setInitialRegulationDate(license.getStartDate());
		}

		facilityDao.save(facility);
	}
	
	public void saveLicenseTerm(LicenseTerm licenseTerm) {
		licenseTermDao.save(licenseTerm);
	}

	@Override
	public Facility saveLicenseWithFacilityActivation(Facility facility, License license) throws CclServiceException {
		List<String> errors = new ArrayList<String>();

		final PickListValue licenseTermType = license.getSubtype();
		if (licenseTermType != null) {
		license.setCalculatesAlerts(applicationService.propertyContainsPickListValue(license.getSubtype(),
				ApplicationPropertyKey.DOES_NOT_CALCULATE_ALERTS_LICENSE_SUBTYPE.getKey()));
		}
		else {
			license.setCalculatesAlerts(false);
		}

		// add the license and license holder to the facility
		if (license.getFacility() == null) {
			facility.addLicense(license);
		}

		// if the initial regulation date has never been set, set it now.
		if (facility.getInitialRegulationDate() == null) {
			facility.setInitialRegulationDate(license.getStartDate());
		}

		// make sure there is a specialist
		if (facility.getLicensingSpecialist() == null) {
			errors.add("A licensing specialist must be set on this facility in order to activate this license.");
		}

		if (facility.getRegion() == null) {
			// set the region for the facility
			Region r = regionService.getRegionForSpecialist(facility.getLicensingSpecialist().getId());

			// only set the region if it is not null because it may have already
			// been set before the facility was deactivated
			if (r != null) {
				facility.setRegion(r);
			}

			// if the region is null then throw an error. We cannot have
			// licensed facilities that do not have a region
			if (facility.getRegion() == null) {
				errors.add(
						"The system was unable to assign a region to the facility because the licensor is not assigned to a region.");
			}
		}

		// Make sure an initial regulation date is set for the facility
		if (facility.getInitialRegulationDate() == null) {
			if (license.getStartDate() != null) {
				facility.setInitialRegulationDate(license.getStartDate());
			} else {
				facility.setInitialRegulationDate(new Date());
			}
		}

		facility.setStatus(FacilityStatus.REGULATED);

		if (!errors.isEmpty()) {
			throw new CclServiceException("Unable to activate regulated facility", errors);
		}

		facility = facilityDao.save(facility);

		ActionLog statusChangeLog = new ActionLog();
		statusChangeLog.setFacility(facility);
		statusChangeLog.setActionDate(new Date());
		statusChangeLog.setActionType(applicationService
				.getPickListValueForApplicationProperty(ApplicationPropertyKey.FACILITY_ACTIVATION_ACTION.getKey()));
		actionLogService.saveActionLog(statusChangeLog, null);

		return facility;
	}

	@Override
	public void removeLicense(License license) throws CclServiceException {
		if (license != null) {
			final Facility facility = license.getFacility();
			facility.removeLicense(license);
			facilityDao.save(facility);
		}
	}
	
	@Override
	public void saveExemption(Facility facility, Exemption exemption) throws CclServiceException {
		List<String> errors = new ArrayList<String>();

		for (License l : facility.getLicenses()) {
			if (gov.utah.dts.det.util.DateUtils.isOverlappingDateRanges(l.getStartDate(), l.getExpirationDate(),
					exemption.getStartDate(), exemption.getExpirationDate())) {
				errors.add("error.exemption.save.overlaps-license");
				break;
			}
		}

		if (!errors.isEmpty()) {
			throw new CclServiceException("Unable to create exemption", errors);
		}

		if (exemption.getFacility() == null) {
			facility.addExemption(exemption);
		}

		facilityDao.save(facility);
	}

	@Override
	public void saveConditionalStatus(Facility facility, FacilityTag status) throws CclServiceException {
		PickListValue conditionalTag = applicationService
				.getPickListValueForApplicationProperty(ApplicationPropertyKey.TAG_CONDITIONAL.getKey());
		List<FacilityTag> tags = facility.getTags(conditionalTag);
		for (FacilityTag tag : tags) {
			if (!tag.getId().equals(status.getId()) && gov.utah.dts.det.util.DateUtils.isOverlappingDateRanges(
					status.getStartDate(), status.getExpirationDate(), tag.getStartDate(), tag.getExpirationDate())) {
				throw new CclServiceException("Unable to save conditional status.",
						"error.conditional-status.overlaps-other-conditional");
			}
		}

		License lic = facility.getLicense(status.getStartDate(), false);
		if (lic == null || status.getExpirationDate().compareTo(lic.getExpirationDate()) > 0) {
			throw new CclServiceException("Unable to save conditional status.",
					"error.conditional-status.within-single-license");
		}

		if (status.getFacility() == null) {
			status.setTag(conditionalTag);
			facility.addTag(status);
		}

		facilityDao.save(facility);
	}

	@Override
	public FacilityPerson loadFacilityPerson(Long facilityId, Long facilityPersonId, Long firstType, Long secondType) {
		final FacilityPerson person = facilityDao.loadFacilityPerson(facilityId, facilityPersonId, firstType, secondType);
		return person;
	}
	@Override
	public FacilityPerson loadFacilityPerson(Long facilityPersonId) {
		return facilityDao.loadFacilityPerson(facilityPersonId);
	}

	@Override
	public FacilityPerson loadFacilityBoardMember(Long facilityId, Long facilityPersonId, Long facilityPersonType) {
		final FacilityPerson boardMember = facilityDao.loadFacilityBoardMember(facilityId, facilityPersonId, facilityPersonType);
		return boardMember;
	}

	@Override
	public List<TrackingRecordScreeningApprovalsView> loadScreenedPeopleForFacility(Long facilityId,
			boolean excludeDirectors, boolean excludeContacts) {
		return facilityDao.loadScreenedPeopleForFacility(facilityId, excludeDirectors, excludeContacts);
	}

	@Override
	public TrackingRecordScreeningApprovalsView loadScreenedPersonForFacility(Long facilityId, Long personId) {
		final TrackingRecordScreeningApprovalsView view = facilityDao.loadScreenedPersonForFacility(facilityId, personId);
		return view;
	}

	@Override
	public List<FacilityPerson> getDirectors(Long facilityId) {
		List<PickListValue> types = new ArrayList<PickListValue>();
		types.add(pickListService.getPickListValueByValue(FIRST_DIRECTOR_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME));
		types.add(
				pickListService.getPickListValueByValue(SECOND_DIRECTOR_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME));
		return facilityDao.loadFacilityPeople(facilityId, types);
	}

	@Override
	public PickListValue getSecondaryContactPersonType() {
		return pickListService.getPickListValueByValue(SECONDARY_CONTACT_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME);
	}

	@Override
	public PickListValue getPrimaryContactPersonType() {
		return pickListService.getPickListValueByValue(PRIMARY_CONTACT_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME);
	}

	@Override
	public List<FacilityPerson> getContacts(Long facilityId) {
		List<PickListValue> types = new ArrayList<PickListValue>();
		types.add(getSecondaryContactPersonType());
		types.add(getPrimaryContactPersonType());
		return facilityDao.loadFacilityPeople(facilityId, types);
	}
	
	@Override
	public List<FacilityPerson> getAllActivePeople(Long facilityId) {
		return facilityDao.loadActiveFacilityPeople(facilityId);
	}

	@Override
	public List<FacilityPerson> getAllPeople(Long facilityId) {
		return facilityDao.loadFacilityPeople(facilityId, null);
	}
	
	@Override
	public Collection<FacilityPerson> getProviders(Long facilityId) {
		return facilityDao.getProviders(facilityId);
	}

	@Override
	public List<FacilityPerson> getPrimaryContact(Long facilityId) {
		
		final List<PickListValue> types = new ArrayList<PickListValue>();
		types.add(pickListService.getPickListValueByValue(PRIMARY_CONTACT_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME));
		
		final List<FacilityPerson> results = facilityDao.loadFacilityPeople(facilityId, types);
		return results;
	}

	@Override
	public void saveDirector(FacilityPerson director, boolean first) {
		
		// if the facility person record is new make sure they have been
		// screened and have director credentials
		if (director.getId() == null) {
			
			final Facility facility = director.getFacility();
			final Long facilityId = facility.getId();
			final Long personId = director.getPerson().getId();
			final TrackingRecordScreeningApprovalsView screenedPerson = facilityDao.loadScreenedPersonForFacility(facilityId, personId);
			
			final boolean hasNotBeenScreened = (screenedPerson == null);
			if (hasNotBeenScreened) {
				throw new IllegalArgumentException("The person selected has not had a background screening for this facility.");
			}
		}

		final String directorType = (first ? FIRST_DIRECTOR_PERSON_TYPE_NAME : SECOND_DIRECTOR_PERSON_TYPE_NAME);
		final PickListValue directorPickListType = pickListService.getPickListValueByValue(directorType, PERSON_TYPE_PICK_LIST_NAME);
		director.setType(directorPickListType);
		facilityPersonDao.save(director);
	}

	@Override
	public void saveFacilityPerson(FacilityPerson contact) {
		facilityPersonDao.save(contact);
	}

	@Override
	public List<FacilityPerson> getOwners(Long facilityId) {
		List<PickListValue> types = new ArrayList<PickListValue>();
		types.add(pickListService.getPickListValueByValue(PRIMARY_OWNER_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME));
		types.add(
				pickListService.getPickListValueByValue(SECONDARY_OWNER_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME));
		return facilityDao.loadFacilityPeople(facilityId, types);
	}

	@Override
	public void saveOwner(FacilityPerson owner, boolean primary) {
		if (primary) {
			owner.setType(pickListService.getPickListValueByValue(PRIMARY_OWNER_PERSON_TYPE_NAME,
					PERSON_TYPE_PICK_LIST_NAME));
		} else {
			owner.setType(pickListService.getPickListValueByValue(SECONDARY_OWNER_PERSON_TYPE_NAME,
					PERSON_TYPE_PICK_LIST_NAME));
		}

		facilityPersonDao.save(owner);
	}

	@Override
	public List<FacilityPerson> getBoardMembers(Long facilityId) {
		List<PickListValue> types = new ArrayList<PickListValue>();
		types.add(pickListService.getPickListValueByValue(BOARD_MEMBER_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME));
		return facilityDao.loadFacilityPeople(facilityId, types);
	}

	@Override
	public void saveBoardMember(FacilityPerson boardMember) {
		boardMember.setType(
				pickListService.getPickListValueByValue(BOARD_MEMBER_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME));
		facilityPersonDao.save(boardMember);
	}

	@Override
	public void saveDocument(FacilityAttachment attachment) {
		facilityAttachmentDao.save(attachment);
	}

	@Override
	public void deleteDocument(Long id) {
		facilityAttachmentDao.delete(id);
	}

	@Override
	public void saveBoardMemberAttachment(FacilityAttachment attachment) {
		facilityAttachmentDao.save(attachment);
	}

	@Override
	public void deleteBoardMemberAttachment(Long id) {
		facilityAttachmentDao.delete(id);
	}
	
	@Override
	public FacilityAttachment getFacilityAttachment(Long id) {
		return facilityAttachmentDao.load(id);
	}

	@Override
	public FacilityAttachment loadBoardMemberAttachment(Long id) {
		return getFacilityAttachment(id);
	}

	@Override
	public List<FacilityAttachment> getFacilityAttachments(Long facilityId) {
		return facilityAttachmentDao.getFacilityAttachments(facilityId);
	}

	@Override
	public List<FacilityAttachment> getFacilityAttachments(Long facilityId, String attachmentType) {
		return facilityAttachmentDao.getFacilityAttachments(facilityId, attachmentType);
	}

	@Override
	public PickListValue getFirstDirectorPersonType() {
		return pickListService.getPickListValueByValue(FIRST_DIRECTOR_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME);
	}

	@Override
	public PickListValue getPrimaryOwnerPersonType() {
		return pickListService.getPickListValueByValue(PRIMARY_OWNER_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME);
	}

	@Override
	public PickListValue getSecondDirectorPersonType() {
		return pickListService.getPickListValueByValue(SECOND_DIRECTOR_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME);
	}

	@Override
	public PickListValue getSecondaryOwnerPersonType() {
		return pickListService.getPickListValueByValue(SECONDARY_OWNER_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME);
	}

	@Override
	public PickListValue getBoardMemberPersonType() {
		return pickListService.getPickListValueByValue(BOARD_MEMBER_PERSON_TYPE_NAME, PERSON_TYPE_PICK_LIST_NAME);
	}

	@Override
	public Facility deletePerson(Facility facility, Long personId) {
		facility.removePerson(personId);
		return facilityDao.save(facility);
	}

	@Override
	public Set<BasicFacilityInformation> getCaseload(Person licensingSpecialist) {
		final List<BasicFacilityInformation> facilities = facilityDao.getCaseload(licensingSpecialist);
		final Set<BasicFacilityInformation> caseload = new HashSet<>(facilities);
		return caseload;
	}

	@Override
	public List<BasicFacilityView> getLicensorCaseload(Person licensingSpecialist) {
		return facilityDao.getLicensorCaseload(licensingSpecialist);
	}

	@Override
	public List<FacilityCaseloadView> getUserCaseload(Long specialistId, RoleType roleType, SortBy sortBy) {
		return facilityDao.getUserCaseload(specialistId, roleType, sortBy);
	}

	@Override
	public List<UserCaseloadCount> getUserCaseloadCounts() {
		return facilityDao.getUserCaseloadCounts();
	}

	@Override
	public void transferFacilities(Person fromLs, Person toLs, List<Facility> facilities) {
		if (toLs != null && facilities != null) {
			for (Facility f : facilities) {
				if (toLs != null) {
					setLicensingSpecialist(f, toLs);
				}

				facilityDao.save(f);
			}
		}
	}

	@Override
	public int assignCbsTech(Person fromLs, Person cbsTech, List<Facility> facilities) {
		int updates = 0;
		if (fromLs != null && cbsTech != null && facilities != null) {
			for (Facility f : facilities) {
				if (f.getCbsTechnician() == null || !cbsTech.equals(f.getCbsTechnician())) {
					f.setCbsTechnician(cbsTech);
					facilityDao.save(f);
					updates++;
				}
			}
		}
		return updates;
	}

	@Override
	public List<FacilitySearchView> searchFacilities(FacilitySearchCriteria criteria, SortBy sortBy, int page, int resultsPerPage) {
		return facilityDao.searchFacilities(criteria, sortBy, page, resultsPerPage);
	}
	
	@Override
	public Set<FacilityLookupView> lookupFacilityEmail(FacilityLookupCriteria criteria) {
		if (criteria.getStatus() == null) {
			throw new IllegalArgumentException("FacilityStatus is required but was null");
		}
		final List<FacilityLookupView> facilities = facilityDao.lookupFacilityEmail(criteria);
		return new HashSet<FacilityLookupView>(facilities);
	}
	
	@Override
	public int searchFacilitiesCount(FacilitySearchCriteria criteria) {
		return facilityDao.searchFacilitiesCount(criteria);
	}

	@Override
	public FacilityResultView getFacilityResultView(Long facilityId) {
		return facilityDao.getFacilityResultView(facilityId);
	}

	@Override
	public List<FacilityResultView> searchFacilitiesByName(String name, Long excludeFacilityId) {
		return facilityDao.searchFacilitiesByName(name, excludeFacilityId);
	}

	@Override
	public List<FacilityEventView> getFacilityHistory(Long facilityId, ListRange listRange, SortBy sortBy,
			List<FacilityEventType> eventTypes) {
		return facilityDao.getFacilityHistory(facilityId, listRange, sortBy, eventTypes);
	}

	@Override
	public List<FileCheckView> getFileCheck(Long facilityId, ListRange listRange) {
		return facilityDao.getFileCheck(facilityId, listRange);
	}

	@Override
	public boolean isCertificateType(PickListValue licenseType) {
		return applicationService.propertyContainsPickListValue(licenseType,
				ApplicationPropertyKey.CERTIFICATE_TYPES.getKey());
	}

	@Override
	public boolean isPeopleOwnershipType(PickListValue ownershipType) {
		return applicationService.propertyContainsPickListValue(ownershipType,
				ApplicationPropertyKey.PEOPLE_FACILITY_OWNERSHIP_TYPES.getKey());
	}

	@Override
	public boolean requiresDirector(PickListValue licenseType) {
		return applicationService.propertyContainsPickListValue(licenseType,
				ApplicationPropertyKey.DIRECTOR_REQUIRED_TYPES.getKey());
	}

	@Override
	public boolean licenseRequiresUnderAgeTwo(PickListValue licenseType) {
		return applicationService.propertyContainsPickListValue(licenseType,
				ApplicationPropertyKey.UNDER_AGE_TWO_REQUIRED.getKey());
	}

	@Override
	public List<ExpiredAndExpiringLicenseView> getExpiredAndExpiringLicenses(Long personId, boolean showWholeRegion,
			SortBy sortBy) {
		return facilityDao.getExpiredAndExpiringLicenses(personId, showWholeRegion, sortBy);
	}

	@Override
	public List<NewApplicationPendingDeadlineView> getNewApplicationPendingDeadlines(Long personId,
			boolean showWholeRegion, SortBy sortBy) {
		return facilityDao.getNewApplicationPendingDeadlines(personId, showWholeRegion, sortBy);
	}

	@Override
	public List<AccreditationExpiringView> getExpiringAccreditations(Long personId, boolean showWholeRegion,
			SortBy sortBy) {
		return facilityDao.getExpiringAccreditations(personId, showWholeRegion, sortBy);
	}

	@Override
	public List<ConditionalFacilityView> getFacilitiesOnConditionalLicenses(Long personId, boolean showWholeRegion,
			SortBy sortBy) {
		return facilityDao.getFacilitiesOnConditionalLicenses(personId, showWholeRegion, sortBy);
	}

	@Override
	public List<AnnouncedInspectionNeededView> getAnnouncedInspectionsNeeded(Long personId, boolean showWholeRegion,
			SortBy sortBy) {
		return facilityDao.getAnnouncedInspectionsNeeded(personId, showWholeRegion, sortBy);
	}

	@Override
	public List<UnannouncedInspectionNeededView> getUnannouncedInspectionsNeeded(Long personId, boolean showWholeRegion,
			SortBy sortBy) {
		return facilityDao.getUnannouncedInspectionsNeeded(personId, showWholeRegion, sortBy);
	}

	@Override
	public List<AlertFollowUpsNeededView> getFollowUpInspectionsNeeded(Long personId, String role, SortBy sortBy) {
		Set<Long> recipientIds = new HashSet<Long>();
		recipientIds.add(personId);
		return facilityDao.getFollowUpInspectionsNeeded(recipientIds, role, sortBy, true);
	}

	@Override
	public List<WorkInProgressView> getWorkInProgress(Long personId, SortBy sortBy) {
		return facilityDao.getWorkInProgress(personId, sortBy);
	}

	@Override
	public List<OutstandingCmpView> getOutstandingCmps(Long personId, boolean showWholeRegion) {
		return facilityDao.getOutstandingCmps(personId, showWholeRegion);
	}

	@Override
	public List<ExemptVerificationView> getExemptVerifications(SortBy sortBy) {
		return facilityDao.getExemptVerifications(sortBy);
	}

	@Override
	public List<Complaint> getComplaintsToBeFinalized(Long personId, boolean showWholeRegion) {
		return null;
	}

	@Override
	public List<FacilityContactView> getContactFacilities(Long personId, SortBy sortBy) {
		return facilityDao.getContactFacilities(personId, sortBy);
	}

	@Override
	public List<FacilityChecksReceivedView> getFacilityChecksReceived(Date receivedDate) {
		return facilityDao.getFacilityChecksReceived(receivedDate);
	}

	@Scheduled(cron = "0 0 1 * * ?")
	public void deactivateFacilitiesWithExpiredExemptions() {
		ApplicationProperty exeExp = applicationService.findApplicationPropertyByKey(
				ApplicationPropertyKey.FACILITY_DEACTIVATION_REASON_EXEMPTIONS_EXPIRED.getKey());
		PickListValue exeExpReason = (PickListValue) pickListService.loadPickListValueById(new Long(exeExp.getValue()));

		for (Facility f : facilityDao.getFacilitiesToDeactivate()) {
			logger.info("Deactivating facility " + f.getId() + " - " + f.getName());
			f.setStatus(FacilityStatus.INACTIVE);
			f = facilityDao.save(f);

			ActionLog expLog = new ActionLog();
			expLog.setFacility(f);
			expLog.setActionDate(new Date());
			expLog.setActionType(exeExpReason);

			actionLogService.saveActionLog(expLog, null);
		}
	}

	@Scheduled(cron = "0 15 1 * * ?")
	public void deactivateScheduledFacilities() {
		logger.debug("Running scheduled facility deactivations job");
		sharedTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<PickListValue> deactivationReasons = pickListService
						.getValuesForPickList("Facility Deactivation Reasons", true);
				List<FacilityTag> facilityTags = facilityDao.getDeactivationFacilityTags(deactivationReasons);
				for (FacilityTag ft : facilityTags) {
					try {
						deactivateFacility(ft.getFacility(), ft.getTag(), ft.getStartDate());
					} catch (Exception e) {
						logger.error("Unable to deactivate facility (" + ft.getFacility().getId() + "):", e);
					}
				}
			}
		});
	}

	@Scheduled(cron = "0 0 22 * * MON-FRI")
	public void sendERepFile() throws Exception {
		logger.debug("Running eRep export");
		String host = applicationService.getApplicationPropertyValue(ApplicationPropertyKey.EREP_HOST.getKey());
		String remotePath = applicationService
				.getApplicationPropertyValue(ApplicationPropertyKey.EREP_REMOTE_PATH.getKey());
		String filename = applicationService.getApplicationPropertyValue(ApplicationPropertyKey.EREP_FILENAME.getKey());
		String username = applicationService.getApplicationPropertyValue(ApplicationPropertyKey.EREP_USERNAME.getKey());
		String password = applicationService.getApplicationPropertyValue(ApplicationPropertyKey.EREP_PASSWORD.getKey());
		String port = applicationService.getApplicationPropertyValue(ApplicationPropertyKey.EREP_PORT.getKey());

		if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(remotePath) && StringUtils.isNotBlank(filename)
				&& StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
			logger.debug("Creating file " + filename);
			File file = new File(filename);

			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			SimpleDateFormat expDateFormatter = new SimpleDateFormat("MM/dd/yyyy");
			SimpleDateFormat modDateFormatter = new SimpleDateFormat("ddMMMyy");
			writer.append(
					"\"Internal Id\",\"Type of Facility\",\"Facility Name\",\"Phone\",\"Address\",\"City\",\"State\",\"Zip Code\",\"Total # Adults\",\"Total # Youth\",\"Expiration Date\",\"Licensor\",\"Status\",\"Date of Modification\"\n");
			List<ErepView> erepViews = facilityDao.getErepViews();
			for (ErepView view : erepViews) {
				if (!("Active".equals(view.getStatus()) && StringUtils.isBlank(view.getLicenseType()))) {
					writer.append(view.getId().toString());
					addCsvField(writer, view.getLicenseType() == null ? "FX" : view.getLicenseType());
					addCsvField(writer, view.getFacilityName());
					if (view.getPrimaryPhone() != null && view.getPrimaryPhone().length() == 10) {
						writer.append(CSV_FIELD_START);
						writer.append(view.getPrimaryPhone().substring(0, 3));
						writer.append('-');
						writer.append(view.getPrimaryPhone().substring(3, 6));
						writer.append('-');
						writer.append(view.getPrimaryPhone().substring(6, 10));
						writer.append(CSV_FIELD_END);
					} else {
						addCsvField(writer, view.getPrimaryPhone());
					}
					writer.append(CSV_FIELD_START);
					writer.append(view.getAddressOne() == null ? "" : view.getAddressOne());
					if (!StringUtils.isEmpty(view.getAddressTwo())) {
						writer.append(", ");
						writer.append(view.getAddressTwo());
					}
					writer.append(CSV_FIELD_END);
					addCsvField(writer, view.getCity());
					addCsvField(writer, view.getState());
					addCsvField(writer, view.getZipCode() != null && view.getZipCode().length() > 5
							? view.getZipCode().substring(0, 5) : view.getZipCode());
					writer.append(",");
					writer.append(view.getAdultTotalSlots() == null ? "0" : view.getAdultTotalSlots().toString());
					writer.append(",");
					writer.append(view.getYouthTotalSlots() == null ? "0" : view.getYouthTotalSlots().toString());
					addCsvField(writer,
							view.getExpirationDate() == null ? "" : expDateFormatter.format(view.getExpirationDate()));
					writer.append(CSV_FIELD_START);
					writer.append(StringUtils.isBlank(view.getLicensorFirstName()) ? "" : view.getLicensorFirstName());
					writer.append(StringUtils.isBlank(view.getLicensorLastName()) ? "" : " ");
					writer.append(StringUtils.isBlank(view.getLicensorLastName()) ? "" : view.getLicensorLastName());
					writer.append(CSV_FIELD_END);
					addCsvField(writer, view.getStatus());
					addCsvField(writer, view.getModifiedDate() == null ? ""
							: modDateFormatter.format(view.getModifiedDate()).toUpperCase());
					writer.append("\n");
				}
			}
			writer.flush();
			writer.close();

			logger.debug("File created.  Ready to send to eRep.");
			ScpUtil.scpTo(host, StringUtils.isBlank(port) ? -1 : Integer.parseInt(port), username, password, file,
					remotePath);
		} else {
			logger.debug("Unable to send file.  Required properties were not provided.");
		}
	}

	private void addCsvField(Writer writer, String field) throws IOException {
		writer.append(",\"");
		if (!StringUtils.isBlank(field)) {
			writer.append(field.replaceAll("\"", "\"\"")); // escape quotes with
															// double quotes.
		}
		writer.append("\"");
	}

	@Override
	public void evict(final Object entity) {
		facilityDao.evict(entity);
	}

	@Override
	public FacilityContactView loadContactFacilityById(Long id) {
		return facilityDao.loadContactFacilityById(id);
	}

	@Override
	public List<SortableFacilityView> getOpenApplicationsBySpecialist(Long specialistId) {
		return facilityDao.getOpenApplicationsBySpecialist(specialistId);
	}

	@Override
	public List<License> getOpenLicenseApplicationsBySpecialist(List<Long> specialistIds) {
		if (specialistIds == null || specialistIds.size() == 0) {
			return new ArrayList<License>();
		}
		return facilityDao.getOpenLicenseApplicationsBySpecialist(specialistIds);
	}
	
	@Override
	public List<License> getExpiringLicensesBySpecialist(List<Long> specialistIds) {
		if (specialistIds == null || specialistIds.size() == 0) {
			return new ArrayList<License>();
		}
		return facilityDao.getExpiringLicensesBySpecialist(specialistIds);
	}
	
	@Override
	public List<Long> getTeamForSupervisor(Long supervisorUserId) {
		return facilityDao.getTeamForSupervisor(supervisorUserId);
	}
	
	@Override
	public List<Long> getPrimaryLicensorIds(List<Long> specialistIds) {
		return facilityDao.getPrimaryLicensorIds(specialistIds);
	}

	@Override
	public List<FacilityLicenseView> getFacilityLicenseSummary(Long specialistId, Date expDate, SortBy sortBy) {
		return facilityDao.getFacilityLicenseSummary(specialistId, expDate, sortBy);
	}

	@Override
	public List<FacilityLicenseView> getFacilityLicenseDetail(Long specialistId, Date expDate) {
		return facilityDao.getFacilityLicenseDetail(specialistId, expDate);
	}

	@Override
	public List<FacilityLicenseView> getExpiringLicensesBySpecialist(Date expDate, Long specialistId) {
		return facilityDao.getExpiringLicensesBySpecialist(expDate, specialistId);
	}

	@Override
	public List<FacilityLicenseView> getRenewalLicensesBySpecialist(Date expDate, Long specialistId) {
		return facilityDao.getRenewalLicensesBySpecialist(expDate, specialistId);
	}

	@Override
	public List<FacilityLicenseView> getFosterCareRenewalLicensesBySpecialist(Date expDate, Long specialistId) {
		return facilityDao.getFosterCareRenewalLicensesBySpecialist(expDate, specialistId);
	}

	public List<KeyValuePair> findAllPreviousLicenseNumbers() {
		return facilityDao.findAllPreviousLicenseNumbers();
	}
	
	@Override
	public TagMap getParent(Long childId) {
		final TagMap parent = tagMapDao.getNewestByRelatedItemIdAndTagName(childId, "PARENT");
		return parent;
	}

	@Override
	public void setParent(Long parentId, Long childId) {
		final Tag parentTag = tagMapDao.getTag("PARENT");
		final TagMap newParent = new TagMap();
		newParent.setItemId(parentId);
		newParent.setRelatedItemId(childId);
		newParent.setTag(parentTag);
		tagMapDao.save(newParent);
	}

	@Override
	public TagMap getAdmin(Long childId) {
		final TagMap admin = tagMapDao.getNewestByRelatedItemIdAndTagName(childId, "ADMIN");
		return admin;
	}

	@Override
	public void setOwner(Long ownerId, Long childId) {
		try {
			final Tag ownerTag = tagMapDao.getTag("ADMIN");
			final TagMap ownerTagMap = new TagMap();
			ownerTagMap.setItemId(ownerId);
			ownerTagMap.setRelatedItemId(childId);
			ownerTagMap.setTag(ownerTag);
			tagMapDao.save(ownerTagMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void removeParent(Long facilityId) {
		try {
			final TagMap parentTagMap = getParent(facilityId);
			parentTagMap.setExpirationDate(new Date());
			tagMapDao.save(parentTagMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void removeAdmin(Long facilityId) {
		try {
			final TagMap ownerTagMap = getAdmin(facilityId);
			ownerTagMap.setExpirationDate(new Date());
			tagMapDao.save(ownerTagMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<OpenReviewView> getAllOpenQaReviews() {
		return facilityDao.getAllOpenQaReviews();
	}

	@Override
	public List<OpenReviewView> getOpenQaReviews(List<Long> specialistIds) {
		
		if (specialistIds == null) {
			throw new IllegalArgumentException("personId cannot be null, but was");
		}
		
		return facilityDao.getOpenQaReviews(specialistIds);
	}
	
	@Override
	public List<License> getOverlappingLicenses(License license) {
		if (license == null) {
			throw new IllegalArgumentException("license is required, but was null");
		}
		else if (license.getFacility() == null) {
			throw new IllegalArgumentException("license facility is required, but was null");
		}
		else if (license.getLicenseType() == null) {
			throw new IllegalArgumentException("license type is required, but was null");
		}
		else if (license.getStatus() == null) {
			throw new IllegalArgumentException("license status is required, but was null");
		}
		else if (license.getExpirationDate() == null) {
			throw new IllegalArgumentException("license expiration date is required, but was null");
		}
		else if (license.getStartDate() == null) {
			throw new IllegalArgumentException("license start date is required, but was null");
		}
		return facilityDao.getOverlappingLicenses(license);
	}
	
	@Override
	public List<License> getFacilityLicenses(Facility facility) {
		if (facility == null) {
			throw new IllegalArgumentException("facility is required, but was null");
		}
		final Long facilityId = facility.getId();
		return facilityDao.getFacilityLicenses(facilityId);
	}
	
	@Override
	public List<Facility> getDecendants(Long facilityId) {
		if (facilityId == null) {
			throw new IllegalArgumentException("facilityId is required, but was null");
		}
		return facilityDao.getDecendants(facilityId);
	}
}
