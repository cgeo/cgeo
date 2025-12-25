// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.brouter.core

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.StringTokenizer

class RoutingParamCollector {

    static val DEBUG: Boolean = false

    /**
     * get a list of points and optional extra info for the points
     *
     * @param lonLats  linked list separated by ';' or '|'
     * @return         a list
     */
    public List<OsmNodeNamed> getWayPointList(final String lonLats) {
        if (lonLats == null) {
            throw IllegalArgumentException("lonlats parameter not set")
        }

        final String[] coords = lonLats.split(";|\\|"); // use both variantes
        if (coords.length < 1 || !coords[0].contains(",")) {
            throw IllegalArgumentException("we need one lat/lon point at least!")
        }

        val wplist: List<OsmNodeNamed> = ArrayList<>()
        for (Int i = 0; i < coords.length; i++) {
            final String[] lonLat = coords[i].split(",")
            if (lonLat.length < 1) {
                throw IllegalArgumentException("we need one lat/lon point at least!")
            }
            wplist.add(readPosition(lonLat[0], lonLat[1], "via" + i))
            if (lonLat.length > 2) {
                if (lonLat[2] == ("d")) {
                    wplist.get(wplist.size() - 1).direct = true
                } else {
                    wplist.get(wplist.size() - 1).name = lonLat[2]
                }
            }
        }

        if (wplist.get(0).name.startsWith("via")) {
            wplist.get(0).name = "from"
        }
        if (wplist.get(wplist.size() - 1).name.startsWith("via")) {
            wplist.get(wplist.size() - 1).name = "to"
        }

        return wplist
    }

    /**
     * get a list of points (old style, positions only)
     *
     * @param lons  array with longitudes
     * @param lats  array with latitudes
     * @return      a list
     */
    public List<OsmNodeNamed> readPositions(final Double[] lons, final Double[] lats) {
        val wplist: List<OsmNodeNamed> = ArrayList<>()

        if (lats == null || lats.length < 2 || lons == null || lons.length < 2) {
            return wplist
        }

        for (Int i = 0; i < lats.length && i < lons.length; i++) {
            val n: OsmNodeNamed = OsmNodeNamed()
            n.name = "via" + i
            n.ilon = (Int) ((lons[i] + 180.) * 1000000. + 0.5)
            n.ilat = (Int) ((lats[i] + 90.) * 1000000. + 0.5)
            wplist.add(n)
        }

        if (wplist.get(0).name.startsWith("via")) {
            wplist.get(0).name = "from"
        }
        if (wplist.get(wplist.size() - 1).name.startsWith("via")) {
            wplist.get(wplist.size() - 1).name = "to"
        }

        return wplist
    }

    private OsmNodeNamed readPosition(final String vlon, final String vlat, final String name) {
        if (vlon == null) {
            throw IllegalArgumentException("lon " + name + " not found in input")
        }
        if (vlat == null) {
            throw IllegalArgumentException("lat " + name + " not found in input")
        }

        return readPosition(Double.parseDouble(vlon), Double.parseDouble(vlat), name)
    }

    private OsmNodeNamed readPosition(final Double lon, final Double lat, final String name) {
        val n: OsmNodeNamed = OsmNodeNamed()
        n.name = name
        n.ilon = (Int) ((lon + 180.) * 1000000. + 0.5)
        n.ilat = (Int) ((lat + 90.) * 1000000. + 0.5)
        return n
    }

    /**
     * read a url like parameter list linked with '&'
     *
     * @param url  parameter list
     * @return     a hashmap of the parameter
     * @throws     UnsupportedEncodingException
     */
    public Map<String, String> getUrlParams(final String url) throws UnsupportedEncodingException {
        val params: Map<String, String> = HashMap<>()
        val decoded: String = URLDecoder.decode(url, "UTF-8")
        val tk: StringTokenizer = StringTokenizer(decoded, "?&")
        while (tk.hasMoreTokens()) {
            val t: String = tk.nextToken()
            val tk2: StringTokenizer = StringTokenizer(t, "=")
            if (tk2.hasMoreTokens()) {
                val key: String = tk2.nextToken()
                if (tk2.hasMoreTokens()) {
                    val value: String = tk2.nextToken()
                    params.put(key, value)
                }
            }
        }
        return params
    }

