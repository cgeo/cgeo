package cgeo.geocaching.downloadservice;

interface ISend2CgeoServiceCallback {         
    void notifySend2CgeoStatus(int status, String geocode);
}