package org.nuxeo.migration.operation.document.importer;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;

/**
 * A writer which is creating new docs or updating existing docs.
 *
 */
// TODO: improve it ->
// modify core session to add a batch create method and use it
public class DocumentModelWriter extends AbstractDocumentModelWriter {

    private static final Log log = LogFactory.getLog(DocumentModelWriter.class);

    /**
     * @param session the session to the repository where to write
     * @param parentPath where to write the tree. this document will be used as
     *            the parent of all top level documents passed as input. Note
     *            that you may have
     */
    public DocumentModelWriter(CoreSession session, String parentPath) {
        super(session, parentPath);
    }

    public DocumentModelWriter(CoreSession session, String parentPath, int saveInterval) {
        super(session, parentPath, saveInterval);
    }

    @Override
    public DocumentTranslationMap write(ExportedDocument xdoc) throws IOException {
        if (xdoc.getDocument() == null) {
            // not a valid doc -> this may be a regular folder for example the
            // root of the tree
            return null;
        }
        Path path = xdoc.getPath();
        // if (path.isEmpty() || path.isRoot()) {
        // return; // TODO avoid to import the root
        // }
        path = root.append(path); // compute target path

        return doWrite(xdoc, path);
    }

    private DocumentTranslationMap doWrite(ExportedDocument xdoc, Path targetPath) {

        DocumentModel previousDoc = null;
        PathRef pathRef = new PathRef(targetPath.toString());
        if (session.exists(pathRef)) {
            previousDoc = session.getDocument(pathRef);
        }

        DocumentModel doc;
        if (previousDoc == null) {
            doc = createDocument(xdoc, targetPath);
        } else {
            doc = updateDocument(xdoc, previousDoc);
        }

        DocumentLocation source = xdoc.getSourceLocation();
        DocumentTranslationMap map = new DocumentTranslationMapImpl(source.getServerName(), doc.getRepositoryName());
        if (source.getDocRef() != null && source.getDocRef().reference() != null) {
            map.put(source.getDocRef(), doc.getRef());
        }
        return map;
    }
}