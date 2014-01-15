package org.deepamehta.plugins.moodle.migrations;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.service.Migration;
import java.util.logging.Logger;

public class Migration3 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    private final String REVIEW_SCORE = "org.deepamehta.reviews.score";
    private final String MOODLE_ITEM = "org.deepamehta.moodle.item";

    @Override
    public void run() {

        // 1) Enrich the "Moodle Item"-Type about one "Score"
        TopicType moodleItem = dms.getTopicType(MOODLE_ITEM);
        moodleItem.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def", MOODLE_ITEM,
                REVIEW_SCORE, "dm4.core.one", "dm4.core.one"));

    }

}
