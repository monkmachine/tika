package org.apache.tika.parser.dgn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.tika.io.EndianUtils;
import org.apache.tika.io.TikaInputStream;

public class Testing {
   
   private TikaInputStream is;
   public Testing(InputStream f) throws Exception {
      is = TikaInputStream.get(f);
      // Check the header signature
      is.mark(4);
      int sig = EndianUtils.readIntBE(is);
      // Cell library not supported - 0x08051700
      if (sig == 0x0809fe02 || sig == 0xc809fe02) {
         // Good
      } else {
         throw new IOException("Bad file signature " + sig + " = " + Integer.toHexString(sig));
      }
      is.reset();
   }
   public boolean next() throws Exception {
      // 6 bits level, 1 bit reserved, 1 bit complex
      int h1 = is.read();
      // 7 bits type, 1 bit deleted
      int h2 = is.read();
      int type = h2 & 0x7f;
      
      // End?
      if (h2 < -1 || (h1 == 0xff && h2 == 0xff)) return false;
      
      int words = EndianUtils.readUShortLE(is);
      int size = 4 + words*2;
      //System.err.println(type + "=" + Integer.toHexString(type) + " from " + words + " as " + size);

      // These aren't the right endian!
      long xlow  = EndianUtils.readUIntLE(is);
      long ylow  = EndianUtils.readUIntLE(is);
      long zlow  = EndianUtils.readUIntLE(is);
      long xhigh = EndianUtils.readUIntLE(is);
      long yhigh = EndianUtils.readUIntLE(is);
      long zhigh = EndianUtils.readUIntLE(is);

      if (type == 17) {
         // Skip symbology, fonts etc
         is.skip(32);
         // Grab the text
         int len = (words-28)*2;

         byte[] str = new byte[len] ;
         IOUtils.readFully(is, str,0, len);

         System.err.println(new String(str, StandardCharsets.US_ASCII));
      } else {
         int skip = (words-12)*2;
         is.skip(skip);
      }

      return true;
   }

   public static void main(String[] args) throws Exception {

      Testing r = new Testing(DGN7ParserTest.class.getResourceAsStream("/test-documents/1344468370.dgn"));
      try {
      while(r.next()) {}
      }
      catch (Exception e){
    	  e.printStackTrace();
      }
   }
}