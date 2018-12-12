package gov.utah.dts.det.ccl.dao.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import gov.utah.dts.det.admin.model.PickListValue;
import gov.utah.dts.det.admin.service.PickListService;
import gov.utah.dts.det.ccl.dao.FacilityDao;
import gov.utah.dts.det.ccl.dao.SearchException;
import gov.utah.dts.det.ccl.model.Facility;
import gov.utah.dts.det.ccl.model.FacilityPerson;
import gov.utah.dts.det.ccl.model.FacilityTag;
import gov.utah.dts.det.ccl.model.License;
import gov.utah.dts.det.ccl.model.Person;
import gov.utah.dts.det.ccl.model.Review;
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
import gov.utah.dts.det.ccl.service.FacilityLookupCriteria;
import gov.utah.dts.det.ccl.service.FacilitySearchCriteria;
import gov.utah.dts.det.ccl.service.UserCaseloadCount;
import gov.utah.dts.det.ccl.service.util.ServiceUtils;
import gov.utah.dts.det.ccl.view.FacilityResultView;
import gov.utah.dts.det.ccl.view.KeyValuePair;
import gov.utah.dts.det.ccl.view.ListRange;
import gov.utah.dts.det.ccl.view.facility.FacilityStatus;
import gov.utah.dts.det.dao.AbstractBaseDaoImpl;
import gov.utah.dts.det.query.SortBy;
import gov.utah.dts.det.service.ApplicationService;

@SuppressWarnings("unchecked")
@Repository("facilityDao")
public class FacilityDaoImpl extends AbstractBaseDaoImpl<Facility, Long> implements FacilityDao {

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private PickListService pickListService;
	
	private static final Logger log = LoggerFactory.getLogger(FacilityDaoImpl.class);

	@PersistenceContext
	private EntityManager em;

	public FacilityDaoImpl() {
		super(Facility.class);
	}

	@Override
	public EntityManager getEntityManager() {
		return em;
	}
	
	@Override
	public Facility getFacility(Long id) {
		final Facility facility = (Facility) em.createQuery("from Facility where id = :id")
				.setParameter("id", id)
				.setMaxResults(1)
				.getSingleResult();
		return facility;
	}

