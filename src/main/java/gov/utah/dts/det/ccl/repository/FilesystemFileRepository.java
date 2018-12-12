package gov.utah.dts.det.ccl.repository;

import gov.utah.dts.det.ccl.model.FilesystemFile;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FilesystemFileRepository extends JpaRepository<FilesystemFile, Long> {

}