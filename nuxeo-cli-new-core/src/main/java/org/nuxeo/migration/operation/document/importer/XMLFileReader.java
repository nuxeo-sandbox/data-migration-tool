package org.nuxeo.migration.operation.document.importer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentReader;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;

public class XMLFileReader extends AbstractDocumentReader {

    protected static Log log = LogFactory.getLog(XMLFileReader.class);

    private File source;

    public XMLFileReader(String sourcePath) {
        this(new File(sourcePath));
        this.readXDoc = null;
    }

    public XMLFileReader(File source) {
        this.source = source;
        this.readXDoc = null;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(File source) {
        this.source = source;
    }

    @Override
    public void close() {
        source = null;
        readXDoc = null;
    }

    private ExportedDocument readXDoc;

    @Override
    public ExportedDocument read() throws IOException {
        if (readXDoc == null) {
            // read document file
            ExportedDocument xdoc = new ExportedDocumentImpl();
            String name = source.getName();
            if (name.endsWith(".xml")) {
                // xdoc.putDocument(FileUtils.getFileNameNoExt(source.getName()),
                // loadXML(source));
                Document doc = loadXML(source);
                String path = doc.getRootElement().selectSingleNode("/document/system/path").getText();
                log.trace("-----------------------------read: path: " + path);
                xdoc.setDocument(doc);
                xdoc.setPath(new Path(path));

            } else {
                throw new IOException("Invalid file name: " + source.getName());
            }
            readXDoc = xdoc;
            return xdoc;
        }
        return null;
    }

    private static Document loadXML(File file) throws IOException {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            return new SAXReader().read(in);
        } catch (DocumentException e) {
            IOException ioe = new IOException("Failed to read file document " + file + ": " + e.getMessage());
            ioe.setStackTrace(e.getStackTrace());
            throw ioe;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}