	@Override
	public Facility loadFacilityByIdNumber(String idNumber) {
		
		final Query query = em.createQuery("from Facility f where f.idNumber = :idNumber")
				.setParameter("idNumber", idNumber);

		try {
			
			final Facility facility = (Facility) query
					.setMaxResults(1)
					.getSingleResult();
			return facility;
			
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public License loadLicenseById(Long id) {
		
		final Query query = em.createQuery("from License l where l.id = :id")
				.setParameter("id", id);

		try {
			final License license = (License) query
					.setMaxResults(1)
					.getSingleResult();
			
			return license;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<FacilityPerson> loadActiveFacilityPeople(Long facilityId) {
		final StringBuilder sb = new StringBuilder("from FacilityPerson fp left join fetch fp.person p where fp.facility.id = :facilityId and p.deleted != :true order by fp.person.name.firstName, fp.person.name.lastName ");
		final List<FacilityPerson> people = (List<FacilityPerson>) em.createQuery(sb.toString())
				.setParameter("facilityId", facilityId)
				.setParameter("true", Boolean.TRUE)
				.getResultList();
		return people;
	}

	@Override
	public List<FacilityPerson> loadFacilityPeople(Long facilityId, List<PickListValue> peopleTypes) {
		
		final StringBuilder sb = new StringBuilder("from FacilityPerson fp left join fetch fp.person left join fetch fp.person.address where fp.facility.id = :facilityId ");
		
		if (peopleTypes != null && !peopleTypes.isEmpty()) {
			sb.append(" and fp.type in (:peopleTypes) ");
		}
		sb.append(" order by fp.person.name.firstName, fp.person.name.lastName ");

		final Query query = em.createQuery(sb.toString())
				.setParameter("facilityId", facilityId);
		
		if (peopleTypes != null && !peopleTypes.isEmpty()) {
			query.setParameter("peopleTypes", peopleTypes);
		}

		return (List<FacilityPerson>) query.getResultList();
	}
	
	@Override
	public Collection<FacilityPerson> getProviders(Long facilityId) {

		final PickListValue providerRole = pickListService.getPickListValueByValue("Provider", "Foster Care Person Roles");
		final PickListValue spouseRole = pickListService.getPickListValueByValue("Spouse", "Foster Care Person Roles");
		
		final StringBuilder sb = new StringBuilder("from FacilityPerson fp left join fetch fp.person p where fp.facility.id = :facilityId and p.deleted != :true and (fp.role = :provider or fp.role = :spouse) order by fp.person.name.firstName, fp.person.name.lastName ");
		final List<FacilityPerson> providers = (List<FacilityPerson>) em.createQuery(sb.toString())
				.setParameter("facilityId", facilityId)
				.setParameter("true", Boolean.TRUE)
				.setParameter("provider", providerRole)
				.setParameter("spouse", spouseRole)
				.getResultList();
		return providers;
	}

	@Override
	public FacilityPerson loadFacilityPerson(Long facilityId, Long facilityPersonId, Long firstType, Long secondType) {
		
		final String queryStr = "from FacilityPerson fp left join fetch fp.person left join fetch fp.person.address where fp.facility.id = :facilityId and fp.id = :facilityPersonId and (fp.type.id = :firstType or fp.type.id = :secondType) ";
		
		try {
			final FacilityPerson person = (FacilityPerson) em.createQuery(queryStr)
					.setParameter("facilityId", facilityId)
					.setParameter("facilityPersonId", facilityPersonId)
					.setParameter("firstType", firstType)
					.setParameter("secondType", secondType)
					.setMaxResults(1)
					.getSingleResult();

			return person;
		} catch (Exception e) {
			return null;
		}
	}
	
	@Override
	public FacilityPerson loadFacilityPerson(Long facilityPersonId) {
		
		final Query query = em.createQuery("from FacilityPerson fp left join fetch fp.person left join fetch fp.person.address where fp.id = :facilityPersonId")
				.setParameter("facilityPersonId", facilityPersonId);
		
		try {
			final FacilityPerson person = (FacilityPerson) query
					.setMaxResults(1)
					.getSingleResult();
			
			return person;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public FacilityPerson loadFacilityBoardMember(Long facilityId, Long facilityPersonId, Long facilityPersonType) {
		
		final String hql = "from FacilityPerson fp left join fetch fp.person left join fetch fp.person.address where fp.facility.id = :facilityId and fp.id = :facilityPersonId and fp.type.id = :facilityPersonType ";
		
		final Query query = em.createQuery(hql)
				.setParameter("facilityId", facilityId)
				.setParameter("facilityPersonId", facilityPersonId)
				.setParameter("facilityPersonType", facilityPersonType);

		try {
			final FacilityPerson singleResult = (FacilityPerson) query
					.setMaxResults(1)
					.getSingleResult();
			
			return singleResult;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<TrackingRecordScreeningApprovalsView> loadScreenedPeopleForFacility(Long facilityId, boolean excludeDirectors, boolean excludeContacts) {
		
		final String SCREENED_PEOPLE_QUERY = "select distinct sav from TrackingRecordScreeningApprovalsView sav "
				+ "left join fetch sav.person p left join fetch p.address a left join fetch sav.facility f "
				+ "where (sav.facilityId = :facilityId or "
				+ "sav.facilityId in (select distinct fa.parent.id from FacilityAssociation fa where fa.child.id = :facilityId) or "
				+ "sav.facilityId in (select distinct fa.child.id from FacilityAssociation fa where fa.parent.id = :facilityId)) ";
		final StringBuilder sb = new StringBuilder(SCREENED_PEOPLE_QUERY);
		if (excludeDirectors) {
			final String EXCLUDE_DIRECTORS_CLAUSE = " and sav.personId not in "
					+ "(select distinct fp.personId from FacilityPerson fp where fp.type.value in ('First Director','Second Director') and "
					+ "(fp.facilityId = :facilityId or "
					+ "(fp.facilityId not in (select distinct fa.parent.id from FacilityAssociation fa where fa.child.id = :facilityId) and "
					+ "fp.facilityId not in (select distinct fa.child.id from FacilityAssociation fa where fa.parent.id = :facilityId)))) ";
			sb.append(EXCLUDE_DIRECTORS_CLAUSE);
		}
		if (excludeContacts) {
			final String EXCLUDE_CONTACTS_CLAUSE = " and sav.personId not in "
					+ "(select distinct fp.personId from FacilityPerson fp where fp.type.value in ('Primary Contact','Secondary Contact') and "
					+ "(fp.facilityId = :facilityId or "
					+ "(fp.facilityId not in (select distinct fa.parent.id from FacilityAssociation fa where fa.child.id = :facilityId) and "
					+ "fp.facilityId not in (select distinct fa.child.id from FacilityAssociation fa where fa.parent.id = :facilityId)))) ";
			sb.append(EXCLUDE_CONTACTS_CLAUSE);
		}
		
		final Query query = em.createQuery(sb.toString());
		query.setParameter("facilityId", facilityId);

		return (List<TrackingRecordScreeningApprovalsView>) query.getResultList();
	}

	@Override
	public TrackingRecordScreeningApprovalsView loadScreenedPersonForFacility(Long facilityId, Long personId) {
		
		final StringBuilder hql = new StringBuilder("select distinct sav from TrackingRecordScreeningApprovalsView sav ")
				.append("left join fetch sav.person p left join fetch p.address a left join fetch sav.facility f ")
				.append("where (sav.facilityId = :facilityId or ")
				.append("sav.facilityId in (select distinct fa.parent.id from FacilityAssociation fa where fa.child.id = :facilityId) or ")
				.append("sav.facilityId in (select distinct fa.child.id from FacilityAssociation fa where fa.parent.id = :facilityId)) ")
				.append(" and sav.personId = :personId ");

		final Query query = em.createQuery(hql.toString())
				.setParameter("facilityId", facilityId)
				.setParameter("personId", personId);

		try {
			return (TrackingRecordScreeningApprovalsView) query
				.setMaxResults(1)
				.getSingleResult();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<FacilitySearchView> searchFacilities(FacilitySearchCriteria criteria, SortBy sortBy, int page, int resultsPerPage) {
		
		final StringBuilder sb = new StringBuilder("select fsv from FacilitySearchView fsv where fsv.id in (select distinct f.id from Facility f ");
		
		final boolean isCountQuery = false;
		buildSearchQueryString(sb, criteria, sortBy, isCountQuery);

		int maxResults = resultsPerPage == 0 ? 25 : resultsPerPage;
		int firstResult = page * resultsPerPage;
		
		final Query query = buildSearchQuery(sb.toString(), criteria, isCountQuery)
				.setFirstResult(firstResult)
				.setMaxResults(maxResults);
		
		final List<FacilitySearchView> facilities = query.getResultList();
		for (FacilitySearchView f : facilities) {
			f.getLicenses().size();
			System.out.println(f.getId());
		}

		return facilities;
	}
	
	@Override
	public List<FacilityLookupView> lookupFacilityEmail(FacilityLookupCriteria criteria) {
		
		final boolean requireType = criteria.getType() != null;
		final boolean requireActiveLicenses = criteria.isActiveLicenses() == true;
		final boolean requireLicensorSpecialist = criteria.getLicensorSpecialistId() > 0;
		final boolean requireExpirationDate = criteria.getLicenseExpirationDate() != null;
		final boolean requireLicenseType = criteria.getLicenseType() != null;
		final boolean requireServiceTypeIds = criteria.getServiceTypeIds() != null && criteria.getServiceTypeIds().size() > 0;
		final boolean requireSpecificServiceCodeIds = criteria.getSpecificServiceCodeIds() != null && criteria.getSpecificServiceCodeIds().size() > 0;
		final boolean requireRegionIds = criteria.getRegionIds() != null && criteria.getRegionIds().size() > 0;
		
		final StringBuilder ql = new StringBuilder()
				.append("select distinct f ")
				.append(" from FacilityLookupView f ")
				.append(" where f.facilityStatus = :status ")
				.append(requireType ? " and f.facilityType = :type " : "")
				.append(requireActiveLicenses ? " and f.licenseStatusId = :activeLicenseStatusId " : "")
				.append(requireLicensorSpecialist ? " and f.licensingSpecialistId = :licensingSpecialistId " : "")
				.append(requireExpirationDate ? " and trunc(f.licenseExpiration) = trunc(:licenseExpirationDate) " : "")
				.append(requireLicenseType ? " and f.licenseTypeId = :licenseType " : "")
				.append(criteria.isRequireCbsEmail() ? " and f.cbsEmail is not null " : "")
				.append(requireSpecificServiceCodeIds ? " and f.specificServiceCodeId in (:specificServiceCodeIds) " : "")
				.append(requireRegionIds ? " and f.regionId in (:regionIds) " : "");
		
		if (requireServiceTypeIds) {
			ql.append(" and (");
			for (int i = 0; i < criteria.getServiceTypeIds().size(); i++) {
				ql.append(i > 0 ? " or " : "").append(" f.serviceTypeIds like :serviceTypeId").append(i).append(" ");
			}
			ql.append(")");
		}
		
		ql.append(" order by f.facilityName asc");
		
		final Query query = em.createQuery(ql.toString())
				.setParameter("status", criteria.getStatus().name());
		
		if (requireType) {
			query.setParameter("type", criteria.getType().getCharacterAsString());
		}
		if (requireActiveLicenses) {
			final PickListValue activeLicenseStatus = pickListService.getPickListValueByValue("Active", "License Status");
			query.setParameter("activeLicenseStatusId", activeLicenseStatus.getId());
		}
		if (requireLicensorSpecialist) {
			query.setParameter("licensingSpecialistId", criteria.getLicensorSpecialistId());
		}
		if (requireExpirationDate) {
			query.setParameter("licenseExpirationDate", criteria.getLicenseExpirationDate());
		}
		if (requireLicenseType) {
			query.setParameter("licenseType", criteria.getLicenseType().getId());
		}
		if (requireSpecificServiceCodeIds) {
			query.setParameter("specificServiceCodeIds", criteria.getSpecificServiceCodeIds());
		}
		if (requireRegionIds) {
			query.setParameter("regionIds", criteria.getRegionIds());
		}
		if (requireServiceTypeIds) {
			for (int i = 0; i < criteria.getServiceTypeIds().size(); i++) {
				final String serviceTypeId = "%"+criteria.getServiceTypeIds().get(i)+"%"; 
				query.setParameter("serviceTypeId"+i, serviceTypeId);
			}
			
		}
				
		final List<FacilityLookupView> facilities = query.getResultList();
		return facilities;
	}
	
	@Override
	public int searchFacilitiesCount(FacilitySearchCriteria criteria) {
		
		final StringBuilder sb = new StringBuilder("select count(fac.id) from Facility fac where fac.id in (select distinct f.id from Facility f ");
		
		final boolean isCountQuery = true;
		buildSearchQueryString(sb, criteria, null, isCountQuery);
		
		final Query query = buildSearchQuery(sb.toString(), criteria, isCountQuery);

		final Long results = (Long) query.getSingleResult();
		return results.intValue();
	}

	public String getReportCodes(Long licenseTypeId, String columnName) {
		
		final String sql = "select " + columnName + " from report_code_lkup where license_type = " + licenseTypeId;
		final Query query = em.createNativeQuery(sql);
	  
		try {
			return (String) query
				.setMaxResults(1)
				.getSingleResult();
		} catch (Exception e) {
			return null;
		}
	}

	private void buildSearchQueryString(StringBuilder queryString, FacilitySearchCriteria criteria, SortBy sortBy, boolean isCountQuery) {
		boolean criteriaSelected = false;
		if (StringUtils.isNotBlank(criteria.getCounty()) || StringUtils.isNotBlank(criteria.getCity()) || StringUtils.isNotBlank(criteria.getZipCode())) {
			queryString.append(", Address ma");
			if (StringUtils.isNotBlank(criteria.getCounty())) {
				queryString.append(", Location loc");
			}
		}
		if (StringUtils.isNotBlank(criteria.getLicenseeName()) ||
			(criteria.getLicenseSubTypeIds() != null && criteria.getLicenseSubTypeIds().size() > 0) ||
			(criteria.getLicenseStatusIds() != null && criteria.getLicenseStatusIds().size() > 0) ||
			(criteria.getLicenseTypeIds() != null && criteria.getLicenseTypeIds().size() > 0) ||
			criteria.getLicenseExpRangeStart() != null ||
			criteria.getLicenseExpRangeEnd() != null
		) {
			queryString.append(", License lic");
		}
		if (criteria.getExemptionIds() != null && criteria.getExemptionIds().size() > 0) {
			queryString.append(", Exemption ex");
		}
		if (criteria.getInactiveReasonIds() != null && criteria.getInactiveReasonIds().size() > 0) {
			queryString.append(", ActionLog al");
		}
		
		queryString.append(" where f.id = f.id");

		// Add facility name filter
		final String facilityNameText = criteria.getFacilityName();
		if (StringUtils.isNotBlank(facilityNameText)) {
			final String[] facilityNameValues = facilityNameText.split(" ");
			for (int i=0; i < facilityNameValues.length; i++) {
				queryString.append(" and upper(trim(f.name)) like upper(trim(:facilityName").append(i).append("))");
			}
			criteriaSelected = true;
		}
		
		// Add site name filter
		if (StringUtils.isNotBlank(criteria.getSiteName())) {
			queryString.append(" and upper(trim(f.siteName)) like upper(trim(:siteName))");
			criteriaSelected = true;			
		}

		// Add Primary Phone filter
		if (StringUtils.isNotBlank(criteria.getPrimaryPhone())) {
			queryString.append(" and f.primaryPhone.phoneNumber = :primaryPhone");
			criteriaSelected = true;
		}

		// Add Facility Owner filter
		if (StringUtils.isNotBlank(criteria.getOwnerName())) {
			queryString.append(" and upper(f.ownerName) like upper(:ownerName)");
			criteriaSelected = true;
		}

		// Add Region filter
		if (criteria.getRegionId() != null) {
			queryString.append(" and f.region.id = :regionId");
			criteriaSelected = true;
		}
		
		// Add County, City, and ZipCode filters
		if (StringUtils.isNotBlank(criteria.getCounty()) || StringUtils.isNotBlank(criteria.getCity()) || StringUtils.isNotBlank(criteria.getZipCode())) {
			queryString.append(" and ma.id = f.mailingAddress.id");
			if (StringUtils.isNotBlank(criteria.getCounty())) {
				//  Get the proper location for address city, state and zipcode
				queryString.append(" and upper(loc.city) = upper(ma.city) and upper(loc.state) = upper(ma.state) and loc.zipCode = ma.zipCode");
				queryString.append(" and upper(loc.county) like upper(:county)");
				criteriaSelected = true;
			}
	
			if (StringUtils.isNotBlank(criteria.getCity())) {
				queryString.append(" and upper(ma.city) like upper(:city)");
				criteriaSelected = true;
			}
	
			if (StringUtils.isNotBlank(criteria.getZipCode())) {
				queryString.append(" and ma.zipCode like :zipCode");
				criteriaSelected = true;
			}
		}

		// Add Facility Type filter
		if (criteria.getFacilityType() != null) {
			queryString.append(" and f.type = :facilityType");
			criteriaSelected = true;
		}
		
		// Add Facility Status filter
		if (criteria.getFacilityStatus() != null) {
			queryString.append(" and f.status = :facilityStatus");
			criteriaSelected = true;
		}

		if (StringUtils.isNotBlank(criteria.getLicenseeName()) ||
			(criteria.getLicenseSubTypeIds() != null && criteria.getLicenseSubTypeIds().size() > 0) ||
			(criteria.getLicenseStatusIds() != null && criteria.getLicenseStatusIds().size() > 0) ||
			(criteria.getLicenseTypeIds() != null && criteria.getLicenseTypeIds().size() > 0) ||
			criteria.getLicenseExpRangeStart() != null ||
			criteria.getLicenseExpRangeEnd() != null
		) {
			queryString.append(" and lic.facility.id = f.id");
		}

		// Add Licensor filter
		if (StringUtils.isNotBlank(criteria.getLicenseeName())) {
			queryString.append(" and upper(lic.licenseHolderName) like upper(:licenseeName) ");
			criteriaSelected = true;
		}

		// Add License Term Type(s) filter
		if (criteria.getLicenseSubTypeIds() != null && criteria.getLicenseSubTypeIds().size() > 0) {
			queryString.append(" and lic.subtype.id in (:licenseSubTypeIds)");
			criteriaSelected = true;
		}

		// Add License status(es) filter
		if (criteria.getLicenseStatusIds() != null && criteria.getLicenseStatusIds().size() > 0) {
			queryString.append(" and lic.status.id in (:licenseStatusIds)");
			criteriaSelected = true;
		}

		// Add License licenseType(s) filter
		if (criteria.getLicenseTypeIds() != null && criteria.getLicenseTypeIds().size() > 0) {
			queryString.append(" and lic.licenseType.id in (:licenseTypeIds)");
			criteriaSelected = true;
		}

		// Add current license date range filter(s)
		if (criteria.getLicenseExpRangeStart() != null && criteria.getLicenseExpRangeEnd() != null) {
			queryString.append(" and (trunc(lic.expirationDate, 'DD') between trunc(:licenseExpRangeStart) and trunc(:licenseExpRangeEnd))");
			criteriaSelected = true;
		} else if (criteria.getLicenseExpRangeStart() != null) {
			queryString.append(" and trunc(lic.expirationDate, 'DD') >= :licenseExpRangeStart");
			criteriaSelected = true;
		} else if (criteria.getLicenseExpRangeEnd() != null) {
			queryString.append(" and trunc(lic.expirationDate, 'DD') <= :licenseExpRangeEnd");
			criteriaSelected = true;
		}

		// Process Is Conditional Facility TAG filter
		if (criteria.isConditional()) 
		{
			queryString.append(" and facilityTag.facility.id = f.id and facilityTag.tag.value = 'Conditional'");
			criteriaSelected = true;
		}


		// Add facility licensing specialist filter
		if (criteria.getLicensingSpecialistId() != null) {
			queryString.append(" and f.licensingSpecialist.id = :licensingSpecialistId");
			criteriaSelected = true;
		}

		// Add exemption reasons filter
		if (criteria.getExemptionIds() != null && criteria.getExemptionIds().size() > 0) {
			queryString.append(" and ex.facility.id = f.id and trunc(current_date, 'DD') between ex.startDate and ex.expirationDate");
			queryString.append(" and ex.exemption.id in (:exemptionIds)");
			criteriaSelected = true;
		}

		// Add inactive reasons filter
		if (criteria.getInactiveReasonIds() != null && criteria.getInactiveReasonIds().size() > 0) {
			queryString.append(" and al.facility.id = f.id and al.actionType.id in (:inactiveReasonIds)");
			criteriaSelected = true;
		}

		queryString.append(")");

		// Add facility is parent filter
		final boolean isNotCountQuery = !isCountQuery;
		if (isNotCountQuery && criteria.isParentFacilityOnly()) {
			queryString.append(" and (fsv.isAdministrator = :isParent or fsv.isOwner = :isParent)");
			criteriaSelected = true;
		}

		if (!criteriaSelected) {
			throw new SearchException("No criteria selected for search.");
		}

		ServiceUtils.addSortByClause(queryString, sortBy, null);
	}

	private Query buildSearchQuery(String queryString, FacilitySearchCriteria criteria, boolean isCountQuery) {
		
		final Query query = em.createQuery(queryString);
		
		String facilityNameText = criteria.getFacilityName();
		if (StringUtils.isNotBlank(facilityNameText)) {
			final String[] facilityNameValues = facilityNameText.split(" ");
			for (int i=0; i < facilityNameValues.length; i++) {
				String value = facilityNameValues[i] + "%";
				if (criteria.getNameSearchType() == FacilitySearchCriteria.NameSearchType.ANY_PART) {
					value = "%" + value;
				}
				query.setParameter("facilityName"+i, value);
			}
		}
		
		if (StringUtils.isNotBlank(criteria.getSiteName())) {
			String siteName = criteria.getSiteName().toUpperCase();
			if (criteria.getSiteNameSearchType() == null || criteria.getSiteNameSearchType() == FacilitySearchCriteria.NameSearchType.STARTS_WITH) {
				siteName = siteName + "%";
			} else {
				//Mean's FacilitySearchCriteria.NameSearchType.ANY_PART
				siteName = "%" + siteName + "%";
			}
			query.setParameter("siteName", siteName);
		}
		
		if (StringUtils.isNotBlank(criteria.getPrimaryPhone())) {
			query.setParameter("primaryPhone", criteria.getPrimaryPhone());
		}
		
		if (StringUtils.isNotBlank(criteria.getOwnerName())) {
			query.setParameter("ownerName",  "%"+criteria.getOwnerName()+"%");
		}
		
		if (criteria.getRegionId() != null) {
			query.setParameter("regionId", criteria.getRegionId());
		}
		if (StringUtils.isNotBlank(criteria.getCounty())) {
			query.setParameter("county", criteria.getCounty()+"%");
		}
		
		if (StringUtils.isNotBlank(criteria.getCity())) {
			query.setParameter("city", criteria.getCity()+"%");
		}
		
		if (StringUtils.isNotBlank(criteria.getZipCode())) {
			query.setParameter("zipCode", criteria.getZipCode()+"%");
		}
		
		if (criteria.getFacilityType() != null) {
			query.setParameter("facilityType", criteria.getFacilityType());
		}
		
		if (criteria.getFacilityStatus() != null) {
			query.setParameter("facilityStatus", criteria.getFacilityStatus());
		}
		
		if (StringUtils.isNotBlank(criteria.getLicenseeName())) {
			query.setParameter("licenseeName", "%"+criteria.getLicenseeName()+"%");
		}
		
		if (criteria.getLicenseSubTypeIds() != null && criteria.getLicenseSubTypeIds().size() > 0) {
			query.setParameter("licenseSubTypeIds", criteria.getLicenseSubTypeIds());
		}
		
		if (criteria.getLicenseStatusIds() != null && criteria.getLicenseStatusIds().size() > 0) {
			query.setParameter("licenseStatusIds", criteria.getLicenseStatusIds());
		}
		
		if (criteria.getLicenseTypeIds() != null && criteria.getLicenseTypeIds().size() > 0) {
			query.setParameter("licenseTypeIds", criteria.getLicenseTypeIds());
		}
		
		if (criteria.getLicenseExpRangeStart() != null) {
			query.setParameter("licenseExpRangeStart", criteria.getLicenseExpRangeStart());
		}
		
		if (criteria.getLicenseExpRangeEnd() != null) {
			query.setParameter("licenseExpRangeEnd", criteria.getLicenseExpRangeEnd());
		}
		
		if (criteria.getLicensingSpecialistId() != null) {
			query.setParameter("licensingSpecialistId", criteria.getLicensingSpecialistId());
		}
		
		if (criteria.getExemptionIds() != null && criteria.getExemptionIds().size() > 0) {
			query.setParameter("exemptionIds", criteria.getExemptionIds());
		}
		
		if (criteria.getInactiveReasonIds() != null && criteria.getInactiveReasonIds().size() > 0) {
			query.setParameter("inactiveReasonIds", criteria.getInactiveReasonIds());
		}

		final boolean isNotCountQuery = !isCountQuery;
		if (isNotCountQuery && criteria.isParentFacilityOnly()) {
			query.setParameter("isParent", Boolean.TRUE);
		}
		
		return query;
	}

	@Override
	public FacilityResultView getFacilityResultView(Long facilityId) {
		
		final String hql = new StringBuilder("select new gov.utah.dts.det.ccl.view.FacilityResultView(f.id, f.name, ")
				.append(" f.idNumber, f.primaryPhone, cbs.addressOne, cbs.addressTwo, cbs.city, cbs.state, cbs.zipCode, ma.addressOne, ma.addressTwo, ")
				.append(" ma.city, ma.state, ma.zipCode) ")
				.append(" from Facility f left join f.cbsAddress cbs left join f.mailingAddress ma ")
				.append(" where f.id = :facilityId ")
				.toString();
		
		final Query query = em.createQuery(hql);
		query.setParameter("facilityId", facilityId);
		
		try {
			final FacilityResultView frv = (FacilityResultView) query
					.setMaxResults(1)
					.getSingleResult();
			return frv;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<FacilityResultView> searchFacilitiesByName(String name, Long excludeFacilityId) {
		
		final StringBuilder hql = new StringBuilder("select new gov.utah.dts.det.ccl.view.FacilityResultView(f.id, initcap(f.name), ")
				.append(" f.idNumber, f.primaryPhone, cbs.addressOne, cbs.addressTwo, cbs.city, cbs.state, cbs.zipCode, ma.addressOne, ma.addressTwo, ")
				.append(" ma.city, ma.state, ma.zipCode) ")
				.append(" from Facility f left join f.cbsAddress cbs left join f.mailingAddress ma ")
				.append(" where f.status IN ('REGULATED','IN_PROCESS') and f.cbsTechnician != null and upper(f.name) like upper(:facilityName) ");
		if (excludeFacilityId != null) {
			hql.append(" and f.id != :excludeFacilityId ");
		}
		hql.append(" order by initcap(f.name) asc");

		final Query query = em.createQuery(hql.toString())
				.setParameter("facilityName", name.toUpperCase() + "%");
		
		if (excludeFacilityId != null) {
			query.setParameter("excludeFacilityId", excludeFacilityId);
		}
		query.setMaxResults(20);
		List<FacilityResultView> frvs = query.getResultList();
		return frvs;
	}

	@Override
	public List<FacilityEventView> getFacilityHistory(Long facilityId, ListRange listRange, SortBy sortBy, List<FacilityEventType> eventTypes) {
		
		final StringBuilder sb = new StringBuilder("from FacilityEventView fev where fev.facilityId = :facilityId ");
		ServiceUtils.addIntervalClause("fev.eventDate", sb, listRange);
		if (eventTypes != null && eventTypes.size() > 0) {
			sb.append(" and fev.primaryKey.eventType in (:eventTypes) ");
		}
		ServiceUtils.addSortByClause(sb, sortBy, null);

		final Query query = em.createQuery(sb.toString())
				.setParameter("facilityId", facilityId);
		
		if (eventTypes != null && eventTypes.size() > 0) {
			query.setParameter("eventTypes", eventTypes);
		}

		return (List<FacilityEventView>) query.getResultList();
	}

	@Override
	public List<FileCheckView> getFileCheck(Long facilityId, ListRange listRange) {
		
		final StringBuilder sb = new StringBuilder("from FileCheckView fcv where fcv.facilityId = :facilityId ");
		ServiceUtils.addIntervalClause("fcv.inspectionDate", sb, listRange);
		sb.append(" order by fcv.inspectionDate desc ");

		final Query query = em.createQuery(sb.toString())
				.setParameter("facilityId", facilityId);

		return (List<FileCheckView>) query.getResultList();
	}

	@Override
	public Facility loadFacilityWithSearchViewById(Long facilityId) {
		
		final Query query = em.createQuery("from Facility f left join fetch f.searchView where f.id = :facilityId ")
				.setParameter("facilityId", facilityId);

		try {
			return (Facility) query
				.setMaxResults(1)
				.getSingleResult();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<BasicFacilityInformation> getCaseload(Person licensingSpecialist) {
		final String hql = "select distinct fac from BasicFacilityInformation fac where fac.licensingSpecialist.id = :specialistId and (fac.status = 'REGULATED' or fac.status = 'IN_PROCESS') ";
		final List<BasicFacilityInformation> facilities = em.createQuery(hql)
				.setParameter("specialistId", licensingSpecialist.getId())
				.getResultList();
		return facilities;
	}

    @Override
    public List<BasicFacilityView> getLicensorCaseload(Person licensingSpecialist) {
        final Query query = em.createQuery("from BasicFacilityView fac left join fetch fac.cbsTechnician where fac.licensingSpecialist.id = :specialistId and (fac.status = 'REGULATED' or fac.status = 'IN_PROCESS') ")
        		.setParameter("specialistId", licensingSpecialist.getId());
        return (List<BasicFacilityView>) query.getResultList();
    }

	@Override
	public List<FacilityCaseloadView> getUserCaseload(Long specialistId, RoleType roleType, SortBy sortBy) {
		
		if (roleType == null || (roleType != RoleType.ROLE_LICENSOR_SPECIALIST)) {
			throw new IllegalArgumentException("Can only pull caseloads for licensing specialists");
		}
		
		final StringBuilder sb = new StringBuilder("from FacilityCaseloadView fcv left join fetch fcv.licensingSpecialist ls where fcv.");
		if (roleType == RoleType.ROLE_LICENSOR_SPECIALIST) {
			sb.append("licensing");
		}
		sb.append("Specialist.id = :specialistId ");
		ServiceUtils.addSortByClause(sb, sortBy, null);

		final Query query = em.createQuery(sb.toString())
				.setParameter("specialistId", specialistId);
		
		return (List<FacilityCaseloadView>) query.getResultList();
	}

	@Override
	public List<UserCaseloadCount> getUserCaseloadCounts() {
		
		final Session session = (Session) em.getDelegate();
		
		final String queryStr = "select pers.id as id, pers.firstname || ' ' || pers.lastname as name, ur.role_name as roleType,"
				+ "     su.active as active, count(distinct fac.id) as count"
				+ " from security_user su inner join person pers on su.personid = pers.id "
				+ "     inner join user_role ur on su.id = ur.user_id and (ur.role_name = 'ROLE_LICENSOR_SPECIALIST') "
				+ "     left outer join ( "
				+ "         select fac.id, fls.specialist_id, 'ROLE_LICENSOR_SPECIALIST' as role_name "
				+ "         from facility fac, facility_licensing_specialist fls "
				+ "         where fac.id = fls.facility_id and (fac.status = 'IN_PROCESS' or fac.status = 'REGULATED') "
				+ "     ) fac on pers.id = fac.specialist_id and ur.role_name = fac.role_name"
				+ " group by pers.id, pers.firstname, pers.lastname, ur.role_name, su.active order by pers.firstname, pers.lastname ";
		
		final SQLQuery query = session.createSQLQuery(queryStr)
				.addScalar("id", StandardBasicTypes.LONG)
				.addScalar("name", StandardBasicTypes.STRING)
				.addScalar("roleType", StandardBasicTypes.STRING)
				.addScalar("active", StandardBasicTypes.YES_NO)
				.addScalar("count", StandardBasicTypes.LONG);
		query.setResultTransformer(Transformers.aliasToBean(UserCaseloadCount.class));
		
		return (List<UserCaseloadCount>) query.list();
	}

	@Override
	public List<ExpiredAndExpiringLicenseView> getExpiredAndExpiringLicenses(Long personId, boolean showWholeRegion, SortBy sortBy) {
		
		final StringBuilder sb = new StringBuilder("select eelv from ExpiredAndExpiringLicenseView eelv left join fetch eelv.applicationReceivedAction left join fetch eelv.facility fac ");
		appendPersonClause(sb, showWholeRegion);
		ServiceUtils.addSortByClause(sb, sortBy, null);

		final List<ExpiredAndExpiringLicenseView> results = (List<ExpiredAndExpiringLicenseView>)  
				em.createQuery(sb.toString())
				.setParameter("personId", personId)
				.getResultList();

		return results;
	}

	@Override
	public List<NewApplicationPendingDeadlineView> getNewApplicationPendingDeadlines(Long personId, boolean showWholeRegion, SortBy sortBy) {
		
		final StringBuilder sb = new StringBuilder("select napdv from NewApplicationPendingDeadlineView napdv left join fetch napdv.facility fac ");
		appendPersonClause(sb, showWholeRegion);
		ServiceUtils.addSortByClause(sb, sortBy, null);

		final Query query = em.createQuery(sb.toString())
				.setParameter("personId", personId);

		return (List<NewApplicationPendingDeadlineView>) query.getResultList();
	}

	@Override
	public List<AccreditationExpiringView> getExpiringAccreditations(Long personId, boolean showWholeRegion, SortBy sortBy) {
		
		final StringBuilder sb = new StringBuilder("select aev from AccreditationExpiringView aev left join fetch aev.facility fac ");
		appendPersonClause(sb, showWholeRegion);
		ServiceUtils.addSortByClause(sb, sortBy, null);

		final Query query = em.createQuery(sb.toString())
				.setParameter("personId", personId);

		return (List<AccreditationExpiringView>) query.getResultList();
	}

	@Override
	public List<ConditionalFacilityView> getFacilitiesOnConditionalLicenses(Long personId, boolean showWholeRegion, SortBy sortBy) {
		
		final StringBuilder sb = new StringBuilder("select cfv from ConditionalFacilityView cfv left join fetch cfv.facility fac ");
		appendPersonClause(sb, showWholeRegion);
		ServiceUtils.addSortByClause(sb, sortBy, null);

		final Query query = em.createQuery(sb.toString())
				.setParameter("personId", personId);

		return (List<ConditionalFacilityView>) query.getResultList();
	}

	@Override
	public List<AnnouncedInspectionNeededView> getAnnouncedInspectionsNeeded(Long personId, boolean showWholeRegion, SortBy sortBy) {
		
		final StringBuilder sb = new StringBuilder("select ainv from AnnouncedInspectionNeededView ainv left join fetch ainv.facility fac ");
		appendPersonClause(sb, showWholeRegion);
		ServiceUtils.addSortByClause(sb, sortBy, null);

		final Query query = em.createQuery(sb.toString())
				.setParameter("personId", personId);

		return (List<AnnouncedInspectionNeededView>) query.getResultList();
	}

	@Override
	public List<UnannouncedInspectionNeededView> getUnannouncedInspectionsNeeded(Long personId, boolean showWholeRegion, SortBy sortBy) {
		
		final StringBuilder sb = new StringBuilder("select uinv from UnannouncedInspectionNeededView uinv left join fetch uinv.facility fac ");
		appendPersonClause(sb, showWholeRegion);
		ServiceUtils.addSortByClause(sb, sortBy, null);

		final Query query = em.createQuery(sb.toString())
				.setParameter("personId", personId);

		return (List<UnannouncedInspectionNeededView>) query.getResultList();
	}

	@Override
	public List<AlertFollowUpsNeededView> getFollowUpInspectionsNeeded(Set<Long> recipientIds, String role, SortBy sortBy, boolean fetchFacility) {
		
		final StringBuilder sb = new StringBuilder();
		if ("COMPL".equals(role)) {
			sb.append("select view from ComplaintFollowUpsNeededView view left join fetch view.facility fac left join fetch view.findings ");
		} else if ("NORM".equals(role)) {
			sb.append("select view from FollowUpsNeededView view left join fetch view.facility fac left join fetch view.findings ");
		}
		sb.append(" where view.recipient.id in (:recipientIds) ");
		ServiceUtils.addSortByClause(sb, sortBy, null);

		final Query query = em.createQuery(sb.toString());
		query.setParameter("recipientIds", recipientIds);

		final Set<AlertFollowUpsNeededView> followUps = new LinkedHashSet<AlertFollowUpsNeededView>((List<AlertFollowUpsNeededView>) query.getResultList());
		final List<AlertFollowUpsNeededView> followUpsList = new ArrayList<AlertFollowUpsNeededView>(followUps);
		return followUpsList;
	}

	@Override
	public List<WorkInProgressView> getWorkInProgress(Long personId, SortBy sortBy) {
		
		final StringBuilder sb = new StringBuilder("select wipv from WorkInProgressView wipv left join fetch wipv.facility fac where wipv.owner.id = :personId");
		ServiceUtils.addSortByClause(sb, sortBy, null);

		final Query query = em.createQuery(sb.toString())
				.setParameter("personId", personId);

		return (List<WorkInProgressView>) query.getResultList();
	}

	@Override
	public List<OutstandingCmpView> getOutstandingCmps(Long personId, boolean showWholeRegion) {
		
		final StringBuilder sb = new StringBuilder("select ocv from OutstandingCmpView ocv left join fetch ocv.facility fac left join fetch fac.licensingSpecialist ");
		appendPersonClause(sb, showWholeRegion);

		final Query query = em.createQuery(sb.toString())
				.setParameter("personId", personId);

		return (List<OutstandingCmpView>) query.getResultList();
	}

	@Override
	public List<ExemptVerificationView> getExemptVerifications(SortBy sortBy) {
		StringBuilder sb = new StringBuilder("select evv from ExemptVerificationView evv left join fetch evv.facility fac ");
		ServiceUtils.addSortByClause(sb, sortBy, null);

		Query query = em.createQuery(sb.toString());

		return (List<ExemptVerificationView>) query.getResultList();
	}

	@SuppressWarnings("unused")
	private void appendFacilityFetch(StringBuilder sb, boolean fetchFacility) {
		if (fetchFacility) {
			sb.append(" left join fetch view.facility fac ");
		}
	}

	private void appendPersonClause(StringBuilder sb, boolean showWholeRegion) {
		if (showWholeRegion) {
			sb.append(" where fac.region.officeSpecialist.id = :personId ");
		} else {
			sb.append(" where fac.licensingSpecialist.id = :personId ");
		}
	}

	@Override
	public List<FacilityContactView> getContactFacilities(Long personId, SortBy sortBy) {
		
		final StringBuilder sb = new StringBuilder(personId != null ? "from FacilityContactView fcv where fcv.personId = :personId " : "from FacilityContactView fcv  ");
		ServiceUtils.addSortByClause(sb, sortBy, null);
		
		final Query query = em.createQuery(sb.toString());
		if (personId != null) {
			query.setParameter("personId", personId);
		}
		return (List<FacilityContactView>) query.getResultList();
	}

	public FacilityContactView loadContactFacilityById(Long id) {
		final StringBuilder sb = new StringBuilder("from FacilityContactView fcv where fcv.id = :id ");
		final Query query = em.createQuery(sb.toString());
		if (id != null) {
			query.setParameter("id", id);
		}
		try {
			return (FacilityContactView) query
					.setMaxResults(1)
					.getSingleResult();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<FacilityTag> getDeactivationFacilityTags(List<PickListValue> deactivationReasons) {
		final Query query = em.createQuery("from FacilityTag ft where ft.tag in (:deactivationReasons) and trunc(ft.startDate, 'dd') <= trunc(current_date(), 'dd') ")
				.setParameter("deactivationReasons", deactivationReasons);
		return (List<FacilityTag>) query.getResultList();
	}

	@Override
	public List<Facility> getFacilitiesToDeactivate() {
		final String hql = "from Facility f where f.status = 'EXEMPT' and id not in ("
				+ " select distinct ex.facility.id from Exemption ex where trunc(current_date(), 'dd') between trunc(ex.startDate, 'dd') and trunc(ex.expirationDate, 'dd')) ";
		final Query query = em.createQuery(hql);
		return (List<Facility>) query.getResultList();
	}

	@Override
	public List<ErepView> getErepViews() {
		final Query query = em.createQuery("from ErepView ev order by ev.id ");
		return (List<ErepView>) query.getResultList();
	}
	
	@Override
	public List<SortableFacilityView> getOpenApplicationsBySpecialist(Long specialistId) {

		if (specialistId == null) {
			return new ArrayList<SortableFacilityView>();
		}
		
		final String hql = new StringBuilder("from SortableFacilityView sfv join fetch sfv.facility f ")
		        .append(" where f.licensingSpecialist.id = :specialistId and f.status = :inProcessStatus ")
		        .append(" order by sfv.facilityName asc")
		        .toString();
		final Query query = em.createQuery(hql)
				.setParameter("specialistId", specialistId)
				.setParameter("inProcessStatus", FacilityStatus.IN_PROCESS);
		return (List<SortableFacilityView>) query.getResultList();
	}
	
	@Override
	public List<License> getOpenLicenseApplicationsBySpecialist(List<Long> specialistIds) {
		
		if (CollectionUtils.isEmpty(specialistIds)) {
			return new ArrayList<License>();
		}
		
		final PickListValue closed = applicationService.getPickListValueForApplicationProperty(ApplicationPropertyKey.CLOSED_LICENSE_STATUS.getKey());
		final PickListValue inactive = applicationService.getPickListValueForApplicationProperty(ApplicationPropertyKey.INACTIVE_LICENSE_STATUS.getKey());
		
		final List<Long> excludedStatuses = new ArrayList<Long>();
		excludedStatuses.add(closed.getId());
		excludedStatuses.add(inactive.getId());
		
		final Boolean finalized = Boolean.TRUE;
		final Query query = em.createQuery("from License l where l.facility.licensingSpecialist.id in (:specialistIds) and l.status.id not in (:excludedStatuses) and l.finalized != :finalized and l.expirationDate > SYSDATE order by l.facility.licensingSpecialist.name.firstName, startDate desc")
			.setParameter("specialistIds", specialistIds)
			.setParameter("excludedStatuses", excludedStatuses)
			.setParameter("finalized", finalized);
		return (List<License>) query.getResultList();
	}
	
	@Override
	public List<Long> getTeamForSupervisor(Long supervisorUserId) {

		if (supervisorUserId == null) {
			return new ArrayList<Long>();
		}
		
		final Query query = em.createQuery("select p.id from Person p where p.user.supervisor.id = :supervisorUserId")
				.setParameter("supervisorUserId", supervisorUserId);
		final List<Long> resultList = (List<Long>) query.getResultList();
		return resultList;
	}
	
	@Override
	public List<Long> getPrimaryLicensorIds(List<Long> specialistIds) {

		if (CollectionUtils.isEmpty(specialistIds)) {
			return new ArrayList<Long>();
		}
		
		final Query query = em.createQuery("select distinct f.licensingSpecialist.id from Facility f where f.licensingSpecialist.id in (:specialistIds)")
				.setParameter("specialistIds", specialistIds);
		final List<Long> resultList = (List<Long>) query.getResultList();
		return resultList;
	}
	
	@Override
	public List<License> getExpiringLicensesBySpecialist(List<Long> specialistIds) {

		if (CollectionUtils.isEmpty(specialistIds)) {
			return new ArrayList<License>();
		}
		
		log.debug("FacilityDaoImpl.getExpiringLicensesBySpecialist(specialistIds) count: " + specialistIds.size());
		
		final PickListValue closed = applicationService.getPickListValueForApplicationProperty(ApplicationPropertyKey.CLOSED_LICENSE_STATUS.getKey());
		
		final Calendar calendar = Calendar.getInstance();
		
		calendar.add(Calendar.DATE, -60);
		final Date startDate = calendar.getTime();
		
		calendar.add(Calendar.DATE, 60); // back to today
		calendar.add(Calendar.MONTH, 2); // "60"-ish days in the future (2 months)
		final int maxDate = calendar.getActualMaximum(Calendar.DATE);
		calendar.set(Calendar.DATE, maxDate); // last day of the month
		calendar.add(Calendar.DATE, 1); // add one day 
		final Date endDate = calendar.getTime();
		
		final Boolean finalized = Boolean.TRUE;
		
		final Query expiringLicenseQuery = em.createQuery("from License l where l.facility.licensingSpecialist.id in (:specialistIds) and closedDate is null and (expirationDate between SYSDATE and :futureDate) and l.status.value != 'Inactive' and l.finalized = :finalized and l.status.id != :closed order by expirationDate asc");
		final List<License> expiringLicenses = (List<License>) expiringLicenseQuery
				.setParameter("specialistIds", specialistIds)
				.setParameter("futureDate", endDate)
				.setParameter("finalized", finalized)
				.setParameter("closed", closed.getId())
				.getResultList();

		final Query expiredLicenseQuery = em.createQuery("from License l where l.facility.licensingSpecialist.id in (:specialistIds) and closedDate is null and (expirationDate between :pastDate and SYSDATE) and l.status.value != 'Inactive' and l.finalized = :finalized and l.status.id != :closed order by expirationDate desc");
		final List<License> expiredLicenses = (List<License>) expiredLicenseQuery
				.setParameter("specialistIds", specialistIds)
				.setParameter("pastDate", startDate)
				.setParameter("finalized", finalized)
				.setParameter("closed", closed.getId())
				.getResultList();
		
		final List<License> expiringAndExpiredLicenses = new ArrayList<License>();
		expiringAndExpiredLicenses.addAll(expiringLicenses);
		expiringAndExpiredLicenses.addAll(expiredLicenses);
		
		return expiringAndExpiredLicenses;
	}
	
	@Override
	public List<FacilityLicenseView> getFacilityLicenseSummary(Long specialistId, Date expDate, SortBy sortBy) {
		
		final StringBuilder sb = new StringBuilder("from FacilityLicenseView flv join fetch flv.facility f where f.licensingSpecialist.id = :specialistId and flv.status = 'Active' ");
		if (expDate != null) {
			sb.append(" and trunc(flv.expirationDate, 'DD') = trunc(:licenseExpEnd) ");
		}
		ServiceUtils.addSortByClause(sb, sortBy, null);
		
		final Query query = em.createQuery(sb.toString())
				.setParameter("specialistId", specialistId);
		
		if (expDate != null) {
			query.setParameter("licenseExpEnd", expDate);
		}
		return (List<FacilityLicenseView>) query.getResultList();
	}

	@Override
	public List<FacilityLicenseView> getFacilityLicenseDetail(Long specialistId, Date expDate) {
		
		final StringBuilder sb = new StringBuilder("from FacilityLicenseView flv join fetch flv.facility f where f.licensingSpecialist.id = :specialistId and flv.status = 'Active' ");
		if (expDate != null) {
			sb.append(" and trunc(flv.expirationDate, 'DD') = trunc(:licenseExpEnd) ");
		}
		sb.append(" order by flv.facilityName asc, flv.expirationDate desc, flv.licenseType");
		
		final Query query = em.createQuery(sb.toString())
				.setParameter("specialistId", specialistId);
		
		if (expDate != null) {
			query.setParameter("licenseExpEnd", expDate);
		}
		return (List<FacilityLicenseView>) query.getResultList();
	}

	@Override
	public List<FacilityLicenseView> getExpiringLicensesBySpecialist(Date expDate, Long specialistId) {
		
		final String hql = new StringBuilder("from FacilityLicenseView flv join fetch flv.facility f ")
				.append(" where f.licensingSpecialist.id = :specialistId and flv.status = 'Active' ")
				.append(" and trunc(flv.expirationDate, 'DD') <= trunc(:licenseExpEnd) ")
				.append(" order by flv.facilityName asc, flv.expirationDate desc, flv.licenseType")
				.toString();
		
		final Query query = em.createQuery(hql)
				.setParameter("specialistId", specialistId)
				.setParameter("licenseExpEnd", expDate);
		
		return (List<FacilityLicenseView>) query.getResultList();
	}

	@Override
	public List<FacilityLicenseView> getRenewalLicensesBySpecialist(Date expDate, Long specialistId) {
		
		final String hql = new StringBuilder("from FacilityLicenseView flv join fetch flv.facility f ")
				.append(" where f.licensingSpecialist.id = :specialistId and flv.status = 'Active' ")
				.append(" and trunc(flv.expirationDate, 'DD') = trunc(:licenseExpEnd) ")
				.append(" order by flv.facilityName asc, flv.expirationDate desc, flv.licenseType")
				.toString();
		
		final Query query = em.createQuery(hql)
				.setParameter("specialistId", specialistId)
				.setParameter("licenseExpEnd", expDate);
		
		return (List<FacilityLicenseView>) query.getResultList();
	}

	@Override
	public List<FacilityLicenseView> getFosterCareRenewalLicensesBySpecialist(Date expDate, Long specialistId) {
		
		final String hql = new StringBuilder("from FacilityLicenseView flv join fetch flv.facility f ")
				.append(" where f.licensingSpecialist.id = :specialistId and f.type in ('F','S') and flv.status = 'Active' ")
				.append(" and trunc(flv.expirationDate, 'DD') = trunc(:licenseExpEnd) ")
				.append(" order by flv.facilityName asc, flv.expirationDate desc, flv.licenseType")
				.toString();
		
		final Query query = em.createQuery(hql)
				.setParameter("specialistId", specialistId)
				.setParameter("licenseExpEnd", expDate);
		
		return (List<FacilityLicenseView>) query.getResultList();
	}
	
	@Override
	public List<FacilityChecksReceivedView> getFacilityChecksReceived(Date receivedDate) {
        final Query query = em.createQuery("from FacilityChecksReceivedView fcrv where fcrv.receivedDate = :receivedDate order by fcrv.name")
        		.setParameter("receivedDate", receivedDate);
        return (List<FacilityChecksReceivedView>) query.getResultList();
	}
	
	public List<KeyValuePair> findAllPreviousLicenseNumbers() {
		final Query query = em.createQuery("select new gov.utah.dts.det.ccl.view.KeyValuePair(l.id, to_char(l.id)) from License l order by l.id");
		return (List<KeyValuePair>) query.getResultList();
	}

	@Override
	public List<OpenReviewView> getAllOpenQaReviews() {
		final List<OpenReviewView> resultList = (List<OpenReviewView>) em.createQuery("select distinct r from OpenReviewView r order by r.startDate desc")
				.getResultList();
		return resultList;
	}

	@Override
	public List<OpenReviewView> getOpenQaReviews(List<Long> specialistIds) {
		
		if (CollectionUtils.isEmpty(specialistIds)) {
			return new ArrayList<OpenReviewView>();
		}
		
		final StringBuilder hql = new StringBuilder("select distinct r from OpenReviewView r where r.specialistId in (:specialistIds) or r.assignedToId in (:specialistIds) ");
		
		int i = 0;
		for (Long id : specialistIds) {
			hql.append(" or r.secondaryAssignedToIds like :specialistId").append(i++);
		}
		
		hql.append(" order by r.startDate desc");
		
		final Query query = em.createQuery(hql.toString())
				.setParameter("specialistIds", specialistIds);
		
		i = 0;
		for (Long id : specialistIds) {
			query.setParameter("specialistId"+i++, "%"+id+"%");
		}
		
		final List<OpenReviewView> resultList = (List<OpenReviewView>) query.getResultList();
		return resultList;
	}
	
	@Override
	public List<License> getOverlappingLicenses(License license) {
		
//		final StringBuilder b = new StringBuilder("from License where facility.id = :facilityId and licenseType.id = :licenseTypeId and status.id = :statusId and (:startDate between startDate and expirationDate or :endDate between startDate and expirationDate or startDate between :startDate and :endDate or expirationDate between :startDate and :endDate)");
//		if (license.getId() != null) {
//			b.append(" and id != :licenseId");
//		}
//		final String hql = b.toString();
//		
//		final Query query = em.createQuery(hql);
//		final List<License> overlappingLicenses = (List<License>) query
//				.setParameter("facilityId", license.getFacility().getId())
//				.setParameter("licenseTypeId", license.getLicenseType().getId())
//				.setParameter("statusId", license.getStatus().getId())
//				.setParameter("startDate", license.getStartDate())
//				.setParameter("endDate", license.getEndDate())
//				.setParameter("licenseId", license.getId())
//				.getResultList();
//		return overlappingLicenses;
		return new ArrayList<License>();
	}
	
	@Override
	public List<License> getFacilityLicenses(Long facilityId) {
		
		if (facilityId == null) {
			return new ArrayList<License>();
		}
		
		final String hql = "from License where facility.id = :facilityId order by licenseType.value ASC, licenseNumber DESC, startDate DESC";
		final Query query = em.createQuery(hql.toString());
		final List<License> licenses = (List<License>) query
				.setParameter("facilityId", facilityId)
				.getResultList();
		return licenses;
	}
	
	@Override 
	public List<Facility> getDecendants(Long facilityId) {
		
		if (facilityId == null) {
			return new ArrayList<Facility>();
		}
		
		final String sql = "SELECT DISTINCT RELATED_ITEM_ID FROM TAGMAP WHERE RELATED_ITEM_ID != :facilityId AND EXPIRATION_DATE IS NULL AND CONNECT_BY_ISCYCLE = 0START WITH ITEM_ID = :facilityId CONNECT BY NOCYCLE ITEM_ID = PRIOR RELATED_ITEM_ID ORDER BY RELATED_ITEM_ID";
		final Query query = em.createNativeQuery(sql);
		final List<Object> decendantIds = (List<Object>) query
				.setParameter("facilityId", facilityId)
				.getResultList();
		
		if (CollectionUtils.isEmpty(decendantIds)) {
			return new ArrayList<Facility>();
		}
		
		final List<Long> ids = new ArrayList<Long>();
		for (Object id : decendantIds) {
			try {
				ids.add(Long.parseLong(id.toString()));
			}
			catch (NumberFormatException e) {
				continue;
			}
		}
		
		final String decendantHql = "from Facility where id in (:decendantIds)";
		final Query decendantQuery = em.createQuery(decendantHql);
		final List<Facility> decendents = (List<Facility>) decendantQuery
				.setParameter("decendantIds", ids)
				.getResultList();
		
		return decendents;
		
	}
	
}