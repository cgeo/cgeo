package cgeo.geocaching.brouter.codec;


public interface TagValueValidator {
    /**
     * @param tagValueSet the way description to check
     * @return 0 = nothing, 1=no matching, 2=normal
     */
    int accessType(byte[] tagValueSet);

    byte[] unify(byte[] tagValueSet, int offset, int len);

    boolean isLookupIdxUsed(int idx);

    void setDecodeForbidden(boolean decodeForbidden);
}
