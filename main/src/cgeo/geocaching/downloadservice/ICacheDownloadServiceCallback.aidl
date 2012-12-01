package cgeo.geocaching.downloadservice;

interface ICacheDownloadServiceCallback {
    void notifyRefresh(int queueSize); 
    void notifyFinish();
}
