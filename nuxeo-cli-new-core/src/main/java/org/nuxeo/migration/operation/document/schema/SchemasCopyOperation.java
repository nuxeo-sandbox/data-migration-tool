package org.nuxeo.migration.operation.document.schema;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 *
 */
@Operation(id = SchemasCopyOperation.ID, category = Constants.CAT_DOCUMENT, label = "Document SchemasCopyOperation", description = "Copy all the properties from the schemas of the source into the input document. Multiple schemas are separated by commas. Either sourceId or sourcePath parameter should be filled. When both are filled, sourceId will be used. If saveDocument is true, the document is saved. If save is true, the session is saved (setting save to true and saveDocument to false has no effect, the session will not be saved)")
public class SchemasCopyOperation {

    public static final String ID = ".Document.SchemasCopyOperation";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Param(name = "schema")
    protected String schema;

    @Param(name = "sourceId", required = false)
    protected String sourceId;

    @Param(name = "sourcePath", required = false)
    protected String sourcePath;

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @Param(name = "saveDocument", required = false, values = { "true" })
    protected boolean saveDocument = true;

    private DocumentModel getDocumentFromIdOrPath() throws OperationException {
        if (sourceId != null) {
            return session.getDocument(new IdRef(sourceId));
        } else if (sourcePath != null) {
            return session.getDocument(new PathRef(sourcePath));
        } else {
            throw new OperationException("No document id or path was provided");
        }
    }

    private void copySchemaProperties(DocumentModel source, DocumentModel target) {
        String[] schemaList = schema.split(",");

        for (String singleSchema : schemaList) {
            target.setProperties(singleSchema, source.getProperties(singleSchema));
        }
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel target) throws OperationException {
        DocumentModel source = getDocumentFromIdOrPath();
        copySchemaProperties(source, target);

        if (saveDocument) {
            target = session.saveDocument(target);
            if (save) {
                session.save();
            }
        }
        return target;
    }
}
