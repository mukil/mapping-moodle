package org.deepamehta.plugins.moodle.migrations;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.Migration;
import java.util.logging.Logger;
import org.deepamehta.plugins.moodle.MoodleServiceClient;

public class Migration2 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    private final String WS_MOODLE_URI = "org.deepamehta.workspaces.moodle";
    private static final String DEEPAMEHTA_USERNAME_URI = "dm4.accesscontrol.username";

    @Override
    public void run() {

        // 1) create "ISIS / Moodle""-Workspace
        TopicModel workspace = new TopicModel(WS_MOODLE_URI, "dm4.workspaces.workspace");
        Topic ws = dms.createTopic(workspace, null);
        ws.setSimpleValue(MoodleServiceClient.WS_MOODLE_NAME);
        // 2) assign "admin" username to "Moodle"-Workspace
        Topic administrator = dms.getTopic(DEEPAMEHTA_USERNAME_URI, new SimpleValue("admin"), true);
        assignToMoodleWorkspace(administrator);
        // 3) Assign some of our types to the "Moodle"-Workspace
        TopicType item = dms.getTopicType(MoodleServiceClient.MOODLE_ITEM_URI);
        assignToMoodleWorkspace(item);
        TopicType section = dms.getTopicType(MoodleServiceClient.MOODLE_SECTION_URI);
        assignToMoodleWorkspace(section);
        TopicType course = dms.getTopicType(MoodleServiceClient.MOODLE_COURSE_URI);
        assignToMoodleWorkspace(course);
        AssociationType participant = dms.getAssociationType(MoodleServiceClient.MOODLE_PARTICIPANT_EDGE);
        assignToMoodleWorkspace(participant);

    }

    // === Workspace ===

    private void assignToMoodleWorkspace(Topic topic) {
        if (hasAnyWorkspace(topic)) {
            return;
        }
        Topic defaultWorkspace = dms.getTopic("uri", new SimpleValue(WS_MOODLE_URI), false);
        dms.createAssociation(new AssociationModel("dm4.core.aggregation",
            new TopicRoleModel(topic.getId(), "dm4.core.parent"),
            new TopicRoleModel(defaultWorkspace.getId(), "dm4.core.child")
        ), null);
    }

    private boolean hasAnyWorkspace(Topic topic) {
        return topic.getRelatedTopics("dm4.core.aggregation", "dm4.core.parent", "dm4.core.child",
            "dm4.workspaces.workspace", false, false, 0).getSize() > 0;
    }

}
