package gov.utah.dts.det.ccl.repository;

import gov.utah.dts.det.ccl.model.StateChange;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StateChangeRepository extends JpaRepository<StateChange, Long> {

	@Query("select sc from StateChange sc where sc.stateObject.id = :id order by sc.changeDate desc ")
	public List<StateChange> findByStateObjectId(@Param("id") Long id);
}