package org.nuxeo.migration.operation.document.importer;

import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_CREATED;
import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_DESCRIPTION;
import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_LABEL;
import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_VERSIONABLE_ID;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportExtension;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.ImportExtension;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.versioning.VersioningService;

/**
 * Compared to the default {@link DocumentModelWriter} implementation this one
 * does handle versions and allows to plug {@link ExportExtension}
 *
 */
public class ExtensibleDocumentWriter extends DocumentModelWriter {

    protected static Log log = LogFactory.getLog(ExtensibleDocumentWriter.class);

    public ExtensibleDocumentWriter(CoreSession session, String parentPath) {
        super(session, parentPath);
        log.trace("----------------------------- ExtensibleDocumentWriter parentPath: " + parentPath);
    }

    protected List<ImportExtension> extensions = new ArrayList<ImportExtension>();

    public void registerExtension(ImportExtension ext) {
        extensions.add(ext);
    }

    @Override
    public DocumentTranslationMap write(ExportedDocument xdoc) throws IOException {
        log.trace("----------------------------- ExtensibleDocumentWriter write");
        if (xdoc.getDocument() == null) {
            // not a valid doc -> this may be a regular folder for example the
            // root of the tree
            return null;
        }
        Path path = xdoc.getPath();
        if (path.isEmpty() || path.isRoot()) {
            log.trace("-----------------------------empty path: " + path);
            return null; // TODO avoid to import the root
        }
        return doWrite(xdoc, path);
    }

    private DocumentTranslationMap doWrite(ExportedDocument xdoc, Path targetPath) {
        log.trace("----------------------------- ExtensibleDocumentWriter doWrite");
        String uuid = xdoc.getId();
        log.trace("-----------------------------uuid: " + uuid);
        log.trace("-----------------------------targetPath from param: " + targetPath);
        DocumentModel previousDoc = null;
        DocumentRef previousDocRef = new IdRef(uuid);
        try {
            previousDoc = session.getDocument(previousDocRef);
        } catch (DocumentNotFoundException e) {
            log.trace("-----------------------------: document not found => create! " + e);
        }
        log.trace("-----------------------------previous: " + previousDoc);
        // PathRef pathRef = new PathRef(targetPath.toString());

        DocumentModel doc;
        if (previousDoc == null) {
            doc = createDocument(xdoc, targetPath);
        } else {
            // remove document and import again:
            // session.removeDocument(previousDocRef);
            // doc = createDocument(xdoc,targetPath);
            // session.importDocuments(Collections.singletonList(doc));

            // update creates duplicate versions.
            Element version = xdoc.getDocument().getRootElement().element("version");
            if (version != null) {

                // Element e = version.element("isVersion");
                String isVersion = version.elementText("isVersion");

                if (!"true".equals(isVersion)) {
                    // do update only if it's not a version
                    // so updates only on the version series id.

                    // TODO: update only if the version series id document
                    // has changed => check modification date

                    doc = updateDocument(xdoc, previousDoc);
                } else {
                    return null;
                }
            } else {
                return null;
            }

        }

        DocumentLocation source = xdoc.getSourceLocation();
        DocumentTranslationMap map = new DocumentTranslationMapImpl(source.getServerName(), doc.getRepositoryName());
        if (source.getDocRef() != null && source.getDocRef().reference() != null) {
            map.put(source.getDocRef(), doc.getRef());
        }
        return map;
    }

