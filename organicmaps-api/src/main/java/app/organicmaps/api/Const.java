/*
 Copyright (c) 2022, Organic Maps OÜ. All rights reserved.

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
 */
package app.organicmaps.api;

public class Const
{
  // Common
  static final String API_SCHEME = "om://";
  static final String AUTHORITY = "app.organicmaps.api";
  static final String EXTRA_PREFIX = AUTHORITY + ".extra";

  // Request extras
  public static final String EXTRA_PICK_POINT = EXTRA_PREFIX + ".PICK_POINT";

  // Response extras
  public static final String EXTRA_POINT_NAME = EXTRA_PREFIX + ".POINT_NAME";
  public static final String EXTRA_POINT_LAT = EXTRA_PREFIX + ".POINT_LAT";
  public static final String EXTRA_POINT_LON = EXTRA_PREFIX + ".POINT_LON";
  public static final String EXTRA_POINT_ID = EXTRA_PREFIX + ".POINT_ID";
  public static final String EXTRA_ZOOM_LEVEL = EXTRA_PREFIX + ".ZOOM_LEVEL";

  private Const() {}
}
