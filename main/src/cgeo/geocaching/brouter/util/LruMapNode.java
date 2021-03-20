package cgeo.geocaching.brouter.util;

public abstract class LruMapNode {
    public int hash;
    LruMapNode nextInBin; // next entry for hash-bin
    LruMapNode next; // next in lru sequence (towards mru)
    LruMapNode previous; // previous in lru sequence (towards lru)
}
