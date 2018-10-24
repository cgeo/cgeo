package cgeo.geocaching.connector.su;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;

public class SUParserTest extends AbstractResourceInstrumentationTestCase {

    @Test
    public void testCanHandle() throws Exception {
        final InputStream stubInputStream =
                IOUtils.toInputStream("<data>\n" +
                        "<cache>\n" +
                        "<id>4439</id>\n" +
                        "<name>Холм имени Ивана Оруженосца</name>\n" +
                        "<autor>Клюкв</autor>\n" +
                        "<lat>54.4446333</lat>\n" +
                        "<lng>53.4865000</lng>\n" +
                        "<ctype>Традиционный</ctype>\n" +
                        "<status>1</status>\n" + "<waypoints>\n" +
                        "<waypoint lat=\"1.23\" lon=\"2.132\" type=\"3\" name=\"Имя точки\">Количество окошек - А вопрос</waypoint>\n" +
                        "<waypoint lat=\"47.5\" lon=\"39.45538\" type=\"1\" name=\"Dnjhfz njxrf\">Описание раз</waypoint>\n" +
                        "</waypoints>" +
                        "<availability>2</availability>\n" +
                        "<complexity>4</complexity>\n" +
                        "<last>2018-08-08 16:27:20</last>\n" +
                        "</cache>\n" +
                        "</data>", "UTF-8");
        final SearchResult caches = GeocachingSuParser.parseCaches("data", stubInputStream);

        Assert.assertFalse(caches.isEmpty());
    }

}
