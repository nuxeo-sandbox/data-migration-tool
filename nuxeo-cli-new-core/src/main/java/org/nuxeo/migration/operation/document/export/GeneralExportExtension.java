package org.nuxeo.migration.operation.document.export;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.ExportedDocument;

/**
 * Interface for extension used to enrich the export This general one uses the
 * interface ExportedDocument instead of the ExportedDocumentImpl.
 *
 */
public interface GeneralExportExtension {

    void updateExport(DocumentModel docModel, ExportedDocument result) throws Exception;

}
