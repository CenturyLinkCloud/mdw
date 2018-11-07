package com.centurylink.mdw.base;

import com.centurylink.mdw.activity.types.SuspendibleActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.workflow.activity.event.EventWaitActivity;

import java.util.List;

@Activity(value="Process Pause", icon="shape:pause",
        category=com.centurylink.mdw.activity.types.PauseActivity.class,
        pagelet="{\n" +
                "  \"widgets\": [\n" +
                "    {\n" +
                "      \"name\": \"SLA\",\n" +
                "      \"label\": \"Optional Timeout\",\n" +
                "      \"type\": \"datetime\",\n" +
                "      \"units\": \"Minutes,Hours,Days\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"STATUS_AFTER_TIMEOUT\",\n" +
                "      \"label\": \"Status After Timeout\",\n" +
                "      \"type\": \"dropdown\",\n" +
                "      \"default\": \"Cancelled\",\n" +
                "      \"options\": [\"Cancelled\", \"Hold\", \"Waiting\"],\n" +
                "      \"vx\": \"160\", \"vw\": \"120\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"WAIT_EVENT_NAMES\",\n" +
                "      \"label\": \"Events\",\n" +
                "      \"type\": \"table\",\n" +
                "      \"section\": \"Events\",\n" +
                "      \"widgets\": [\n" +
                "        {\n" +
                "          \"name\": \"Event Name\",\n" +
                "          \"label\": \"Event Name\",\n" +
                "          \"type\": \"text\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Completion Code\",\n" +
                "          \"label\": \"Completion Code\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"STATUS_AFTER_EVENT\",\n" +
                "      \"label\": \"Status After Event\",\n" +
                "      \"type\": \"dropdown\",\n" +
                "      \"default\": \"Cancelled\",\n" +
                "      \"options\": [\"Cancelled\", \"Hold\", \"Waiting\"],\n" +
                "      \"section\": \"Events\",\n" +
                "      \"vx\": \"160\", \"vw\": \"120\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"Pause Activity Help\",\n" +
                "      \"type\": \"link\",\n" +
                "      \"url\": \"help/PauseActivity.html\"\n" +
                "    }\n" +
                "  ]\n" +
                "}")
public class PauseActivity extends EventWaitActivity implements SuspendibleActivity {

    @Override
    public List<String[]> getWaitEventSpecs() {
        List<String[]> specs = super.getWaitEventSpecs();
        specs.add(new String[] { "mdw.Resume-" + getActivityInstanceId(), "", null});
        return specs;
    }

    @Override
    protected final boolean isEventRecurring(String completionCode) {
        return false;
    }

}
