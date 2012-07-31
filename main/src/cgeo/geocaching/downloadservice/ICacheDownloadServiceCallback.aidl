package cgeo.geocaching.downloadservice;

interface ICacheDownloadServiceCallback {
    void notifyRefresh(); 
    void notifyFinish();
}
