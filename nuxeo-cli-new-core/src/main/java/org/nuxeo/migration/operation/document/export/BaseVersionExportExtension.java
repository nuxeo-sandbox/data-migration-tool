package org.nuxeo.migration.operation.document.export;

import org.dom4j.Element;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.ExportedDocument;

/**
 * Exports the baseVersion for a Document which dos not represent a version
 * {@link DocumentModel}
 *
 */
public class BaseVersionExportExtension implements GeneralExportExtension {

    @Override
    public void updateExport(DocumentModel docModel, ExportedDocument result) throws Exception {
        // if the document is a version, then there is not base version,
        // => add the baseVersion only if the document is not a version
        // and the document has a baseVersion.
        if (!docModel.isVersion()) {
            CoreSession session = docModel.getCoreSession();
            DocumentRef srcRef = docModel.getRef();
            DocumentRef baseVersionRef = session.getBaseVersion(srcRef);
            if (baseVersionRef != null) {
                DocumentModel baseVersionModel = session.getDocument(baseVersionRef);
                if (!baseVersionModel.getDocumentType().isFolder()) {
                    String baseVersionId = baseVersionModel.getId();

                    Element baseVersionElement = result.getDocument().getRootElement().addElement("baseVersion");
                    baseVersionElement.setText(baseVersionId);
                }
            }
        }
    }
}
