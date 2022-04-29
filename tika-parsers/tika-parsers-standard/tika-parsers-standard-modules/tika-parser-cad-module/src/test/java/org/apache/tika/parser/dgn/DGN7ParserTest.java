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


import java.io.InputStream;


import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;
import org.apache.tika.TikaTest;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

public class DGN7ParserTest extends TikaTest {
    /**
     * Try with a simple file
     */

	@Test
    public void testBasics() throws Exception {
    	InputStream input =
    			DGN7ParserTest.class.getResourceAsStream("/test-documents/testDGN7.dgn");
        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, "vnd.dgn; version=7");
        ContentHandler handler = new BodyContentHandler();
        ParseContext context = new ParseContext();
        new DGN7Parser().parse(input, handler, metadata, context);
    }

}
