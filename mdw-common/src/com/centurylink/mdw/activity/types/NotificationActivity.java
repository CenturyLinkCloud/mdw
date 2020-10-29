package com.centurylink.mdw.activity.types;

import com.centurylink.mdw.activity.ActivityException;

public interface NotificationActivity extends GeneralActivity {

    void sendNotices() throws ActivityException;
}
