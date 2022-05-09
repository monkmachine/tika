package org.apache.tika.parser.dwg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class DWGReadParser extends AbstractDWGParser {

    /**
     * 
     */
    private static final long serialVersionUID = 7983127145030096837L;
    private static MediaType TYPE = MediaType.image("vnd.dwg");

    public Set < MediaType > getSupportedTypes(ParseContext context) {
        return Collections.singleton(TYPE);
    }

    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
    throws IOException, SAXException, TikaException {

        configure(context);

        final XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);

        xhtml.startDocument();
        UUID uuid = UUID.randomUUID();
        File tmpFileOut = File.createTempFile(uuid + "dwgreadout", ".json");
        File tmpFileOutCleaned = File.createTempFile(uuid + "dwgreadoutclean", ".json");
        File tmpFileIn = File.createTempFile(uuid + "dwgreadin", ".dwg");
        try {

            FileUtils.copyInputStreamToFile(stream, tmpFileIn);

            List < String > command = Arrays.asList(this.getDwgReadExecutable(), "-O", "JSON", "-o",
                tmpFileOut.getCanonicalPath(), tmpFileIn.getCanonicalPath());
            Process p = new ProcessBuilder(command).start();

            try {
                int exitCode = p.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (this.isCleanDwgReadOutput()) {
                // dwgread sometimes creates strings with invalid utf-8 sequences. replace them
                // with empty string.

                try (FileInputStream fis = new FileInputStream(tmpFileOut); FileOutputStream fos = new FileOutputStream(tmpFileOutCleaned)) {
                    byte[] bytes = new byte[this.getCleanDwgReadOutputBatchSize()];
                    while (fis.read(bytes) != -1) {
                        byte[] fixedBytes = new String(bytes, StandardCharsets.UTF_8)
                            .replaceAll(this.getCleanDwgReadRegexToReplace(), this.getCleanDwgReadReplaceWith())
                            .getBytes(StandardCharsets.UTF_8);
                        fos.write(fixedBytes, 0, fixedBytes.length);
                    }
                } finally {
                    FileUtils.deleteQuietly(tmpFileOut);
                    tmpFileOut = tmpFileOutCleaned;
                }

            }

        } finally {
            FileUtils.deleteQuietly(tmpFileOut);
            FileUtils.deleteQuietly(tmpFileIn);
        }
        xhtml.endDocument();
    }

}