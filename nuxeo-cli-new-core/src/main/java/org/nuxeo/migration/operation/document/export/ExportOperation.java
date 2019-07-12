package org.nuxeo.migration.operation.document.export;

import java.io.File;
import java.io.IOException;

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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.TransactionBatchingDocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;

/**
 *
 */
@Operation(id = ExportOperation.ID, category = Constants.CAT_DOCUMENT, label = "Document Export To File on Server", description = "Exports the metadata of a document from Nuxeo to a defined path on the server where Nuxeo runs.")
public class ExportOperation {

    public static final String ID = "Document.ExportOperation";

    private static Log log = LogFactory.getLog(ExportOperation.class);

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "uuids", required = false)
    protected String uuids;

    @OperationMethod
    public DocumentModel run() {
        log.trace("-----------------Export Operation: entering in the run method");

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
        if (!(ctx.getPrincipal() instanceof NuxeoPrincipal)
                || !((NuxeoPrincipal) ctx.getPrincipal()).isAdministrator()) {
            throw new OperationException("Not allowed. You must be administrator to use this operation");
        }
        // just for having the same names as in the example ...
        CoreSession docMgr = session;
        DocumentModel src = doc;

        boolean readFolderChildren = false;
        boolean excludeRoot = false;

        ExtensibleDocumentTreeReaderNoBlobs reader = new ExtensibleDocumentTreeReaderNoBlobs(docMgr, src, excludeRoot,
                readFolderChildren);
        DocumentWriter writer = null;
        File exportTempFile = null;
        String uuid = src.getId();
        String filename = Helper.getZipFilename(uuid);
        log.trace("filename with path: " + filename);
        exportTempFile = new File(filename);

        // todo: add path parameter again to define the output path
        // and use as default "doc-exchange/"

        try {
            // create a pipe that will process 10 documents on each iteration
            DocumentPipe pipe = new TransactionBatchingDocumentPipeImpl(10);
            reader.setInlineBlobs(false);

            // register version info extension
            reader.registerExtension(new VersionInfoExportExtension());

            // base version extension
            // only documents have a base version..., not folders
            reader.registerExtension(new BaseVersionExportExtension());

            // full path
            reader.registerExtension(new DocumentPathExportExtension());

            writer = new NuxeoArchiveWriter(exportTempFile);

            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
        // export doc to path on server in xml or zip format

        // return the exported doc
        return doc;
    }
}