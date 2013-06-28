package org.deepamehta.plugins.moodle.migrations;

import de.deepamehta.core.service.Migration;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.*;
import java.util.logging.Logger;

public class Migration2 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    private String MOODLE_SECURITY_KEY_URI = "org.deepamehta.moodle.security_key";
    private String MOODLE_USER_ID_URI = "org.deepamehta.moodle.user_id";
    private String MOODLE_CONFIG = "org.deepamehta.moodle.web_service_url";
    private String MOODLE_ITEM = "org.deepamehta.moodle.item";
    private String MOODLE_SECTION = "org.deepamehta.moodle.course";
    private String MOODLE_COURSE = "org.deepamehta.moodle.section";

    // private String RESOURCE_URI = "org.deepamehta.resources.resource";
    // private String FILE_URI = "dm4.files.file";

    private String WS_DEFAULT_URI = "de.workspaces.deepamehta";
    private String DEEPAMEHTA_USER_URI = "dm4.accesscontrol.user_account";

    @Override
    public void run() {

        TopicType user = dms.getTopicType(DEEPAMEHTA_USER_URI, null);

        // 1) Enrich the "User"-Type about a "Moodle Security Token" and "Moodle User ID"
        user.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def", DEEPAMEHTA_USER_URI,
                MOODLE_SECURITY_KEY_URI, "dm4.core.one", "dm4.core.one"));
        user.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def", DEEPAMEHTA_USER_URI,
                MOODLE_USER_ID_URI, "dm4.core.one", "dm4.core.one"));

        // 2) Assign new type "Moodle Security Token" to our default workspace
        TopicType moodleToken = dms.getTopicType(MOODLE_SECURITY_KEY_URI, null);
        assignWorkspace(moodleToken);

        // 3) Assign new type "Moodle Config" to our default workspace
        TopicType moodleConfig = dms.getTopicType(MOODLE_CONFIG, null);
        assignWorkspace(moodleConfig);

        // 4) Assign new type "Moodle Item" to our default workspace
        TopicType moodleItem = dms.getTopicType(MOODLE_ITEM, null);
        assignWorkspace(moodleItem);

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