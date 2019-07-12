package org.nuxeo.migration.operation.document.export;

import java.io.File;

import org.nuxeo.common.Environment;

public class Helper {

    public static String BASEFOLDER = "doc-exchange/";

    private static String getDocFolder() {
        String logDir = Environment.getDefault().getLog().getAbsolutePath();

        if (!logDir.endsWith("/")) {
            logDir = logDir + "/";
        }

        String path = logDir + BASEFOLDER;
        return path;
    }

    public static String checkSubfolder(String uuid) {

        String firstPart = uuid.split("-")[0];
        String subfolder = String.join("/", firstPart.split("(?<=\\G..)")) + "/";
        String path = getDocFolder() + subfolder;

        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return subfolder;
    }

    public static String getZipFilename(String uuid) {

        String subfolder = Helper.checkSubfolder(uuid);
        String pathAndFilename = getDocFolder() + subfolder + uuid + ".zip";

        return pathAndFilename;
    }
}