    @Override
    protected DocumentModel createDocument(ExportedDocument xdoc, Path toPath) {
        log.trace("----------------------------createDocument start ...");
        log.trace("----------------------------path: " + toPath);

        // get access to the full path

        String fullPath = null;
        Document domDoc = xdoc.getDocument();
        Element rootElement = domDoc.getRootElement();
        Node fullPathNode = rootElement.selectSingleNode("/document/fullPath");
        if (fullPathNode != null) {
            fullPath = fullPathNode.getText();
            log.trace("----------------------------- fullPath: " + fullPath);
        } else {
            log.trace("----------------------------- fullPath not found!");
        }

        // get access to the baseVersion
        String baseVersion = null;
        Node baseVersionNode = rootElement.selectSingleNode("/document/baseVersion");
        if (baseVersionNode != null) {
            baseVersion = baseVersionNode.getText();
            log.trace("----------------------------- baseVersion: " + baseVersion);
        } else {
            log.trace("----------------------------- baseVersion not found!");
        }

        Path parentPath = null;
        String name = null;
        if (fullPath != null) {
            Path importedFullPath = new Path(fullPath);
            parentPath = importedFullPath.removeLastSegments(1);
            name = importedFullPath.lastSegment();
        } else {
            parentPath = toPath.removeLastSegments(1);
            name = toPath.lastSegment();
        }

        // ignore path passed to the method and use full path
        log.trace("----------------------------parent path: " + parentPath);

        // verify that parent path exists
        DocumentRef parentRef = new PathRef(parentPath.toString());
        if (!session.exists(parentRef)) {
            log.error("!!!!  parent path does not exists - return! ");
            return null;
        }

        // TODO: verify that acls exists

        DocumentModel doc = session.createDocumentModel(parentPath.toString(), name, xdoc.getType());

        doc.setPathInfo(parentPath.toString(), name);

        // set base version id
        if (baseVersion != null) {
            doc.putContextData(CoreSession.IMPORT_BASE_VERSION_ID, baseVersion);
        }

        // set lifecycle state at creation
        Element system = xdoc.getDocument().getRootElement().element(ExportConstants.SYSTEM_TAG);
        String lifeCycleState = system.element(ExportConstants.LIFECYCLE_STATE_TAG).getText();
        String lifeCyclePolicy = system.element(ExportConstants.LIFECYCLE_POLICY_TAG).getText();

        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_POLICY, lifeCyclePolicy);
        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_STATE, lifeCycleState);

        // loadFacets before schemas so that additional schemas are not skipped
        loadFacetsInfo(doc, xdoc.getDocument());

        // then load schemas data
        loadSchemas(xdoc, doc, xdoc.getDocument());

        if (doc.hasSchema("uid")) {
            doc.putContextData(VersioningService.SKIP_VERSIONING, true);
        }

        String uuid = xdoc.getId();
        if (uuid != null) {
            ((DocumentModelImpl) doc).setId(uuid);
        }

        Element version = xdoc.getDocument().getRootElement().element("version");
        if (version != null) {

            // Element e = version.element("isVersion");
            String isVersion = version.elementText("isVersion");

            if ("true".equals(isVersion)) {
                String label = version.elementText(IMPORT_VERSION_LABEL.substring(4));
                String sourceId = version.elementText(IMPORT_VERSION_VERSIONABLE_ID.substring(4));
                String desc = version.elementText(IMPORT_VERSION_DESCRIPTION.substring(4));
                String created = version.elementText(IMPORT_VERSION_CREATED.substring(4));

                if (label != null) {
                    doc.putContextData(IMPORT_VERSION_LABEL, label);
                }
                if (sourceId != null) {
                    doc.putContextData(IMPORT_VERSION_VERSIONABLE_ID, sourceId);
                }
                if (desc != null) {
                    doc.putContextData(IMPORT_VERSION_DESCRIPTION, desc);
                }
                if (created != null) {
                    doc.putContextData(IMPORT_VERSION_CREATED, (Serializable) new DateType().decode(created));
                }
                // doc.setPathInfo(parentPath.toString(), name);
                doc.setPathInfo(null, name);
                ((DocumentModelImpl) doc).setIsVersion(true);

                doc.putContextData(CoreSession.IMPORT_VERSION_MAJOR, doc.getPropertyValue("uid:major_version"));
                doc.putContextData(CoreSession.IMPORT_VERSION_MINOR, doc.getPropertyValue("uid:minor_version"));
                doc.putContextData(CoreSession.IMPORT_IS_VERSION, true);
            }
        }

        if (doc.getId() != null) {
            log.trace("----------------------------import document");
            session.importDocuments(Collections.singletonList(doc));
        } else {
            log.trace("----------------------------create document");
            doc = session.createDocument(doc);
        }

        // load into the document the system properties, document needs to exist
        loadSystemInfo(doc, xdoc.getDocument());

        for (ImportExtension ext : extensions) {
            try {
                ext.updateImport(session, doc, xdoc);
            } catch (Exception e) {
                log.error("Error while processing extensions", e);
                throw new NuxeoException(e);
            }
        }
        doc.setPathInfo(parentPath.toString(), name);
        log.trace("----------------------------document imported with Parent ref: " + doc.getParentRef());
        // todo set parent ref...

        unsavedDocuments += 1;
        saveIfNeeded();
        // save after each document ...
        // session.save();

        return doc;
    }

    /**
     * Updates an existing document.
     */
    protected DocumentModel updateDocument(ExportedDocument xdoc, DocumentModel doc) {
        log.trace("----------------------------updateDocument start ...");

        // set lifecycle state at creation
        Element system = xdoc.getDocument().getRootElement().element(ExportConstants.SYSTEM_TAG);

        String lifeCycleState = system.element(ExportConstants.LIFECYCLE_STATE_TAG).getText();
        String lifeCyclePolicy = system.element(ExportConstants.LIFECYCLE_POLICY_TAG).getText();

        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_POLICY, lifeCyclePolicy);
        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_STATE, lifeCycleState);

        // load schemas data
        loadSchemas(xdoc, doc, xdoc.getDocument());

        loadFacetsInfo(doc, xdoc.getDocument());

        String state = doc.getCurrentLifeCycleState();
        log.trace("pre state: " + state);
        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_POLICY, lifeCyclePolicy);

        doc = DocumentModelFactory.writeDocumentModel(doc, resolveReference(doc.getRef()));
        if (!doc.isCheckedOut()) {
            doc.checkOut();
        }
        if (state == null) {
            session.reinitLifeCycleState(doc.getRef());
        }
        log.trace("post state: " + doc.getCurrentLifeCycleState());

        String postLifeCyclePolicy = doc.getLifeCyclePolicy();
        if (postLifeCyclePolicy == null) {
            log.error("No lifecycle policy after update!");
        }

        unsavedDocuments += 1;
        saveIfNeeded();

        return doc;
    }

    protected org.nuxeo.ecm.core.model.Document resolveReference(DocumentRef docRef) {
        if (docRef == null) {
            throw new IllegalArgumentException("null docRref");
        }
        Object ref = docRef.reference();
        if (ref == null) {
            throw new IllegalArgumentException("null reference");
        }
        int type = docRef.type();
        switch (type) {
        case DocumentRef.ID:
            return ((AbstractSession) session).getSession().getDocumentByUUID((String) ref);
        case DocumentRef.PATH:
            return ((AbstractSession) session).getSession().resolvePath((String) ref);
        case DocumentRef.INSTANCE:
            return ((AbstractSession) session).getSession().getDocumentByUUID(((DocumentModel) ref).getId());
        default:
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }
}
