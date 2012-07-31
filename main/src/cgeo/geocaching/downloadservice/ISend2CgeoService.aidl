package cgeo.geocaching.downloadservice;

import cgeo.geocaching.downloadservice.ISend2CgeoServiceCallback;

interface ISend2CgeoService {
    void registerStatusCallback(ISend2CgeoServiceCallback cdsc);
    void unregisterStatusCallback(ISend2CgeoServiceCallback cdsc);
}
