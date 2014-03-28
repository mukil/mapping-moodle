package org.deepamehta.plugins.moodle.migrations;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.ResultList;
import java.util.logging.Logger;
import org.deepamehta.plugins.moodle.MoodleServiceClient;

public class Migration5 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void run() {

        String usage_type_uri = "org.deepamehta.moodle.usage_report";
        // 1) Delete all instance of usage reports
        ResultList<RelatedTopic> usage_reports = dms.getTopics(usage_type_uri, false, 0);
        for (int i=0; i < usage_reports.getItems().size(); i++) {
            RelatedTopic usage_report = usage_reports.getItems().get(i);
            logger.info("Migration 5 deleting \"Moodle Usage Report\"-Topic " + usage_report.getId());
            dms.deleteTopic(usage_report.getId());
        }
        // 2) Delete "Usage Report"-Type
        dms.getTopicType(usage_type_uri).delete(new Directives());
        logger.info("Migration 5 deleted \"Moodle Usage Report\"-TopicType");
        //
        String moodle_config_type_uri =  "org.deepamehta.moodle.web_service_url";
        // 3) Delete all instances of type to be deleted
        ResultList<RelatedTopic> service_urls = dms.getTopics(moodle_config_type_uri, false, 0);
        for (int i=0; i < service_urls.getItems().size(); i++) {
            RelatedTopic service_url = service_urls.getItems().get(i);
            logger.info("Migration 5 deleting \"Moodle Web Service URL\"-Topic " + service_url.getId());
            dms.deleteTopic(service_url.getId());
        }
        // 4) Delete "Moodle Service URL"-Type
        dms.getTopicType(moodle_config_type_uri).delete(new Directives());
        // logger.info("Migration 5 deleted \"Moodle Web Service URL\"-TopicType");
        // 5) Fix Moodle WS-Name Topic
        Topic moodle_ws = dms.getTopic("uri", new SimpleValue("org.deepamehta.workspaces.moodle"), true);
            moodle_ws.getCompositeValue().set("dm4.workspaces.name", MoodleServiceClient.WS_MOODLE_NAME,
                    null, new Directives());
    }

}
