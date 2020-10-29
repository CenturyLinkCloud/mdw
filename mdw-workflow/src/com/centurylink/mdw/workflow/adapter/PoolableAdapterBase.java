package com.centurylink.mdw.workflow.adapter;

import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;

/**
 * @deprecated Extend {@link TextAdapterActivity} whose name better
 * describes its purpose.  Adapter poolability is no longer a concept
 * in MDW as we rely on underlying protocol pooling mechanisms.
 */
@Deprecated
@Tracked(LogLevel.TRACE)
public abstract class PoolableAdapterBase extends TextAdapterActivity {

}
