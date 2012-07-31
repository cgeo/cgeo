package cgeo.geocaching.downloadservice;

import cgeo.geocaching.downloadservice.ICacheDownloadServiceCallback;

interface ICacheDownloadService {
    int queueStatus();
    
    String actualDownload();
    
    String[] queuedCodes();
    
    void flushQueueAndStopService();
    
    void pauseDownloading();
    
    void resumeDownloading();
    
    void removeFromQueue(String geocode);
    
    void registerStatusCallback(ICacheDownloadServiceCallback cdsc);
    
    void unregisterStatusCallback(ICacheDownloadServiceCallback cdsc);
}
