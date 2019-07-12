package org.nuxeo.migration.operation.document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.binary.AbstractBinaryManager;
import org.nuxeo.ecm.core.blob.binary.Binary;

/**
 * A simple binary manager which always returns the same file. This is only for
 * use during import or reindexing when it's very clear that all the files
 * should exists. Any kind of import of files will not work with this binary
 * manager.
 */
public class FakeBinaryManager extends AbstractBinaryManager {

    private File defaultFile;

    private static final Log log = LogFactory.getLog(FakeBinaryManager.class);

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    private Binary getDefaultBinary(String digest) {
        if (defaultFile == null) {
            try {
                defaultFile = File.createTempFile("default-binary_", ".tmp");
            } catch (IOException e) {
                log.error("unable to create temp file: " + e);
                return null;
            }
        }
        return new Binary(defaultFile, digest, blobProviderId);
    }

    @Override
    protected Binary getBinary(InputStream in) throws IOException {
        log.trace("get binary for: " + in);
        return getDefaultBinary("42");
    }

    @Override
    public Binary getBinary(String digest) {
        log.trace("get binary for: " + digest);
        return getDefaultBinary(digest);
    }

}