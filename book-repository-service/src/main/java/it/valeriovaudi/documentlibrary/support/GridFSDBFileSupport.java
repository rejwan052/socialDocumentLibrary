package it.valeriovaudi.documentlibrary.support;

import com.mongodb.gridfs.GridFSDBFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Valerio on 30/04/2015.
 */
public abstract class GridFSDBFileSupport {

    private static final Logger LOGGER  = LoggerFactory.getLogger(GridFSDBFileSupport.class);
    private GridFSDBFileSupport() {
    }

    public static byte[] gridFSDBFile2ByteArray(GridFSDBFile gridFSDBFile) {
        byte[] bytes;
        try (ByteArrayOutputStream byteArrayOutputStream1 = new ByteArrayOutputStream()) {
            gridFSDBFile.writeTo(byteArrayOutputStream1);
            bytes = byteArrayOutputStream1.toByteArray();
        } catch (IOException | NullPointerException e) {
            LOGGER.error(e.getMessage());
            return null;
        }
        return bytes;
    }
}
