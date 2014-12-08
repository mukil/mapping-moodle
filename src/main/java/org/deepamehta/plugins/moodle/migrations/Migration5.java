package org.deepamehta.plugins.moodle.migrations;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.ResultList;
import java.util.logging.Logger;
import org.deepamehta.plugins.moodle.MoodleServiceClient;

/** 
 * This migration removes the initially introduced Topic Types
 * "Usage Report" and "Moodle Service URL" and changes the Workspace name to "Moodle".
 */
public class Migration5 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());
    
    final String MOODLE_SERVICE_URL_TYPE =  "org.deepamehta.moodle.web_service_url";
    final String USAGE_TYPE_URI = "org.deepamehta.moodle.usage_report";
    
    @Override
    public void run() {

        // 1) Delete all instance of usage reports
        ResultList<RelatedTopic> usage_reports = dms.getTopics(USAGE_TYPE_URI, 0);
        for (int i=0; i < usage_reports.getItems().size(); i++) {
            RelatedTopic usage_report = usage_reports.getItems().get(i);
            logger.info("Migration 5 deleting \"Moodle Usage Report\"-Topic " + usage_report.getId());
            dms.deleteTopic(usage_report.getId());
        }
        // 2) Delete "Usage Report"-Type
        dms.getTopicType(USAGE_TYPE_URI).delete();
        logger.info("Migration 5 deleted \"Moodle Usage Report\"-TopicType");
        //
        // 3) Delete all instances of type to be deleted
        ResultList<RelatedTopic> service_urls = dms.getTopics(MOODLE_SERVICE_URL_TYPE, 0);
        for (int i=0; i < service_urls.getItems().size(); i++) {
            RelatedTopic service_url = service_urls.getItems().get(i);
            logger.info("Migration 5 deleting \"Moodle Web Service URL\"-Topic " + service_url.getId());
            dms.deleteTopic(service_url.getId());
        }
        // 4) Delete "Moodle Service URL"-Type
        dms.getTopicType(MOODLE_SERVICE_URL_TYPE).delete();
        // logger.info("Migration 5 deleted \"Moodle Web Service URL\"-TopicType");
        // 5) Fix Moodle WS-Name Topic
        Topic moodle_ws = dms.getTopic("uri", new SimpleValue("org.deepamehta.workspaces.moodle")).loadChildTopics();
        moodle_ws.getChildTopics().set("dm4.workspaces.name", MoodleServiceClient.WS_MOODLE_NAME);
    }

}
