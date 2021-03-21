/*
 * This file is part of WhereYouGo.
 * 
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package menion.android.whereyougo.gui.extension;


import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import menion.android.whereyougo.geo.location.Location;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class DataInfo implements Comparable<DataInfo> {

    private static final String TAG = "DataInfo";
    public double value01;
    public double value02;
    /*
     * USAGE: - DbWaypoints to store GeocachingSimpleData object in DataInfo list
     */
    public Object addData01;
    public Object addData02;
    public boolean enabled = true;
    private int id;
    private String name;
    private String description;
    private int image;
    private Drawable imageD;
    private Bitmap imageB;
    private Bitmap imageRight;
    private double distance = -1;
    private double azimuth = -1;

    public DataInfo(DataInfo con) {
        this.id = con.id;
        this.name = con.name;
        this.description = con.description;
        this.image = con.image;
        this.imageD = con.imageD;
        this.imageB = con.imageB;
        this.imageRight = con.imageRight;
        this.value01 = con.value01;
        this.value02 = con.value02;
        this.distance = con.distance;
        this.addData01 = con.addData01;
    }

    public DataInfo(int id, String name) {
        this(id, name, "", -1);
    }

    public DataInfo(int id, String name, Bitmap image) {
        this(id, name, "", image);
    }

    public DataInfo(int id, String name, String desc) {
        this(id, name, desc, -1);
    }

    public DataInfo(int id, String name, String description, Bitmap imageB) {
        setBasics(id, name, description);
        this.imageB = imageB;
    }

    public DataInfo(int id, String name, String description, Drawable imageD) {
        setBasics(id, name, description);
        this.imageD = imageD;
    }

    public DataInfo(int id, String name, String description, int image) {
        setBasics(id, name, description);
        this.image = image;
    }

    public DataInfo(String name) {
        this(-1, name, "", -1);
    }

    public DataInfo(String name, String description) {
        this(-1, name, description, -1);
    }

    public DataInfo(String name, String description, Bitmap image) {
        this(-1, name, description, image);
    }

    public DataInfo(String name, String description, Drawable image) {
        this(-1, name, description, image);
    }

    public DataInfo(String name, String description, int image) {
        this(-1, name, description, image);
    }

    public DataInfo(String name, String description, Object addData01) {
        this(-1, name, description, -1);
        this.addData01 = addData01;
    }

    public void addDescription(String desc) {
        if (description == null || description.length() == 0)
            description = desc;
        else
            description += ", " + desc;
    }

    public void clearDistAzi() {
        distance = -1;
    }

    public int compareTo(DataInfo another) {
        return name.compareTo(another.getName());
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public void setImage(Bitmap imageB) {
        this.imageB = imageB;
    }

    public Bitmap getImageB() {
        return imageB;
    }

    public Drawable getImageD() {
        return imageD;
    }

    public Bitmap getImageRight() {
        return imageRight;
    }

    public DataInfo setImageRight(Bitmap image) {
        this.imageRight = image;
        return this;
    }

    public Location getLocation() {
        Location loc = new Location(TAG);
        loc.setLatitude(value01);
        loc.setLongitude(value02);
        return loc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDistAziSet() {
        return distance != -1;
    }

    public DataInfo setAddData01(Object data) {
        this.addData01 = data;
        return this;
    }

    private void setBasics(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.image = -1;
        this.imageD = null;
        this.imageB = null;
        this.imageRight = null;
    }

    public void setCoordinates(double lat, double lon) {
        this.value01 = lat;
        this.value02 = lon;
    }

    public void setDistAzi(float dist, float azi) {
        distance = dist;
        azimuth = azi;
    }

    public void setDistAzi(Location refLocation) {
        Location loc = getLocation();
        distance = refLocation.distanceTo(loc);
        azimuth = refLocation.bearingTo(loc);
    }

    public String toString() {
        return getName();
    }
}
