package cgeo.geocaching.service;

import androidx.core.app.NotificationCompat;

public interface IAbstractForgreoundIntentService {
    NotificationCompat.Builder createInitialNotification();
}
