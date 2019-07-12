package org.nuxeo.migration.operation.document.reindex;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;

/**
 *
 */
@Operation(id = ReindexOperation.ID, category = Constants.CAT_DOCUMENT, label = "Document ReindexOperation", description = "Reindex document in Elasticsearch")
public class ReindexOperation {

    public static final String ID = "Document.ReindexOperation";

    private static Log log = LogFactory.getLog(ReindexOperation.class);

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Context
    protected ElasticSearchIndexing esi;

    @Param(name = "path", required = false)
    protected String path;

    @Param(name = "uuids", required = false)
    protected String uuids;

    private void checkAccess() {
        NuxeoPrincipal principal = (NuxeoPrincipal) ctx.getPrincipal();
        if (principal == null || !principal.isAdministrator()) {
            throw new NuxeoException("Unauthorized access: " + principal);
        }
    }

    @OperationMethod
    public DocumentModel run() {
        log.trace("----------------- Reindex Operation");
        checkAccess();

        int docsProcessed = 0;

        if (uuids != null) {
            log.trace("uuids:" + uuids);
            String[] uuidList = uuids.split(",");
            for (String uuid : uuidList) {
                // find document by uuid
                DocumentRef docRef = new IdRef(uuid);
                DocumentModel doc = session.getDocument(docRef);
                try {
                    this.run(doc);
                    docsProcessed++;
                } catch (IOException | OperationException | RuntimeException e) {
                    e.printStackTrace();
                    log.error("error for uuid: " + uuid + " error: " + e);
                    // log.error(ExceptionUtils.getStackTrace(e));
                }
            }
            log.info("Docs processed: " + docsProcessed);
        }

        return session.getRootDocument();
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws IOException, OperationException {
        log.trace("------------------Starting Document Export to file operation with doc: " + doc);

        checkAccess();
        reindexDoc(doc);
        return doc;
    }

    private void reindexDoc(DocumentModel doc) {
        // index recursive from head doc
        IndexingCommand cmd = new IndexingCommand(doc, IndexingCommand.Type.INSERT, false, true);
        esi.runIndexingWorker(Arrays.asList(cmd));
        return;
    }
}
