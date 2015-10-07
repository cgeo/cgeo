package cgeo.geocaching.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();
    public static final ObjectReader reader = mapper.reader();
    public static final ObjectWriter writer = mapper.writer();

    public static final JsonNodeFactory factory = new JsonNodeFactory(true);

    private JsonUtils() {
        // Do not instantiate
    }

}
