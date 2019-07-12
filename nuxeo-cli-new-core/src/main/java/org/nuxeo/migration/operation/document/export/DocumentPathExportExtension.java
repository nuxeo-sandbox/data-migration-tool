package org.nuxeo.migration.operation.document.export;

import org.dom4j.Element;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.ExportedDocument;

/**
 * Exports the complete path of a document {@link DocumentModel}
 *
 */
public class DocumentPathExportExtension implements GeneralExportExtension {

    @Override
    public void updateExport(DocumentModel docModel, ExportedDocument result) throws Exception {

        String path = docModel.getPathAsString();
        Element fullPath = result.getDocument().getRootElement().addElement("fullPath");
        fullPath.setText(path);
    }
}
