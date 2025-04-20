package cgeo.geocaching.brouter.core;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

public class RoutingParamCollector {

    static final boolean DEBUG = false;

    /**
     * get a list of points and optional extra info for the points
     *
     * @param lonLats  linked list separated by ';' or '|'
     * @return         a list
     */
    public List<OsmNodeNamed> getWayPointList(final String lonLats) {
        if (lonLats == null) {
            throw new IllegalArgumentException("lonlats parameter not set");
        }

        final String[] coords = lonLats.split(";|\\|"); // use both variantes
        if (coords.length < 1 || !coords[0].contains(",")) {
            throw new IllegalArgumentException("we need one lat/lon point at least!");
        }

        final List<OsmNodeNamed> wplist = new ArrayList<>();
        for (int i = 0; i < coords.length; i++) {
            final String[] lonLat = coords[i].split(",");
            if (lonLat.length < 1) {
                throw new IllegalArgumentException("we need one lat/lon point at least!");
            }
            wplist.add(readPosition(lonLat[0], lonLat[1], "via" + i));
            if (lonLat.length > 2) {
                if (lonLat[2].equals("d")) {
                    wplist.get(wplist.size() - 1).direct = true;
                } else {
                    wplist.get(wplist.size() - 1).name = lonLat[2];
                }
            }
        }

        if (wplist.get(0).name.startsWith("via")) {
            wplist.get(0).name = "from";
        }
        if (wplist.get(wplist.size() - 1).name.startsWith("via")) {
            wplist.get(wplist.size() - 1).name = "to";
        }

        return wplist;
    }

    /**
     * get a list of points (old style, positions only)
     *
     * @param lons  array with longitudes
     * @param lats  array with latitudes
     * @return      a list
     */
    public List<OsmNodeNamed> readPositions(final double[] lons, final double[] lats) {
        final List<OsmNodeNamed> wplist = new ArrayList<>();

        if (lats == null || lats.length < 2 || lons == null || lons.length < 2) {
            return wplist;
        }

        for (int i = 0; i < lats.length && i < lons.length; i++) {
            final OsmNodeNamed n = new OsmNodeNamed();
            n.name = "via" + i;
            n.ilon = (int) ((lons[i] + 180.) * 1000000. + 0.5);
            n.ilat = (int) ((lats[i] + 90.) * 1000000. + 0.5);
            wplist.add(n);
        }

        if (wplist.get(0).name.startsWith("via")) {
            wplist.get(0).name = "from";
        }
        if (wplist.get(wplist.size() - 1).name.startsWith("via")) {
            wplist.get(wplist.size() - 1).name = "to";
        }

        return wplist;
    }

    private OsmNodeNamed readPosition(final String vlon, final String vlat, final String name) {
        if (vlon == null) {
            throw new IllegalArgumentException("lon " + name + " not found in input");
        }
        if (vlat == null) {
            throw new IllegalArgumentException("lat " + name + " not found in input");
        }

        return readPosition(Double.parseDouble(vlon), Double.parseDouble(vlat), name);
    }

    private OsmNodeNamed readPosition(final double lon, final double lat, final String name) {
        final OsmNodeNamed n = new OsmNodeNamed();
        n.name = name;
        n.ilon = (int) ((lon + 180.) * 1000000. + 0.5);
        n.ilat = (int) ((lat + 90.) * 1000000. + 0.5);
        return n;
    }

    /**
     * read a url like parameter list linked with '&'
     *
     * @param url  parameter list
     * @return     a hashmap of the parameter
     * @throws     UnsupportedEncodingException
     */
    public Map<String, String> getUrlParams(final String url) throws UnsupportedEncodingException {
        final Map<String, String> params = new HashMap<>();
        final String decoded = URLDecoder.decode(url, "UTF-8");
        final StringTokenizer tk = new StringTokenizer(decoded, "?&");
        while (tk.hasMoreTokens()) {
            final String t = tk.nextToken();
            final StringTokenizer tk2 = new StringTokenizer(t, "=");
            if (tk2.hasMoreTokens()) {
                final String key = tk2.nextToken();
                if (tk2.hasMoreTokens()) {
                    final String value = tk2.nextToken();
                    params.put(key, value);
                }
            }
        }
        return params;
    }

