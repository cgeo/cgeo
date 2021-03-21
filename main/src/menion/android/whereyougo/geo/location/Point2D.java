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

package menion.android.whereyougo.geo.location;

/**
 * This class implements a generic point in 2D Cartesian space. The storage representation is left
 * up to the subclass. Point includes two useful nested classes, for float and double storage
 * respectively.
 */
public abstract class Point2D {
    /**
     * The default constructor.
     *
     */
    Point2D() {
    }

    /**
     * Return the distance between two points.
     *
     * @param x1 the x coordinate of point 1
     * @param y1 the y coordinate of point 1
     * @param x2 the x coordinate of point 2
     * @param y2 the y coordinate of point 2
     * @return the distance from (x1,y1) to (x2,y2)
     */
    public static double distance(double x1, double y1, double x2, double y2) {
      return Math.sqrt(distanceSq(x1, y1, x2, y2));
    }

    /**
     * Return the square of the distance between two points.
     *
     * @param x1 the x coordinate of point 1
     * @param y1 the y coordinate of point 1
     * @param x2 the x coordinate of point 2
     * @param y2 the y coordinate of point 2
     * @return (x2 - x1)^2 + (y2 - y1)^2
     */
    public static double distanceSq(double x1, double y1, double x2, double y2) {
      x2 -= x1;
      y2 -= y1;
      return x2 * x2 + y2 * y2;
    }

    /**
     * Return the distance from this point to the given one.
     *
     * @param x the x coordinate of the other point
     * @param y the y coordinate of the other point
     * @return the distance
     */
    public double distance(double x, double y) {
      return distance(getX(), getY(), x, y);
    }

    /**
     * Return the distance from this point to the given one.
     *
     * @param p the other point
     * @return the distance
     * @throws NullPointerException if p is null
     */
    public double distance(Point2D p) {
      return distance(getX(), getY(), p.getX(), p.getY());
    }

    /**
     * Return the square of the distance from this point to the given one.
     *
     * @param x the x coordinate of the other point
     * @param y the y coordinate of the other point
     * @return the square of the distance
     */
    public double distanceSq(double x, double y) {
      return distanceSq(getX(), getY(), x, y);
    }

    /**
     * Return the square of the distance from this point to the given one.
     *
     * @param p the other point
     * @return the square of the distance
     * @throws NullPointerException if p is null
     */
    public double distanceSq(Point2D p) {
      return distanceSq(getX(), getY(), p.getX(), p.getY());
    }

    /**
     * Compares two points for equality. This returns true if they have the same coordinates.
     *
     * @param o the point to compare
     * @return true if it is equal
     */
    public boolean equals(Object o) {
      if (!(o instanceof Point2D))
        return false;
      Point2D p = (Point2D) o;
      return getX() == p.getX() && getY() == p.getY();
    }

    /**
     * Get the X coordinate, in double precision.
     *
     * @return the x coordinate
     */
    public abstract double getX();

  /**
   * Get the Y coordinate, in double precision.
   *
   * @return the y coordinate
   */
  public abstract double getY();

  /**
   * Return the hashcode for this point. The formula is not documented, but appears to be the same
   * as:
   * <p/>
   * <pre>
   * long l = Double.doubleToLongBits(getY());
   * l = l * 31 &circ; Double.doubleToLongBits(getX());
   * return (int) ((l &gt;&gt; 32) &circ; l);
   * </pre>
   *
   * @return the hashcode
   */
  public int hashCode() {
    // Talk about a fun time reverse engineering this one!
    long l = java.lang.Double.doubleToLongBits(getY());
    l = l * 31 ^ java.lang.Double.doubleToLongBits(getX());
    return (int) ((l >> 32) ^ l);
  }

  /**
   * Set the location of this point to the new coordinates. There may be a loss of precision.
   *
   * @param x the new x coordinate
   * @param y the new y coordinate
   */
  public abstract void setLocation(double x, double y);

  /**
   * Set the location of this point to the new coordinates. There may be a loss of precision.
   *
   * @param p the point to copy
   * @throws NullPointerException if p is null
   */
  public void setLocation(Point2D p) {
    setLocation(p.getX(), p.getY());
  }

  public String toString() {
    return "[ X: " + getX() + " Y: " + getY() + " ]";
  }

  /**
   * This class defines a point in <code>int</code> precision.
   *
   * @author Eric Blake (ebb9@email.byu.edu)
   * @since 1.2
   */
  public static class Int extends Point2D {
    /**
     * The X coordinate.
     */
    public int x;

    /**
     * The Y coordinate.
     */
    public int y;

    /**
     * Create a new point at (0,0).
     */
    public Int() {
    }

    /**
     * Create a new point at (x,y).
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public Int(int x, int y) {
      this.x = x;
      this.y = y;
    }

    /**
     * Return the x coordinate.
     *
     * @return the x coordinate
     */
    public double getX() {
      return x;
    }

    /**
     * Return the y coordinate.
     *
     * @return the y coordinate
     */
    public double getY() {
      return y;
    }

    /**
     * Sets the location of this point.
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     */
    public void setLocation(double x, double y) {
      this.x = (int) x;
      this.y = (int) y;
    }

    /**
     * Sets the location of this point.
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     */
    public void setLocation(int x, int y) {
      this.x = x;
      this.y = y;
    }

    /**
     * Returns a string representation of this object. The format is:
     * <code>"Point2D.int[" + x + ", " + y + ']'</code>.
     *
     * @return a string representation of this object
     */
    public String toString() {
      return "Point2D.int[" + x + ", " + y + ']';
    }
  } // class int
} // class Point2D
