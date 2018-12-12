package gov.utah.dts.det.ccl.repository;

import gov.utah.dts.det.ccl.model.Document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {

	@Query("from Document where context.value in (:contexts) order by sortOrder, name")
	public List<Document> getDocumentsInContexts(@Param("contexts") List<String> contexts);
}