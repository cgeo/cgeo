import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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

        final File inFile = new File(args[0]);
        InputStream inputStream;

        try {
            writeHeader();

            inputStream = new FileInputStream(inFile);
            final Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            final InputSource is = new InputSource(reader);
            is.setEncoding("UTF-8");

            parseAttributes(is);

            writeTrailer();

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeHeader() {
        System.out.print("// This is a generated file, do not change manually!\n" + "\n"
                + "# internal name   | gcid | ocid(de) | acode | ocid(pl) | ocid(nl) | ocid(ro) | ocid(uk) | ocid(us) |\n");
    }

    private static String formattedId(int id, int width) {
        final String result = "                            ".substring(0, width - 1) + id;
        return result.substring(result.length() - width);
    }

    private static void writeAttr(AttrInfo attr) {

        System.out.println("                  | " + formattedId(attr.idGC, 4) + " | " + formattedId(attr.idOCDE, 8)
                + " | " + formattedId(attr.acode, 5) + " | " + formattedId(attr.idOCPL, 8)
                + " | " + formattedId(attr.idOCNL, 8) + " | " + formattedId(attr.idOCRO, 8)
                + " | " + formattedId(attr.idOCUK, 8) + " | " + formattedId(attr.idOCUS, 8) + " |");
    }

    private static void writeTrailer() {
        System.out.println();
    }

    private static void parseAttributes(InputSource stream) {

        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            final SAXParser saxParser = factory.newSAXParser();

            final DefaultHandler handler = new DefaultHandler() {

                AttrInfo attr;
                ArrayList<String> names;
                boolean readingName;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {

                    if (qName.equalsIgnoreCase("attr")) {
                        attr = new AttrInfo();
                        names = new ArrayList<String>();
                        attr.acode = Integer.parseInt(attributes.getValue("acode").substring(1));
                    }

                    if (attr != null && qName.equalsIgnoreCase("opencaching")) {
                        final String schema = attributes.getValue("schema");
                        final int id = Integer.parseInt(attributes.getValue("id"));
                        if ("OCDE".equalsIgnoreCase(schema)) {
                            attr.idOCDE = id;
                        } else if ("OCPL".equalsIgnoreCase(schema)) {
                            attr.idOCPL = id;
                        } else if ("OCNL".equalsIgnoreCase(schema)) {
                            attr.idOCNL = id;
                        } else if ("OCRO".equalsIgnoreCase(schema)) {
                            attr.idOCRO = id;
                        } else if ("OCUK".equalsIgnoreCase(schema)) {
                            attr.idOCUK = id;
                        } else if ("OCUS".equalsIgnoreCase(schema)) {
                            attr.idOCUS = id;
                        }
                    }

                    if (attr != null && qName.equalsIgnoreCase("groundspeak")) {
                        attr.idGC = Integer.parseInt(attributes.getValue("id"));
                    }

                    if (attr != null && qName.equalsIgnoreCase("lang") && "en".equalsIgnoreCase(attributes.getValue("id"))) {
                        // TODO read english name
                        readingName = true;
                    }
                }

                @Override
                public void endElement(final String uri, final String localName, final String qName) throws SAXException {

                    if (attr != null && qName.equalsIgnoreCase("name")) {
                        attr.names = names.toArray(new String[]{});
                        names = null;
                        writeAttr(attr);
                        attr = null;
                    }

                    readingName = false;
                }

                @Override
                public void characters(final char[] ch, final int start, final int length) throws SAXException {

                    if (readingName) {
                        names.add(new String(ch, start, length));
                    }
                }

            };

            saxParser.parse(stream, handler);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    static class AttrInfo {
        public int acode;
        public int idGC;
        public int idOCDE;
        public int idOCPL;
        public int idOCNL;
        public int idOCRO;
        public int idOCUK;
        public int idOCUS;
        public String[] names;
    }
}
