/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.dgn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.tika.io.EndianUtils;
import org.apache.commons.io.IOUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.SummaryExtractor;
import org.apache.tika.sax.XHTMLContentHandler;

/**
 * It parses metadata & Content out of dgn7 files.
 */
public class DGN7Parser extends AbstractParser {
	private static final long serialVersionUID = 7279402818737419485L;
	TikaInputStream tstream;
	Metadata DGNMeta ;
	boolean keeprunning = true;
	
	List<String> DGNContent =new ArrayList<String>();;
	
	Set<MediaType> SUPPORTED_TYPES = Collections.singleton(MediaType.image("vnd.dgn; version=7"));

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return SUPPORTED_TYPES;
	}

	@Override
	public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
			throws IOException, SAXException, TikaException {
		XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
		xhtml.startDocument();

		tstream = TikaInputStream.get(stream);
		try {
			CheckHeaderSig(tstream);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(keeprunning) {
			try {
				next(tstream);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		xhtml.endDocument();
	}

	private void next(TikaInputStream tstream) throws Exception {

			// 6 bits level, 1 bit reserved, 1 bit complex
			int h1 = tstream.read();
			// 7 bits type, 1 bit deleted
			int h2 = tstream.read();
			int type = h2 & 0x7f;
			// End?
			if (h2 < -1 || (h1 == 0xff && h2 == 0xff))
				keeprunning = false;
			int words = EndianUtils.readUShortLE(tstream);
			int size = 4 + words * 2;
			// What range does this element cover?
			// TODO Swap to readIntME on Git Head
			long xlow = EndianUtils.readUIntLE(tstream);
			long ylow = EndianUtils.readUIntLE(tstream);
			long zlow = EndianUtils.readUIntLE(tstream);
			long xhigh = EndianUtils.readUIntLE(tstream);
			long yhigh = EndianUtils.readUIntLE(tstream);
			long zhigh = EndianUtils.readUIntLE(tstream);

			// Rest of element general properties
			int graphicGroup = EndianUtils.readUShortLE(tstream);
			int skipToAttr = EndianUtils.readUShortLE(tstream);
			int properties = EndianUtils.readUShortLE(tstream);
			int symbology = EndianUtils.readUShortLE(tstream);

			if (type == 17) {
				// Skip symbology, fonts etc
				IOUtils.skipFully(tstream, 24);
				// Grab the text
				int len = (words - 28) * 2;
				byte[] str = IOUtils.readFully(tstream, len);
				DGNContent.add(new String(str, StandardCharsets.US_ASCII));
			} else {
				int skip = (words - 16) * 2;
				IOUtils.skipFully(tstream, skip);
			}
		
		keeprunning = true;

	}

	public void CheckHeaderSig(InputStream f) throws Exception {
		tstream = TikaInputStream.get(f);
		// Check the header signature
		tstream.mark(4);
		int sig = EndianUtils.readIntBE(tstream);
		// Cell library not supported - 0x08051700
		if (sig == 0x0809fe02 || sig == 0xc809fe02) {
			// Good
		} else {
			throw new IOException("Bad file signature " + sig + " = " + Integer.toHexString(sig));
		}
		tstream.reset();
	}
}
