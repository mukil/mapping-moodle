package org.deepamehta.plugins.moodle.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.Migration;
import java.util.logging.Logger;

public class Migration2 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    private String MOODLE_CONFIG = "org.deepamehta.moodle.web_service_url";
    // private String MOODLE_ITEM = "org.deepamehta.moodle.item";
    private String WS_DEFAULT_URI = "de.workspaces.deepamehta";

    @Override
    public void run() {

        // 1) Assign new type "Moodle Config" to our default workspace
        TopicType moodleConfig = dms.getTopicType(MOODLE_CONFIG);
        assignWorkspace(moodleConfig);

        /** 2) Assign new type "Moodle Item" to our default workspace
        TopicType moodleItem = dms.getTopicType(MOODLE_ITEM);
        assignWorkspace(moodleItem); **/

        // moodleItem.getViewConfig().addSetting("dm4.core.view_configuration", "dm4.core.locked", true);

    }

    // === Workspace ===

    private void assignWorkspace(Topic topic) {
        if (hasWorkspace(topic)) {
            return;
        }
        Topic defaultWorkspace = dms.getTopic("uri", new SimpleValue(WS_DEFAULT_URI), false);
        dms.createAssociation(new AssociationModel("dm4.core.aggregation",
            new TopicRoleModel(topic.getId(), "dm4.core.parent"),
            new TopicRoleModel(defaultWorkspace.getId(), "dm4.core.child")
        ), null);
    }

    private boolean hasWorkspace(Topic topic) {
        return topic.getRelatedTopics("dm4.core.aggregation", "dm4.core.parent", "dm4.core.child",
            "dm4.workspaces.workspace", false, false, 0).getSize() > 0;
    }

}