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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DWGReadParser extends AbstractDWGParser {
	private static final Logger LOG = LoggerFactory.getLogger(DWGParser.class);
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
        DWGParserConfig dwgc = context.get(DWGParserConfig.class);
        final XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);

        xhtml.startDocument();
        UUID uuid = UUID.randomUUID();
        File tmpFileOut = File.createTempFile(uuid + "dwgreadout", ".json");
        File tmpFileOutCleaned = File.createTempFile(uuid + "dwgreadoutclean", ".json");
        File tmpFileIn = File.createTempFile(uuid + "dwgreadin", ".dwg");
        try {

            FileUtils.copyInputStreamToFile(stream, tmpFileIn);

            List < String > command = Arrays.asList(dwgc.getDwgReadExecutable(), "-O", "JSON", "-o",
                tmpFileOut.getCanonicalPath(), tmpFileIn.getCanonicalPath());
            Process p = new ProcessBuilder(command).start();

            try {
                int exitCode = p.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (dwgc.isCleanDwgReadOutput()) {
                // dwgread sometimes creates strings with invalid utf-8 sequences. replace them
                // with empty string.

                try (FileInputStream fis = new FileInputStream(tmpFileOut); FileOutputStream fos = new FileOutputStream(tmpFileOutCleaned)) {
                    byte[] bytes = new byte[dwgc.getCleanDwgReadOutputBatchSize()];
                    while (fis.read(bytes) != -1) {
                        byte[] fixedBytes = new String(bytes, StandardCharsets.UTF_8)
                            .replaceAll(dwgc.getCleanDwgReadRegexToReplace(), dwgc.getCleanDwgReadReplaceWith())
                            .getBytes(StandardCharsets.UTF_8);
                        fos.write(fixedBytes, 0, fixedBytes.length);
                    }
                } finally {
                    FileUtils.deleteQuietly(tmpFileOut);
                    tmpFileOut = tmpFileOutCleaned;
                }

            }
    		ObjectMapper mapper = new ObjectMapper();
    		JsonNode actualObj = mapper.readTree(tmpFileOut);
    		JsonNode array = actualObj.get("OBJECTS");
			JsonNode textNode;
			String nodeval;
			if (array.isArray()) {
				for (final JsonNode objNode : array) {
					JsonNode objectNode = objNode.get("object");
					JsonNode entityNode = objNode.get("entity");
					if (objectNode != null || entityNode != null) {
						if (objectNode != null) {
							nodeval = objectNode.textValue();
						} else {
							nodeval = entityNode.textValue();
						}

						switch (nodeval) {
						case "TEXT":
						case "ATTRIB":
							 textNode = objNode.get("text_value");					 
							 if (textNode != null) 
								 xhtml.characters(removeStringFormatting(textNode.asText()));
							break;
						case "MTEXT":
						case "BLOCK":
							 textNode = objNode.get("text");
							 if (textNode != null) 

								 xhtml.characters(removeStringFormatting(textNode.asText()));

							break;
						default:
							break;
						}
					}
				}
			}
        } finally {
            FileUtils.deleteQuietly(tmpFileOut);
            FileUtils.deleteQuietly(tmpFileIn);
        }
        
        
        xhtml.endDocument();
    }
    
    private String removeStringFormatting(String dwgString) {
		String cleanString;
		//remove \\p and replace with new line
		cleanString = dwgString.replaceAll("\\\\P", "\n");
		//replace lines with \L/l
		cleanString = cleanString.replaceAll("\\\\H[0-9]*\\.[0-9]*x;\\\\[lL]", "");
		//replace lines without \L.l
		cleanString = cleanString.replaceAll("\\\\H[0-9]*\\.[0-9]*x;", "");
		//replace Starting formating
		cleanString = cleanString.replaceAll("\\{\\\\L", "");
		//replace }
		cleanString = cleanString.replaceAll("\\}", "");
		//replace pi
		cleanString = cleanString.replaceAll("\\\\pi-[0-9].*;", "");
		//replace \A1;
		cleanString = cleanString.replaceAll("\\\\A1;", "");
		
		
		//
		return cleanString;
		
	}

}