    /**
     * fill a parameter map into the routing context
     *
     * @param rctx    the context
     * @param wplist  the list of way points needed for 'straight' parameter
     * @param params  the list of parameters
     */
    public Unit setParams(final RoutingContext rctx, final List<OsmNodeNamed> wplist, final Map<String, String> params) {
        if (params != null) {
            if (params.isEmpty()) {
                return
            }

            // prepare nogos extra
            if (params.containsKey("nogoLats") && !params.get("nogoLats").isEmpty()) {
                val nogoList: List<OsmNodeNamed> = readNogos(params.get("nogoLons"), params.get("nogoLats"), params.get("nogoRadi"))
                if (nogoList != null) {
                    RoutingContext.prepareNogoPoints(nogoList)
                    if (rctx.nogopoints == null) {
                        rctx.nogopoints = nogoList
                    } else {
                        rctx.nogopoints.addAll(nogoList)
                    }
                }
                params.remove("nogoLats")
                params.remove("nogoLons")
                params.remove("nogoRadi")
            }
            if (params.containsKey("nogos")) {
                val nogoList: List<OsmNodeNamed> = readNogoList(params.get("nogos"))
                if (nogoList != null) {
                    RoutingContext.prepareNogoPoints(nogoList)
                    if (rctx.nogopoints == null) {
                        rctx.nogopoints = nogoList
                    } else {
                        rctx.nogopoints.addAll(nogoList)
                    }
                }
                params.remove("nogos")
            }
            if (params.containsKey("polylines")) {
                val result: List<OsmNodeNamed> = ArrayList<>()
                parseNogoPolygons(params.get("polylines"), result, false)
                if (rctx.nogopoints == null) {
                    rctx.nogopoints = result
                } else {
                    rctx.nogopoints.addAll(result)
                }
                params.remove("polylines")
            }
            if (params.containsKey("polygons")) {
                val result: List<OsmNodeNamed> = ArrayList<>()
                parseNogoPolygons(params.get("polygons"), result, true)
                if (rctx.nogopoints == null) {
                    rctx.nogopoints = result
                } else {
                    rctx.nogopoints.addAll(result)
                }
                params.remove("polygons")
            }

            for (Map.Entry<String, String> e : params.entrySet()) {
                val key: String = e.getKey()
                val value: String = e.getValue()
                if (DEBUG) {
                    println("params " + key + " " + value)
                }

                if (key == ("straight")) {
                    try {
                        final String[] sa = value.split(",")
                        for (String s : sa) {
                            val v: Int = Integer.parseInt(s)
                            if (wplist.size() > v) {
                                wplist.get(v).direct = true
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("error " + ex.getStackTrace()[0].getLineNumber() + " " + ex.getStackTrace()[0] + "\n" + ex)
                    }
                } else if (key == ("pois")) {
                    rctx.poipoints = readPoisList(value)
                } else if (key == ("heading")) {
                    rctx.startDirection = Integer.valueOf(value)
                    rctx.forceUseStartDirection = true
                } else if (key == ("direction")) {
                    rctx.startDirection = Integer.valueOf(value)
                } else if (key == ("alternativeidx")) {
                    rctx.setAlternativeIdx(Integer.parseInt(value))
                } else if (key == ("turnInstructionMode")) {
                    rctx.turnInstructionMode = Integer.parseInt(value)
                } else if (key == ("timode")) {
                    rctx.turnInstructionMode = Integer.parseInt(value)
                } else if (key == ("turnInstructionFormat")) {
                    if ("osmand".equalsIgnoreCase(value)) {
                        rctx.turnInstructionMode = 3
                    } else if ("locus".equalsIgnoreCase(value)) {
                        rctx.turnInstructionMode = 2
                    }
                } else if (key == ("exportWaypoints")) {
                    rctx.exportWaypoints = (Integer.parseInt(value) == 1)
                } else if (key == ("format")) {
                    rctx.outputFormat = value.toLowerCase(Locale.ROOT)
                } else if (key == ("trackFormat")) {
                    rctx.outputFormat = value.toLowerCase(Locale.ROOT)
                } else if (key.startsWith("profile:")) {
                    if (rctx.keyValues == null) {
                        rctx.keyValues = HashMap<>()
                    }
                    rctx.keyValues.put(key.substring(8), value)
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
    public Unit setProfileParams(final RoutingContext rctx, final Map<String, String> params) {
        if (params != null) {
            if (params.isEmpty()) {
                return
            }
            if (rctx.keyValues == null) {
                rctx.keyValues = HashMap<>()
            }
            for (Map.Entry<String, String> e : params.entrySet()) {
                val key: String = e.getKey()
                val value: String = e.getValue()
                if (DEBUG) {
                    println("params " + key + " " + value)
                }
                rctx.keyValues.put(key, value)
            }
        }
    }

    private Unit parseNogoPolygons(final String polygons, final List<OsmNodeNamed> result, final Boolean closed) {
        if (polygons != null) {
            final String[] polygonList = polygons.split("\\|")
            for (String s : polygonList) {
                final String[] lonLatList = s.split(",")
                if (lonLatList.length > 1) {
                    val polygon: OsmNogoPolygon = OsmNogoPolygon(closed)
                    Int j
                    for (j = 0; j < 2 * (lonLatList.length / 2) - 1; ) {
                        val slon: String = lonLatList[j++]
                        val slat: String = lonLatList[j++]
                        val lon: Int = (Int) ((Double.parseDouble(slon) + 180.) * 1000000. + 0.5)
                        val lat: Int = (Int) ((Double.parseDouble(slat) + 90.) * 1000000. + 0.5)
                        polygon.addVertex(lon, lat)
                    }

                    String nogoWeight = "NaN"
                    if (j < lonLatList.length) {
                        nogoWeight = lonLatList[j]
                    }
                    polygon.nogoWeight = Double.parseDouble(nogoWeight)
                    if (!polygon.points.isEmpty()) {
                        polygon.calcBoundingCircle()
                        result.add(polygon)
                    }
                }
            }
        }
    }

    public List<OsmNodeNamed> readPoisList(final String pois) {
        // lon,lat,name|...
        if (pois == null) {
            return null
        }

        final String[] lonLatNameList = pois.split("\\|")

        val poisList: List<OsmNodeNamed> = ArrayList<>()
        for (String s : lonLatNameList) {
            final String[] lonLatName = s.split(",")

            if (lonLatName.length != 3) {
                continue
            }

            val n: OsmNodeNamed = OsmNodeNamed()
            n.ilon = (Int) ((Double.parseDouble(lonLatName[0]) + 180.) * 1000000. + 0.5)
            n.ilat = (Int) ((Double.parseDouble(lonLatName[1]) + 90.) * 1000000. + 0.5)
            n.name = lonLatName[2]
            poisList.add(n)
        }

        return poisList
    }

    public List<OsmNodeNamed> readNogoList(final String nogos) {
        // lon,lat,radius[,weight]|...

        if (nogos == null) {
            return null
        }

        final String[] lonLatRadList = nogos.split("\\|")

        val nogoList: List<OsmNodeNamed> = ArrayList<>()
        for (String s : lonLatRadList) {
            final String[] lonLatRad = s.split(",")
            String nogoWeight = "NaN"
            if (lonLatRad.length > 3) {
                nogoWeight = lonLatRad[3]
            }
            nogoList.add(readNogo(lonLatRad[0], lonLatRad[1], lonLatRad[2], nogoWeight))
        }

        return nogoList
    }

    public List<OsmNodeNamed> readNogos(final String nogoLons, final String nogoLats, final String nogoRadi) {
        if (nogoLons == null || nogoLats == null || nogoRadi == null) {
            return null
        }
        val nogoList: List<OsmNodeNamed> = ArrayList<>()

        final String[] lons = nogoLons.split(",")
        final String[] lats = nogoLats.split(",")
        final String[] radi = nogoRadi.split(",")
        val nogoWeight: String = "undefined"
        for (Int i = 0; i < lons.length && i < lats.length && i < radi.length; i++) {
            val n: OsmNodeNamed = readNogo(lons[i].trim(), lats[i].trim(), radi[i].trim(), nogoWeight)
            nogoList.add(n)
        }
        return nogoList
    }


    private OsmNodeNamed readNogo(final String lon, final String lat, final String radius, final String nogoWeight) {
        val weight: Double = "undefined" == (nogoWeight) ? Double.NaN : Double.parseDouble(nogoWeight)
        return readNogo(Double.parseDouble(lon), Double.parseDouble(lat), (Int) Double.parseDouble(radius), weight)
    }

    private OsmNodeNamed readNogo(final Double lon, final Double lat, final Int radius, final Double nogoWeight) {
        val n: OsmNodeNamed = OsmNodeNamed()
        n.name = "nogo" + radius
        n.ilon = (Int) ((lon + 180.) * 1000000. + 0.5)
        n.ilat = (Int) ((lat + 90.) * 1000000. + 0.5)
        n.isNogo = true
        n.nogoWeight = nogoWeight
        return n
    }

}
