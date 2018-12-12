/**
 * 
 */
package gov.utah.dts.det.ccl.dao;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import gov.utah.dts.det.admin.model.PickListValue;
import gov.utah.dts.det.ccl.model.Facility;
import gov.utah.dts.det.ccl.model.FacilityPerson;
import gov.utah.dts.det.ccl.model.FacilityTag;
import gov.utah.dts.det.ccl.model.License;
import gov.utah.dts.det.ccl.model.Person;
import gov.utah.dts.det.ccl.model.Review;
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
import gov.utah.dts.det.ccl.service.FacilityLookupCriteria;
import gov.utah.dts.det.ccl.service.FacilitySearchCriteria;
import gov.utah.dts.det.ccl.service.UserCaseloadCount;
import gov.utah.dts.det.ccl.view.FacilityResultView;
import gov.utah.dts.det.ccl.view.KeyValuePair;
import gov.utah.dts.det.ccl.view.ListRange;
import gov.utah.dts.det.dao.AbstractBaseDao;
import gov.utah.dts.det.query.SortBy;

/**
 * @author DOLSEN
 * 
 */
public interface FacilityDao extends AbstractBaseDao<Facility, Long> {

	public Facility getFacility(Long id);
	
	public Facility loadFacilityByIdNumber(String idNumber);

	public License loadLicenseById(Long id);

	public List<FacilityPerson> loadActiveFacilityPeople(Long facilityId);
	
	public List<FacilityPerson> loadFacilityPeople(Long facilityId, List<PickListValue> peopleTypes);

	public FacilityPerson loadFacilityPerson(Long facilityId, Long facilityPersonId, Long firstType, Long secondType);

	public FacilityPerson loadFacilityPerson(Long facilityPersonId);
	
	public Collection<FacilityPerson> getProviders(Long facilityId);

	public FacilityPerson loadFacilityBoardMember(Long facilityId, Long facilityPersonId, Long facilityPersonType);

	public List<TrackingRecordScreeningApprovalsView> loadScreenedPeopleForFacility(Long facilityId,
			boolean excludeDirectors, boolean excludeContacts);

	public TrackingRecordScreeningApprovalsView loadScreenedPersonForFacility(Long facilityId, Long personId);

	public List<FacilitySearchView> searchFacilities(FacilitySearchCriteria criteria, SortBy sortBy, int page, int resultsPerPage);
	
	public List<FacilityLookupView> lookupFacilityEmail(FacilityLookupCriteria criteria);
	
	public int searchFacilitiesCount(FacilitySearchCriteria criteria);

	public FacilityResultView getFacilityResultView(Long facilityId);

	public List<FacilityResultView> searchFacilitiesByName(String name, Long excludeFacilityId);

	public List<FacilityEventView> getFacilityHistory(Long facilityId, ListRange listRange, SortBy sortBy,
			List<FacilityEventType> eventTypes);

	public List<FileCheckView> getFileCheck(Long facilityId, ListRange listRange);

	public Facility loadFacilityWithSearchViewById(Long facilityId);

	public List<BasicFacilityInformation> getCaseload(Person licensingSpecialist);

	public List<BasicFacilityView> getLicensorCaseload(Person licensingSpecialist);

	public List<FacilityCaseloadView> getUserCaseload(Long specialistId, RoleType roleType, SortBy sortBy);

	public List<UserCaseloadCount> getUserCaseloadCounts();

	public List<ExpiredAndExpiringLicenseView> getExpiredAndExpiringLicenses(Long personId, boolean showWholeRegion,
			SortBy sortBy);

	public List<NewApplicationPendingDeadlineView> getNewApplicationPendingDeadlines(Long personId,
			boolean showWholeRegion, SortBy sortBy);

	public List<AccreditationExpiringView> getExpiringAccreditations(Long officeSpecialistId, boolean showWholeRegion,
			SortBy sortBy);

	public List<ConditionalFacilityView> getFacilitiesOnConditionalLicenses(Long personId, boolean showWholeRegion,
			SortBy sortBy);

	public List<AnnouncedInspectionNeededView> getAnnouncedInspectionsNeeded(Long personId, boolean showWholeRegion,
			SortBy sortBy);

	public List<UnannouncedInspectionNeededView> getUnannouncedInspectionsNeeded(Long personId, boolean showWholeRegion,
			SortBy sortBy);

	public List<AlertFollowUpsNeededView> getFollowUpInspectionsNeeded(Set<Long> recipientIds, String role,
			SortBy sortBy, boolean fetchFacility);

	public List<WorkInProgressView> getWorkInProgress(Long personId, SortBy sortBy);
	
	public List<OpenReviewView> getAllOpenQaReviews();

	public List<OpenReviewView> getOpenQaReviews(List<Long> specialistIds);

	public List<OutstandingCmpView> getOutstandingCmps(Long personId, boolean showWholeRegion);

	public List<ExemptVerificationView> getExemptVerifications(SortBy sortBy);

	public List<FacilityTag> getDeactivationFacilityTags(List<PickListValue> deactivationReasons);

	public List<Facility> getFacilitiesToDeactivate();

	public List<ErepView> getErepViews();

	public List<FacilityContactView> getContactFacilities(Long personId, SortBy sortBy);

	public FacilityContactView loadContactFacilityById(Long id);

	public List<SortableFacilityView> getOpenApplicationsBySpecialist(Long specialistId);

	public List<License> getOpenLicenseApplicationsBySpecialist(List<Long> specialistIds);

	public List<License> getExpiringLicensesBySpecialist(List<Long> specialistIds);
	
	public List<Long> getTeamForSupervisor(Long supervisorUserId);
	
	public List<Long> getPrimaryLicensorIds(List<Long> specialistIds);

	public List<FacilityLicenseView> getFacilityLicenseSummary(Long specialistId, Date expDate, SortBy sortBy);

	public List<FacilityLicenseView> getFacilityLicenseDetail(Long specialistId, Date expDate);

	public List<FacilityLicenseView> getExpiringLicensesBySpecialist(Date expDate, Long specialistId);

	public List<FacilityLicenseView> getRenewalLicensesBySpecialist(Date expDate, Long specialistId);

	public List<FacilityLicenseView> getFosterCareRenewalLicensesBySpecialist(Date expDate, Long specialistId);

	public List<FacilityChecksReceivedView> getFacilityChecksReceived(Date receivedDate);

	public List<KeyValuePair> findAllPreviousLicenseNumbers();
	
	public List<License> getOverlappingLicenses(License license);
	
	public List<License> getFacilityLicenses(Long facilityId);
	
	public List<Facility> getDecendants(Long facilityId);
	
}