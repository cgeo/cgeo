/******************************************************************************
   Copyright (c) 2013, MapsWithMe GmbH All rights reserved.

  Redistribution and use in source and binary forms, with or without modification,
  are permitted provided that the following conditions are met:

  Redistributions of source code must retain the above copyright notice, this list
  of conditions and the following disclaimer. Redistributions in binary form must
  reproduce the above copyright notice, this list of conditions and the following
  disclaimer in the documentation and/or other materials provided with the
  distribution. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
  CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
  OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
  IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
  OF SUCH DAMAGE.
******************************************************************************/
package com.mapswithme.maps.api;

import java.io.Serializable;

/**
 * POI wrapper object.
 * Has its <code>equals()</code> and <code>hashCode()</code> methods overloaded
 * so could be used in Hash(Map/Set/etc) classes.
 */
public final class MWMPoint implements Serializable
{
  private static final long serialVersionUID = 1L;

  final private double mLat;
  final private double mLon;
  final private String mName;
  private String mId;
  private Style mStyle;

  public MWMPoint(double lat, double lon, String name)
  {
    this(lat, lon, name, null);
  }

  public MWMPoint(double lat, double lon, String name, String id)
  {
    this.mLat = lat;
    this.mLon = lon;
    this.mName = name;
    this.mId = id;
  }

  public MWMPoint(double lat, double lon, String name, String id, Style style)
  {
    this.mLat = lat;
    this.mLon = lon;
    this.mName = name;
    this.mId = id;
    this.mStyle = style;
  }

  public double getLat()       { return mLat;   }
  public double getLon()       { return mLon;   }
  public String getName()      { return mName;  }
  public String getId()        { return mId;    }
  public Style  getStyle()     { return mStyle; }

  public String getStyleForUrl() { return (mStyle == null) ? null : mStyle.getName(); }

  /**
   * Sets string ID for this point. Internally it is not used to distinguish point,
   * it's purpose to help clients code to associate point with domain objects of their application.
   * @param id
   */
  public void setId(String id) { mId = id; }

  /**
   * Sets the style (appearance) for this point.
   *
   * @param style Style to use, or null for default (violet circle).
   */
  public void setStyle(Style style)
  {
    this.mStyle = style;
  }

  @Override
  public String toString()
  {
    return "MWMPoint [lat=" + mLat +
            ", lon=" + mLon +
            ", name=" + mName +
            ", id=" + mId +
            ", style=" + mStyle + "]";
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(mLat);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(mLon);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((mName == null) ? 0 : mName.hashCode());
    return result;
  }

  /**
   *  Two point are considered
   *  equal if they have they lat, lon, and name attributes equal.
   */
  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final MWMPoint other = (MWMPoint) obj;
    if (Double.doubleToLongBits(mLat) != Double.doubleToLongBits(other.mLat))
      return false;
    if (Double.doubleToLongBits(mLon) != Double.doubleToLongBits(other.mLon))
      return false;

    return mName == null ? other.mName == null : mName.equals(other.mName);
  }

  /**
   * Supported styles for MAPS.ME. Each appears as a small flag of the appropriate colour.
   */
  public enum Style
  {
    PlacemarkRed("placemark-red"),
    PlacemarkBlue("placemark-blue"),
    PlacemarkPurple("placemark-purple"),
    PlacemarkYellow("placemark-yellow"),
    PlacemarkPink("placemark-pink"),
    PlacemarkBrown("placemark-brown"),
    PlacemarkGreen("placemark-green"),
    PlacemarkOrange("placemark-orange");

    private String name;

    private Style(String name)
    {
      this.name = name;
    }

    /**
     * @return name as it should appear in the MAPS.ME URL.
     */
    private String getName()
    {
      return name;
    }
  }
}
