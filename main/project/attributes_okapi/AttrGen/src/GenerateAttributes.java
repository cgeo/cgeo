import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;;


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
"package cgeo.geocaching.connector.oc;\n" +
"\n" +
"import java.util.HashMap;\n" +
"import java.util.Map;\n" +
"\n" +
"public class AttributeParser {\n" +
"\n" +
"    private final static Map<String, Integer> attrMapDe;\n" +
"    private final static Map<String, Integer> attrMapPl;\n" +
"\n" +
"    static {\n" +
"        attrMapDe = new HashMap<String, Integer>();\n" +
"        attrMapPl = new HashMap<String, Integer>();\n" +
"\n" +
"        // last header line\n");
	}

    private static void writeAttr(AttrInfo attr) {
    	
    	for(String name : attr.names) {
    		if (attr.oc_de_id > 0) {
    			System.out.println("        attrMapDe.put(\"" + name + "\", " + attr.oc_de_id + ");");
    		}
    		if (attr.oc_pl_id > 0) {
    			System.out.println("        attrMapPl.put(\"" + name + "\", " + attr.oc_pl_id + ");");
    		}
    	}
		
	}
    
	private static void writeTrailer() {
		System.out.print(
"        // first trailer line\n" +
"\n" +
"    }\n" +
"\n" +
"    public static int getOcDeId(final String name) {\n" +
"\n" +
"        int result = 0;\n" +
"\n" +
"        if (attrMapDe.containsKey(name)) {\n" +
"            result = attrMapDe.get(name);\n" +
"        }\n" +
"        return result;\n" +
"    }\n" +
"}\n");		
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
	          }

	          if (attr != null && qName.equalsIgnoreCase("opencaching")) {
	        	  if ("http://opencaching.de/".equalsIgnoreCase(attributes.getValue("site_url"))) {
	        		  attr.oc_de_id = Integer.parseInt(attributes.getValue("id"));
	        	  } else if ("http://opencaching.pl/".equalsIgnoreCase(attributes.getValue("site_url"))) {
	        		  attr.oc_pl_id = Integer.parseInt(attributes.getValue("id"));
	        	  }
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
		public int oc_pl_id;
		public String[] names;
	}
	
}
