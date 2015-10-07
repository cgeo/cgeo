import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class GenerateAttributes {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		File inFile = new File(args[0]);
		InputStream inputStream;

		try {

			writeHeader();

			inputStream = new FileInputStream(inFile);
			Reader reader = new InputStreamReader(inputStream,"UTF-8");

			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");

			parseAttributes(is);

			writeTrailer();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void writeHeader() {
		System.out.print(
"// This is a generated file, do not change manually!\n" +
"\n" +
 "# internal name   | gcid | ocid | acode | man | license | copyright holder | URL\n");
	}

	private static String formattedId(int id, int width) {
		String result = "                            ".substring(0, width - 1)
				+ id;
		return result.substring(result.length() - width);
	}

    private static void writeAttr(AttrInfo attr) {

		System.out.println("                  | " + formattedId(attr.gc_id, 4)
				+ " | " + formattedId(attr.oc_de_id, 4) + " | "
				+ formattedId(attr.acode, 5) + " |");
	}

	private static void writeTrailer() {
		System.out.println();
	}

	private static void parseAttributes(InputSource stream) {

		try {

	      SAXParserFactory factory = SAXParserFactory.newInstance();
	      SAXParser saxParser = factory.newSAXParser();

	      DefaultHandler handler = new DefaultHandler() {

	        AttrInfo attr;
	        ArrayList<String> names;
	        boolean readingName;

	        public void startElement(String uri, String localName,
	            String qName, Attributes attributes)
	            throws SAXException {

	          if (qName.equalsIgnoreCase("attr")) {
	        	  attr = new AttrInfo();
	        	  names = new ArrayList<String>();
						attr.acode = Integer.parseInt(attributes.getValue(
								"acode").substring(1));
	          }

	          if (attr != null && qName.equalsIgnoreCase("opencaching")) {
						if ("http://www.opencaching.de/"
								.equalsIgnoreCase(attributes
.getValue("schema"))) {
	        		  attr.oc_de_id = Integer.parseInt(attributes.getValue("id"));
						} else if ("http://opencaching.pl/"
								.equalsIgnoreCase(attributes.getValue("schema"))) {
	        		  attr.oc_pl_id = Integer.parseInt(attributes.getValue("id"));
						} else if ("http://www.opencaching.nl/"
								.equalsIgnoreCase(attributes.getValue("schema"))) {
							attr.oc_nl_id = Integer.parseInt(attributes
									.getValue("id"));
	        	  }
	          }

					if (attr != null && qName.equalsIgnoreCase("groundspeak")) {
						attr.gc_id = Integer
								.parseInt(attributes.getValue("id"));
					}

	          if (names != null && qName.equalsIgnoreCase("name")) {
	        	  readingName = true;
	          }
	        }

	        public void endElement(String uri, String localName,
	                String qName)
	                throws SAXException {

	        	if (attr != null && qName.equalsIgnoreCase("attr")) {
	        	  attr.names = names.toArray(new String[]{});
	        	  names = null;
	              writeAttr(attr);
	              attr = null;
	        	}

	        	readingName = false;
	        }

			public void characters(char ch[], int start, int length)
	            throws SAXException {

	          if (readingName) {
	            names.add(new String(ch, start, length));
	          }
	        }

	      };

	      saxParser.parse(stream, handler);


	    } catch (Exception e) {
	      e.printStackTrace();
	    }

	}

	static class AttrInfo {
		public int oc_de_id;
		public int oc_nl_id;
		public int oc_pl_id;
		public int acode;
		public int gc_id;
		public String[] names;
	}

}
