package com.centurylink.mdw.activity.types;

import com.centurylink.mdw.java.JavaExecutor;
import com.centurylink.mdw.java.MdwJavaException;

/**
 * Activity type for dynamic Java.
 */
public interface JavaActivity extends GeneralActivity {
    JavaExecutor getExecutorInstance() throws MdwJavaException;
}
