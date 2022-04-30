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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.apache.tika.sax.XHTMLContentHandler;

/**
 * It parses metadata & Content out of dgn7 files.
 */
public class DGN7Parser extends AbstractParser {
	private static final long serialVersionUID = 7279402818737419485L;
	private TikaInputStream tstream;
	Metadata DGNMeta;
	private Set<Integer> DGNElementTypes = new TreeSet<>();
	private List<String> DGNContent = new ArrayList<String>();;
	private boolean keeprunning = true;

	Set<MediaType> SUPPORTED_TYPES = Collections.singleton(MediaType.image("vnd.dgn; version=7"));

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return SUPPORTED_TYPES;
	}

	@Override
	public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
			throws IOException, SAXException, TikaException {


		tstream = TikaInputStream.get(stream);
		try {
			CheckHeaderSig(tstream);
		} catch (Exception e) {

			throw new TikaException("DGN Parser Exception: " + e.getMessage());

		}
		while (keeprunning) {
			try {
				extractContent(tstream);
			} catch (Exception e) {
				keeprunning = false;
				throw new TikaException("DGN Parser Exception: " + e.getMessage());
				
			}
		}

		XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
		xhtml.startDocument();
		metadata.add("ElemetTypes", DGNElementTypes.toString());
		String content = null;
        xhtml.startDocument();
        for (Iterator<String> iter = DGNContent.iterator(); iter.hasNext(); ) {
        	
        	content = content + "\n"+iter.next();
        	
        }
        
        xhtml.element("p", content);
        System.out.println(DGNElementTypes);
		// System.out.println(DGNContent);

        xhtml.endDocument();
	}

	private void extractContent(TikaInputStream tstream) throws Exception {

		// 6 bits level, 1 bit reserved, 1 bit complex
		int h1 = tstream.read();
		// 7 bits type, 1 bit deleted
		int h2 = tstream.read();
		int type = h2 & 0x7f;
		// End?
		if (h2 < -1 || (h1 == 0xff && h2 == 0xff)) {
			keeprunning = false;
			return;
		}
		int words = EndianUtils.readUShortLE(tstream);
		// These may be useful in future if some of the other attributes of a DGN we
		// want to pull out, leaving as unused for the moment
		@SuppressWarnings("unused")
		int size = 4 + words * 2;
		// What range does this element cover?

		// TODO Swap to readIntME on Git Head
		@SuppressWarnings("unused")
		long xlow = EndianUtils.readUIntLE(tstream);
		@SuppressWarnings("unused")
		long ylow = EndianUtils.readUIntLE(tstream);
		@SuppressWarnings("unused")
		long zlow = EndianUtils.readUIntLE(tstream);
		@SuppressWarnings("unused")
		long xhigh = EndianUtils.readUIntLE(tstream);
		@SuppressWarnings("unused")
		long yhigh = EndianUtils.readUIntLE(tstream);
		@SuppressWarnings("unused")
		long zhigh = EndianUtils.readUIntLE(tstream);

		// Rest of element general properties
		@SuppressWarnings("unused")
		int graphicGroup = EndianUtils.readUShortLE(tstream);
		@SuppressWarnings("unused")
		int skipToAttr = EndianUtils.readUShortLE(tstream);
		@SuppressWarnings("unused")
		int properties = EndianUtils.readUShortLE(tstream);
		@SuppressWarnings("unused")
		int symbology = EndianUtils.readUShortLE(tstream);

		DGNElementTypes.add(type);
		if (type == 17) {
			// Skip symbology, fonts etc
			IOUtils.skipFully(tstream, 24);
			// Grab the text
			int len = (words - 28) * 2;
			String str =new String( IOUtils.readFully(tstream, len), StandardCharsets.ISO_8859_1);
			DGNContent.add(str.replaceAll("[^\\x20-\\x7E]", ""));
			
			
		} else {
			int skip = (words - 16) * 2;
			tstream.skip(skip);

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
			throw new TikaException("Bad file signature " + sig + " = " + Integer.toHexString(sig));

		}
		tstream.reset();
	}
}
