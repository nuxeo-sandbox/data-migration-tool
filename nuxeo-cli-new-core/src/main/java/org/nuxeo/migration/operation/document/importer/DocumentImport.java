package org.nuxeo.migration.operation.document.importer;

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
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.ZipReader;

import org.nuxeo.migration.operation.document.export.Helper;

/**
 *
 */
@Operation(id = DocumentImport.ID, category = Constants.CAT_DOCUMENT, label = "Document Import from File on Server", description = "Imports the metadata of a document from a defined path on the server where nuxeo runs into nuxeo.")
public class DocumentImport {

    public static final String ID = "Document.ImportOperation";

    private static Log log = LogFactory.getLog(DocumentImport.class);

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "path", required = false)
    protected String path;

    @Param(name = "uuid", required = false)
    protected String uuid;

    @Param(name = "uuids", required = false)
    protected String uuids;

    @Param(name = "deleteAfterImport", required = false)
    protected boolean deleteAfterImport;

    @OperationMethod
    public DocumentModel run() throws IOException, OperationException {
        log.trace("----------------Document.ImportOperation start...");
        if (!(ctx.getPrincipal() instanceof NuxeoPrincipal)
                || !((NuxeoPrincipal) ctx.getPrincipal()).isAdministrator()) {
            throw new OperationException("Not allowed. You must be an administrator to use this operation");
        }

        int docsProcessed = 0;

        if (uuids != null) {
            DocumentModel lastImportedDoc = null;
            log.trace("uuids:" + uuids);
            String[] uuidList = uuids.split(",");
            for (String uuid : uuidList) {
                try {
                    lastImportedDoc = this.doImport(uuid);
                    docsProcessed++;
                } catch (IOException | OperationException | RuntimeException e) {
                    e.printStackTrace();
                    log.error("error for uuid: " + uuid + " error: " + e);
                }
            }
            log.info("Docs processed: " + docsProcessed);
            return lastImportedDoc;
        } else if (uuid != null) {
            return this.doImport(uuid);
        } else {
            throw new OperationException("no uuid and no uuids given!");
        }
    }

    public DocumentModel doImport(String uuid) throws IOException, OperationException {

        // just for the tests:
        if (path.equals("/")) {
            return session.getRootDocument();
        }

        log.trace("uuid: " + uuid);
        log.trace("path from params: " + path);

        DocumentReader reader = null;
        DocumentWriter writer = null;

        // just for having the same names as in the example ...
        CoreSession docMgr = session;

        String pathAndFilename = Helper.getZipFilename(uuid);

        try {

            File exportArchive = new File(pathAndFilename);

            // define reader and writer for pipe
            reader = new ZipReader(exportArchive);
            writer = new ExtensibleDocumentWriter(docMgr, path);

            // creating a pipe
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }

        log.trace("--- end - import done for : " + uuid);

        if (deleteAfterImport) {
            log.trace("delete : " + pathAndFilename);
            File toDelete = new File(pathAndFilename);
            toDelete.delete();
        }

        return session.getDocument(new IdRef(uuid));
    }
}
