package org.deepamehta.plugins.moodle.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Migration;
import java.util.logging.Logger;
import org.deepamehta.plugins.moodle.MoodleServiceClient;

public class Migration5 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void run() {

        // 1) Delete "Usage Report"-Type
        String usage_type_uri = "org.deepamehta.moodle.usage_report";
        dms.getTopic("uri", new SimpleValue(usage_type_uri), true).delete(new Directives());
        // 2) Delete "Moodle Config: .. "-Type
        String moodle_config_type_uri =  "org.deepamehta.moodle.web_service_url";
        dms.getTopic("uri", new SimpleValue(moodle_config_type_uri), true).delete(new Directives());
        // 3) Fix Moodle WS-Name Topic
        Topic moodle_ws = dms.getTopic("uri", new SimpleValue("org.deepamehta.workspaces.moodle"), true);
            moodle_ws.getCompositeValue().set("dm4.workspaces.name", MoodleServiceClient.WS_MOODLE_NAME,
                    null, new Directives());
    }

}
