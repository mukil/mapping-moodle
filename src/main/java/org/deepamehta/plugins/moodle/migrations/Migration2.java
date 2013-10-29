package org.deepamehta.plugins.moodle.migrations;

import de.deepamehta.core.service.Migration;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.*;
import java.util.logging.Logger;

public class Migration2 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    private String MOODLE_CONFIG = "org.deepamehta.moodle.web_service_url";
    private String MOODLE_ITEM = "org.deepamehta.moodle.item";
    private String DEEPAMEHTA_FILE_ITEM = "dm4.files.file";
    private String MOODLE_FILE_URL = "org.deepamehta.moodle.fileurl";

    private String WS_DEFAULT_URI = "de.workspaces.deepamehta";

    @Override
    public void run() {

        // 1) Assign new type "Moodle Config" to our default workspace
        TopicType moodleConfig = dms.getTopicType(MOODLE_CONFIG, null);
        assignWorkspace(moodleConfig);

        // 2) Assign new type "Moodle Item" to our default workspace
        TopicType moodleItem = dms.getTopicType(MOODLE_ITEM, null);
        assignWorkspace(moodleItem);

        // 3) Assign moodle file extension to type \"File\"
        TopicType fileType = dms.getTopicType(DEEPAMEHTA_FILE_ITEM, null);
        fileType.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def", DEEPAMEHTA_FILE_ITEM,
                MOODLE_FILE_URL, "dm4.core.one", "dm4.core.one"));
        // moodleItem.getViewConfig().addSetting("dm4.core.view_configuration", "dm4.core.locked", true);

    }

    // === Workspace ===

    private void assignWorkspace(Topic topic) {
        if (hasWorkspace(topic)) {
            return;
        }
        Topic defaultWorkspace = dms.getTopic("uri", new SimpleValue(WS_DEFAULT_URI), false, null);
        dms.createAssociation(new AssociationModel("dm4.core.aggregation",
            new TopicRoleModel(topic.getId(), "dm4.core.parent"),
            new TopicRoleModel(defaultWorkspace.getId(), "dm4.core.child")
        ), null);
    }

    private boolean hasWorkspace(Topic topic) {
        return topic.getRelatedTopics("dm4.core.aggregation", "dm4.core.parent", "dm4.core.child",
            "dm4.workspaces.workspace", false, false, 0, null).getSize() > 0;
    }

}