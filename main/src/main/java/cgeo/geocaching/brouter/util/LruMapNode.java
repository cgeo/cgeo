package cgeo.geocaching.brouter.util;

public abstract class LruMapNode {
    public int hash;
    public LruMapNode nextInBin; // next entry for hash-bin
    public LruMapNode next; // next in lru sequence (towards mru)
    public LruMapNode previous; // previous in lru sequence (towards lru)
}