    /**
     * fill a parameter map into the routing context
     *
     * @param rctx    the context
     * @param wplist  the list of way points needed for 'straight' parameter
     * @param params  the list of parameters
     */
    public void setParams(final RoutingContext rctx, final List<OsmNodeNamed> wplist, final Map<String, String> params) {
        if (params != null) {
            if (params.isEmpty()) {
                return;
            }

            // prepare nogos extra
            if (params.containsKey("nogoLats") && !params.get("nogoLats").isEmpty()) {
                final List<OsmNodeNamed> nogoList = readNogos(params.get("nogoLons"), params.get("nogoLats"), params.get("nogoRadi"));
                if (nogoList != null) {
                    RoutingContext.prepareNogoPoints(nogoList);
                    if (rctx.nogopoints == null) {
                        rctx.nogopoints = nogoList;
                    } else {
                        rctx.nogopoints.addAll(nogoList);
                    }
                }
                params.remove("nogoLats");
                params.remove("nogoLons");
                params.remove("nogoRadi");
            }
            if (params.containsKey("nogos")) {
                final List<OsmNodeNamed> nogoList = readNogoList(params.get("nogos"));
                if (nogoList != null) {
                    RoutingContext.prepareNogoPoints(nogoList);
                    if (rctx.nogopoints == null) {
                        rctx.nogopoints = nogoList;
                    } else {
                        rctx.nogopoints.addAll(nogoList);
                    }
                }
                params.remove("nogos");
            }
            if (params.containsKey("polylines")) {
                final List<OsmNodeNamed> result = new ArrayList<>();
                parseNogoPolygons(params.get("polylines"), result, false);
                if (rctx.nogopoints == null) {
                    rctx.nogopoints = result;
                } else {
                    rctx.nogopoints.addAll(result);
                }
                params.remove("polylines");
            }
            if (params.containsKey("polygons")) {
                final List<OsmNodeNamed> result = new ArrayList<>();
                parseNogoPolygons(params.get("polygons"), result, true);
                if (rctx.nogopoints == null) {
                    rctx.nogopoints = result;
                } else {
                    rctx.nogopoints.addAll(result);
                }
                params.remove("polygons");
            }

            for (Map.Entry<String, String> e : params.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                if (DEBUG) {
                    System.out.println("params " + key + " " + value);
                }

                if (key.equals("straight")) {
                    try {
                        final String[] sa = value.split(",");
                        for (String s : sa) {
                            final int v = Integer.parseInt(s);
                            if (wplist.size() > v) {
                                wplist.get(v).direct = true;
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("error " + ex.getStackTrace()[0].getLineNumber() + " " + ex.getStackTrace()[0] + "\n" + ex);
                    }
                } else if (key.equals("pois")) {
                    rctx.poipoints = readPoisList(value);
                } else if (key.equals("heading")) {
                    rctx.startDirection = Integer.valueOf(value);
                    rctx.forceUseStartDirection = true;
                } else if (key.equals("direction")) {
                    rctx.startDirection = Integer.valueOf(value);
                } else if (key.equals("alternativeidx")) {
                    rctx.setAlternativeIdx(Integer.parseInt(value));
                } else if (key.equals("turnInstructionMode")) {
                    rctx.turnInstructionMode = Integer.parseInt(value);
                } else if (key.equals("timode")) {
                    rctx.turnInstructionMode = Integer.parseInt(value);
                } else if (key.equals("turnInstructionFormat")) {
                    if ("osmand".equalsIgnoreCase(value)) {
                        rctx.turnInstructionMode = 3;
                    } else if ("locus".equalsIgnoreCase(value)) {
                        rctx.turnInstructionMode = 2;
                    }
                } else if (key.equals("exportWaypoints")) {
                    rctx.exportWaypoints = (Integer.parseInt(value) == 1);
                } else if (key.equals("format")) {
                    rctx.outputFormat = value.toLowerCase(Locale.ROOT);
                } else if (key.equals("trackFormat")) {
                    rctx.outputFormat = value.toLowerCase(Locale.ROOT);
                } else if (key.startsWith("profile:")) {
                    if (rctx.keyValues == null) {
                        rctx.keyValues = new HashMap<>();
                    }
                    rctx.keyValues.put(key.substring(8), value);
                }
                // ignore other params
            }
        }
    }

    /**
     * fill profile parameter list
     *
     * @param rctx    the routing context
     * @param params  the list of parameters
     */
    public void setProfileParams(final RoutingContext rctx, final Map<String, String> params) {
        if (params != null) {
            if (params.isEmpty()) {
                return;
            }
            if (rctx.keyValues == null) {
                rctx.keyValues = new HashMap<>();
            }
            for (Map.Entry<String, String> e : params.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                if (DEBUG) {
                    System.out.println("params " + key + " " + value);
                }
                rctx.keyValues.put(key, value);
            }
        }
    }

    private void parseNogoPolygons(final String polygons, final List<OsmNodeNamed> result, final boolean closed) {
        if (polygons != null) {
            final String[] polygonList = polygons.split("\\|");
            for (String s : polygonList) {
                final String[] lonLatList = s.split(",");
                if (lonLatList.length > 1) {
                    final OsmNogoPolygon polygon = new OsmNogoPolygon(closed);
                    int j;
                    for (j = 0; j < 2 * (lonLatList.length / 2) - 1; ) {
                        final String slon = lonLatList[j++];
                        final String slat = lonLatList[j++];
                        final int lon = (int) ((Double.parseDouble(slon) + 180.) * 1000000. + 0.5);
                        final int lat = (int) ((Double.parseDouble(slat) + 90.) * 1000000. + 0.5);
                        polygon.addVertex(lon, lat);
                    }

                    String nogoWeight = "NaN";
                    if (j < lonLatList.length) {
                        nogoWeight = lonLatList[j];
                    }
                    polygon.nogoWeight = Double.parseDouble(nogoWeight);
                    if (!polygon.points.isEmpty()) {
                        polygon.calcBoundingCircle();
                        result.add(polygon);
                    }
                }
            }
        }
    }

    public List<OsmNodeNamed> readPoisList(final String pois) {
        // lon,lat,name|...
        if (pois == null) {
            return null;
        }

        final String[] lonLatNameList = pois.split("\\|");

        final List<OsmNodeNamed> poisList = new ArrayList<>();
        for (String s : lonLatNameList) {
            final String[] lonLatName = s.split(",");

            if (lonLatName.length != 3) {
                continue;
            }

            final OsmNodeNamed n = new OsmNodeNamed();
            n.ilon = (int) ((Double.parseDouble(lonLatName[0]) + 180.) * 1000000. + 0.5);
            n.ilat = (int) ((Double.parseDouble(lonLatName[1]) + 90.) * 1000000. + 0.5);
            n.name = lonLatName[2];
            poisList.add(n);
        }

        return poisList;
    }

    public List<OsmNodeNamed> readNogoList(final String nogos) {
        // lon,lat,radius[,weight]|...

        if (nogos == null) {
            return null;
        }

        final String[] lonLatRadList = nogos.split("\\|");

        final List<OsmNodeNamed> nogoList = new ArrayList<>();
        for (String s : lonLatRadList) {
            final String[] lonLatRad = s.split(",");
            String nogoWeight = "NaN";
            if (lonLatRad.length > 3) {
                nogoWeight = lonLatRad[3];
            }
            nogoList.add(readNogo(lonLatRad[0], lonLatRad[1], lonLatRad[2], nogoWeight));
        }

        return nogoList;
    }

    public List<OsmNodeNamed> readNogos(final String nogoLons, final String nogoLats, final String nogoRadi) {
        if (nogoLons == null || nogoLats == null || nogoRadi == null) {
            return null;
        }
        final List<OsmNodeNamed> nogoList = new ArrayList<>();

        final String[] lons = nogoLons.split(",");
        final String[] lats = nogoLats.split(",");
        final String[] radi = nogoRadi.split(",");
        final String nogoWeight = "undefined";
        for (int i = 0; i < lons.length && i < lats.length && i < radi.length; i++) {
            final OsmNodeNamed n = readNogo(lons[i].trim(), lats[i].trim(), radi[i].trim(), nogoWeight);
            nogoList.add(n);
        }
        return nogoList;
    }


    private OsmNodeNamed readNogo(final String lon, final String lat, final String radius, final String nogoWeight) {
        final double weight = "undefined".equals(nogoWeight) ? Double.NaN : Double.parseDouble(nogoWeight);
        return readNogo(Double.parseDouble(lon), Double.parseDouble(lat), (int) Double.parseDouble(radius), weight);
    }

    private OsmNodeNamed readNogo(final double lon, final double lat, final int radius, final double nogoWeight) {
        final OsmNodeNamed n = new OsmNodeNamed();
        n.name = "nogo" + radius;
        n.ilon = (int) ((lon + 180.) * 1000000. + 0.5);
        n.ilat = (int) ((lat + 90.) * 1000000. + 0.5);
        n.isNogo = true;
        n.nogoWeight = nogoWeight;
        return n;
    }

}
