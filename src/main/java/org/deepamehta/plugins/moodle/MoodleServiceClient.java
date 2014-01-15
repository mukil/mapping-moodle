package org.deepamehta.plugins.moodle;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.*;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.core.util.JavaUtils;
import de.deepamehta.plugins.accesscontrol.event.PostLoginUserListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.plugins.accesscontrol.model.ACLEntry;
import de.deepamehta.plugins.accesscontrol.model.AccessControlList;
import de.deepamehta.plugins.accesscontrol.model.Operation;
import de.deepamehta.plugins.accesscontrol.model.UserRole;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import de.deepamehta.plugins.websockets.event.WebsocketTextMessageListener;
import de.deepamehta.plugins.websockets.service.WebSocketsService;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


/**
 *
 * A webservice-client enabling collaborative mapping on course-materials (and more) for any moodle
 * (2.4+) course. A very simple plugin to connect users of DeepaMehta 4 with a moodle installation.
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/mapping-moodle
 * @version 1.2.0
 *
 */

@Path("/moodle")
public class MoodleServiceClient extends PluginActivator implements PostLoginUserListener,
                                                                    WebsocketTextMessageListener {

    private static Logger log = Logger.getLogger(MoodleServiceClient.class.getName());

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AccessControlService aclService;
    private WebSocketsService webSocketsService;

    // --- URIs DeepaMehta and all plugins in use

    public static final String WS_MOODLE_NAME = "ISIS / Moodle";
    public static final String WS_MOODLE_URI = "org.deepamehta.workspaces.moodle";
    public static final String MOODLE_PARTICIPANT_EDGE = "org.deepamehta.moodle.course_participant";
    public static final String MOODLE_USAGE_REPORT = "org.deepamehta.moodle.usage_report";

    public static final String MOODLE_COURSE_URI = "org.deepamehta.moodle.course";
    public static final String MOODLE_COURSE_NAME_URI = "org.deepamehta.moodle.course_name";
    public static final String MOODLE_COURSE_SHORT_NAME_URI = "org.deepamehta.moodle.course_short_name";

    public static final String MOODLE_SECTION_URI = "org.deepamehta.moodle.section";
    public static final String MOODLE_SECTION_NAME_URI = "org.deepamehta.moodle.section_name";
    public static final String MOODLE_SECTION_SUMMARY_URI = "org.deepamehta.moodle.section_summary";
    public static final String MOODLE_SECTION_ORDINAL_NR = "org.deepamehta.moodle.section_ordinal_nr";

    public static final String MOODLE_ITEM_URI = "org.deepamehta.moodle.item";
    public static final String MOODLE_ITEM_NAME_URI = "org.deepamehta.moodle.item_name";
    public static final String MOODLE_ITEM_ICON_URI = "org.deepamehta.moodle.item_icon";
    public static final String MOODLE_ITEM_REMOTE_URL_URI = "org.deepamehta.moodle.item_url";
    public static final String MOODLE_ITEM_MEDIA_TYPE_URI = "org.deepamehta.moodle.item_media_type";
    public static final String MOODLE_ITEM_DESC_URI = "org.deepamehta.moodle.item_description";
    public static final String MOODLE_ITEM_HREF_URI = "org.deepamehta.moodle.item_href";
    public static final String MOODLE_ITEM_TYPE_URI = "org.deepamehta.moodle.item_type";
    public static final String MOODLE_ITEM_MODIFIED_URI = "org.deepamehta.moodle.item_modified";
    public static final String MOODLE_ITEM_CREATED_URI = "org.deepamehta.moodle.item_created";
    public static final String MOODLE_ITEM_AUTHOR_URI = "org.deepamehta.moodle.item_author";
    public static final String MOODLE_ITEM_LICENSE_URI = "org.deepamehta.moodle.item_license";
    public static final String MOODLE_ITEM_SIZE_URI = "org.deepamehta.moodle.item_size";

    // --- Data instance URIs for ISIS 2

    public static final String ISIS_COURSE_URI_PREFIX = "de.tu-berlin.course.";
    public static final String ISIS_SECTION_URI_PREFIX = "de.tu-berlin.section.";
    public static final String ISIS_ITEM_URI_PREFIX = "de.tu-berlin.item.";

    // --- ISIS 2 Webservice related URIs

    private final String SERVICE_ENDPOINT_TYPE_URI = "org.deepamehta.config.moodle_service_url";
    private final String USERNAME_OF_SETTINGS_ADMINISTRATOR = "admin"; // Username eligible to edit SETTINGs
    private final String MOODLE_SECURITY_KEY_URI = "org.deepamehta.moodle.security_key";
    private final String MOODLE_USER_ID_URI = "org.deepamehta.moodle.user_id";
    private final String MOODLE_SERVICE_NAME = "eduzen_web_service";
    private final String MOODLE_SERVICE_FORMAT = "moodlewsrestformat=json";

    // --- Deepamehta 4 URIs

    private final String DEFAULT_ROLE_TYPE_URI = "dm4.core.default";
    private final String CHILD_ROLE_TYPE_URI = "dm4.core.child";
    private final String PARENT_ROLE_TYPE_URI = "dm4.core.parent";
    private final String AGGREGATION_TYPE_URI = "dm4.core.aggregation";
    private final String COMPOSITION_TYPE_URI = "dm4.core.composition";
    private final String USER_ACCOUNT_TYPE_URI = "dm4.accesscontrol.user_account";
    private final String USER_NAME_TYPE_URI = "dm4.accesscontrol.username";
    private final String CHILD_URI = "dm4.core.child";
    private final String PARENT_URI = "dm4.core.parent";
    private final String TAG_URI = "dm4.tags.tag";
    private final String REVIEW_SCORE_URI = "org.deepamehta.reviews.score";


    // -------------------------------------------------------------------------------------------------- Public Methods

    // --
    // --- Hook implementations
    // --

    @Override
    public void init() {
        if (aclService != null) {
            Topic serviceEndpointUri = getMoodleServiceUrl();
            setDefaultMoodleAdminACLEntries(serviceEndpointUri);
            Topic moodleWs = dms.getTopic("uri", new SimpleValue(WS_MOODLE_URI), false);
            setDefaultMoodleAdminACLEntries(moodleWs);
        }
    }

    @Override
    @ConsumesService({
        "de.deepamehta.plugins.accesscontrol.service.AccessControlService",
        "de.deepamehta.plugins.websockets.service.WebSocketsService"
    })
    public void serviceArrived(PluginService service) {
        if (service instanceof AccessControlService) {
            aclService = (AccessControlService) service;
        } else if (service instanceof WebSocketsService) {
            webSocketsService = (WebSocketsService) service;
        }
    }

    @Override
    public void serviceGone(PluginService service) {
        if (service instanceof AccessControlService) {
            aclService = null;
        } else if (service instanceof WebSocketsService) {
            webSocketsService = null;
        }
    }

    // --
    // --- Listener Implementations
    // --

    @Override
    public void postLoginUser(String username) {

        startMoodleSynchronization(username);

    }

    @Override
    public void websocketTextMessage(String message) {
        log.info("### Receiving message from WebSocket client: \"" + message + "\"");

    }

    private void sendClientNotification(String message, Topic topic) {
        if (webSocketsService != null) {
            JSONObject banana = new JSONObject();
            try {
                banana.put("message", message);
                banana.put("topic", topic.toJSON());
                webSocketsService.broadcast(getUri(), banana.toString());
            } catch (JSONException j) {
                log.warning("Problems with sending stupid message to all clients.");
            }
        } else {
            log.warning("MoodleServiceClient.webSocketService is suddenly GONE!");
        }
    }


    /** Relates the moodle-security-key to our currently logged-in user-account. **/
    @POST
    @Path("/key/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String setMoodleSecurityKey(@PathParam("id") int id, final String input ) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // 1) Store security key for this user
            Topic userAccount = checkAuthorization();
            if (userAccount.getId() != id) throw new WebApplicationException(new RuntimeException("Not allowed"), 401);
            Topic user = dms.getTopic(id, true);
            JSONObject payload = new JSONObject(input);
            String moodle_key = payload.getString("moodle_key");
            // 2) prevent setting of keys which do not look like a moodle security key
            if (moodle_key.equals("") || moodle_key.length() != 32) {
                throw new RuntimeException("Sorry but that does not look like a \"Moodle security key.\"");
            }
            user.setProperty(MOODLE_SECURITY_KEY_URI, moodle_key, false); // addToIndex=false **/
            // 3) Fetch user_id from moodle installation (to be able to start querying the service)
            fetchAndSetMoodleUserId(userAccount);
            tx.success();
            return "{ \"result\": \"OK\"}";
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
            tx.failure();
            throw new WebApplicationException(new Throwable("Problems reading your payload."), 500);
        } catch (WebApplicationException wex) {
            log.info("ERROR: Your Moodle UserId could not be fetched. Your security key must be entered again "
                + "once your administrator configured a proper service endpoint");
            tx.failure();
            throw new WebApplicationException(new Throwable("Due to a (remote) configuration error (or missing "
                    + "internet connection) your security key could not be set."), 500);
        } finally {
            tx.finish();
        }
    }

    @GET
    @Path("/key")
    @Produces(MediaType.TEXT_PLAIN)
    public String getMoodleSecurityKey() {
        Topic userAccount = checkAuthorization();
        if (userAccount.hasProperty(MOODLE_SECURITY_KEY_URI)) {
            String token = (String) userAccount.getProperty(MOODLE_SECURITY_KEY_URI);
            return token;
        }
        return null;
    }

    @GET
    @Path("/synchronize/{username}")
    @Produces(MediaType.TEXT_PLAIN)
    public String startMoodleSynchronization(@PathParam("username") String username) {

        final Topic user = checkAuthorization();
        // 1) Twofold sanity check
        if (!user.getSimpleValue().toString().equals(username)) {
            log.info("MoodleServiceClient sanity check failed " + username + " != " + user.getSimpleValue().toString());
            throw new RuntimeException();
        }
        // 2) Start Moodle-Sync for this username
        startSynchronizationThreadFor(user);

        return "OK";

    }



    // --- Private helper methods ---

    private Topic checkAuthorization() {
        String username = aclService.getUsername();
        if (username == null) throw new WebApplicationException(401);
        return getUserAccountTopic(username);
    }

    private boolean setMoodleUserId(Topic userAccount, long moodleUserId) {
        DeepaMehtaTransaction tx = dms.beginTx();
        userAccount.setProperty(MOODLE_USER_ID_URI, "" + moodleUserId + "", false);
        tx.success();
        tx.finish();
        return true;
    }

    private void startSynchronizationThreadFor(Topic user) {

        final String user_name = user.getCompositeValue().getString(USER_NAME_TYPE_URI).toString();
        final Topic local_user = user;

        new Thread() {

            @Override
            public void run() {

                log.info("Started MoodleServiceClient-Synchronization Thread due to a _login-Event .. ");
                // 2) Fetch Moodle Security Key for newly logged in user
                String key = getMoodleSecurityKeyWithoutAuthCheck(local_user);
                if (key != null && key.length() == 32) { // 2) Check if it looks like a legit one for MOODLE
                    // todo: check if we have a MoodleUserID for this user ..
                    log.info("MoodleServiceclient found a security key for \"" + user_name + "\" which looks legit ...");
                } else {
                    throw new RuntimeException("This user has no \"Moodle Security Key\" set no SYNC.. ");
                }
                // 3) Ask for potentially new \"Moodle Courses\" ..
                log.info("MoodleServiceClient checking for news in all \"Moodle Courses\" of \""+user_name+"\"");
                //    and create them if necessary / yet unknown to our system/installation
                getMoodleCoursesWithoutAuth(local_user, getMoodleUserId(local_user), key, null);
                // 4) Check for new \"Moodle Items\" in each tagged \"Moodle Course\" our user is "participating"
                ResultList<RelatedTopic> courses = getMoodleCoursesByUser(local_user);
                for (RelatedTopic course : courses) {
                    // 5) set new Moodle Courses (if they have no "Moodle Course Hashtag" set) blocked for synchronziations
                    course.loadChildTopics(TAG_URI);
                    if (course.getCompositeValue().has(TAG_URI) &&
                        course.getCompositeValue().getTopics(TAG_URI).size() > 0) {
                        Topic courseHashtag = course.getCompositeValue().getTopics(TAG_URI).get(0);
                        log.info("MoodleServiceClient SYNC Course under tag #" + courseHashtag.getSimpleValue());
                        getCourseContentsWithoutAuth(course.getId(), key, courseHashtag, null);
                    } else {
                        log.warning("MoodleServiceClient waiting with SYNC cause of missing #Hashtag (on \""
                                + course.getSimpleValue() + "\")");
                    }
                }
            }
        }.start();
    }

    /** Fetches and relates the internal moodle-user-id to our currently logged-in user-account. **/
    private Topic fetchAndSetMoodleUserId(Topic userAccount) throws WebApplicationException {

        String token = getMoodleSecurityKeyWithoutAuthCheck(userAccount);
        if (token == null) throw new WebApplicationException(new RuntimeException("User has no security key."), 500);
        String parameter = "serviceshortnames[0]=" + MOODLE_SERVICE_NAME;
        String data = "";
        try {
            data = callMoodle(token, "core_webservice_get_site_info", parameter);
            JSONObject response = new JSONObject(data.toString());
            long userId = response.getLong("userid");
            log.info("Set Moodle User ID => " + userId);
            setMoodleUserId(userAccount, userId);
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
            try {
                JSONObject exception = new JSONObject(data.toString());
                log.warning("MoodleException: " + exception.getString("message"));
            } catch (JSONException ex1) {
                Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (MoodleConnectionException mc) {
            throw new WebApplicationException(mc, mc.status);
        }
        return userAccount;
    }

    private Topic getMoodleCoursesWithoutAuth(Topic userAccount, long moodleUserId, String token, ClientState clientState) {
        String parameter = "userid=" + moodleUserId;
        String data = "";
        try {
            data = callMoodle(token, "core_enrol_get_users_courses", parameter);
            if (data.indexOf("webservice_access_exception") != -1) {
                log.warning("Looks like external service (webservice feature) is not \"Enabled\" or the called function"
                        + " is not part of the \"External Service\" defintion on the configured Moodle installation.");
                throw new WebApplicationException(new MoodleConnectionException(data, 404), 404);
            }
            //
            JSONArray response = new JSONArray(data.toString());
            for (int i = 0; i < response.length(); i++) {
                JSONObject course = response.getJSONObject(i);
                Topic courseTopic = getMoodleCourseTopic(course.getLong("id"));
                if (courseTopic == null) {
                    // 1) Create new item
                    courseTopic = createMoodleCourseTopic(course, clientState);
                    if (courseTopic != null) {
                        // 2) Fix ACLEntries (caused by missing request-scope in Thread-local)
                        setDefaultMoodleAdminACLEntries(courseTopic); // just "admin" can edit course-items
                        sendClientNotification("New Moodle Course", courseTopic);
                    } else {
                        log.info("OMITTING HIDDEN MoodleCourse \"" + course.getString("shortname") + "\"");
                    }
                } else {
                    updateMoodleCourseTopic(courseTopic, course, clientState);
                }
                if (!hasParticipantEdge(courseTopic, userAccount)) {
                    createParticipantEdge(courseTopic, userAccount, clientState);
                }
            }
        } catch (JSONException ex) {
            try {
                JSONObject response = new JSONObject(data.toString());
                String exception = response.getString("exception");
                throw new WebApplicationException(new MoodleConnectionException(exception, 500), 500);
            } catch (JSONException ex1) {}
        } catch (MoodleConnectionException mc) {
            throw new WebApplicationException(new Throwable(mc.message), mc.status);
        } finally {
            return userAccount;
        }
    }

    private Topic getCourseContentsWithoutAuth(long topicId, String token, Topic hashtag, ClientState clientState) {
        long courseId = -1;
        Topic courseTopic = dms.getTopic(topicId, true);
        courseId = Long.parseLong(courseTopic.getUri().replaceAll(ISIS_COURSE_URI_PREFIX, ""));
        String parameter = "courseid=" + courseId;
        String data = "";
        try {
            data = callMoodle(token, "core_course_get_contents", parameter);
            JSONArray response = new JSONArray(data.toString());
            for (int i = 0; i < response.length(); i++) {
                JSONObject section = response.getJSONObject(i);
                Topic sectionTopic = getMoodleSectionTopic(section.getLong("id"));
                // 1) Create new \"Moodle Section\"
                if (sectionTopic == null) {
                    sectionTopic = createMoodleSectionTopic(section, i, clientState);
                    if (sectionTopic != null) {
                        // Fix Section-ACL-Entries so that "admin" can edit them
                        setDefaultMoodleAdminACLEntries(sectionTopic);
                        sendClientNotification("New Moodle Section in \"" +
                                courseTopic.getSimpleValue().toString() + "\"", sectionTopic);
                    }
                // 2) Update existing \"Moodle section\"
                } else {
                    updateMoodleSectionTopic(sectionTopic, section, clientState);
                }
                // 3) Create or Update all \"Moodle Items\"
                if (sectionTopic != null) { // (if section is not null/ hidden)
                    JSONArray modules = section.getJSONArray("modules");
                    for (int k = 0; k < modules.length(); k++) {
                        JSONObject item = modules.getJSONObject(k);
                        Topic itemTopic = getMoodleItemTopic(item.getLong("id"));
                        if (itemTopic == null) {
                            itemTopic = createMoodleItemTopic(item, hashtag, clientState);
                            if (itemTopic != null) {
                                // Fix ACL so that all "Moodle"-WS Members can edit these items
                                setDefaultMoodleGroupACLEntries(itemTopic);
                                Association creator_edge = assignDefaultAuthorship(itemTopic);
                                setDefaultMoodleGroupACLEntries(creator_edge);
                            }
                        } else {
                            updateMoodleItemTopic(itemTopic, courseTopic, item, clientState);
                        }
                        if (!hasAggregatingSectionParentEdge(itemTopic, sectionTopic)) {
                            createAggregatingSectionEdge(sectionTopic, itemTopic, clientState);
                        }
                    }
                }
                // 4) Assign new \"Moodle section\" to course (if section is not null/hidden)
                if (!hasAggregatingCourseParentEdge(sectionTopic, courseTopic)) {
                    createAggregatingCourseEdge(courseTopic, sectionTopic, clientState);
                }
            }
            log.info("MoodleServiceClient finished loading materials for course \""+courseTopic.getSimpleValue()+"\"");
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
            try {
                JSONObject exception = new JSONObject(data.toString());
                String message = exception.getString("message");
                throw new WebApplicationException(new MoodleConnectionException(message, 500), 500);
            } catch (JSONException ex1) {
                Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (MoodleConnectionException mc) {
            throw new WebApplicationException(mc, mc.status);
        }
        return courseTopic;
    }

    private String callMoodle (String key, String functionName, String params) throws MoodleConnectionException {

        Topic serviceUrl = getMoodleServiceUrl();
        String endpointUri = serviceUrl.getSimpleValue().value().toString();
        String queryUrl = endpointUri + "?wstoken=" + key + "&wsfunction=" + functionName + "&" + MOODLE_SERVICE_FORMAT;
        // "&service=" + MOODLE_SERVICE_NAME +
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(queryUrl).openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Content-Language", "en-US");
            con.setDoOutput(true);
            con.setUseCaches (false);
            con.setDoInput(true);
            DataOutputStream wr = new DataOutputStream (con.getOutputStream());
            wr.writeBytes (params);
            wr.flush();
            wr.close();
            if (con.getResponseCode() != 200) { // this case never occurred to me
                log.warning("MoodleConnection HTTP Status \"" + con.getResponseCode() + "\"");
                throw new MoodleConnectionException("MoodleConnection has thrown an error", con.getResponseCode());
            }
            //Get Response
            InputStream is = con.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder response = new StringBuilder();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            // Handle empty response
            if (response.toString().isEmpty()) {
                String message = "MoodleConnection Response was empty. This happens even if webservice features "
                    + "are completely deactivated or the function is not part of the \"External services\" definition.";
                log.warning(message);
                throw new MoodleConnectionException(message, 204);
            }
            return response.toString();
        } catch (MoodleConnectionException ex) {
            throw new MoodleConnectionException(ex.message, ex.status);
        } catch (MalformedURLException ml)  {
            throw new MoodleConnectionException("DeepaMehta could not connect to malformed url: \"" + queryUrl + "\"",
                    404);
        } catch (IOException ex) {
            throw new MoodleConnectionException("DeepaMehta could not connect to \"" + queryUrl + "\"", 404);
        }
    }

    private void createParticipantEdge(Topic courseTopic, Topic userAccount, ClientState clientState) {
        AssociationModel participantEdge = new AssociationModel(MOODLE_PARTICIPANT_EDGE,
                new TopicRoleModel(courseTopic.getId(), DEFAULT_ROLE_TYPE_URI),
                new TopicRoleModel(userAccount.getId(), DEFAULT_ROLE_TYPE_URI));
        Association edge = dms.createAssociation(participantEdge, clientState);
    }

    private void createAggregatingCourseEdge (Topic courseTopic, Topic sectionTopic, ClientState clientState) {
        AssociationModel aggregationEdge = new AssociationModel(AGGREGATION_TYPE_URI,
                new TopicRoleModel(courseTopic.getId(), PARENT_ROLE_TYPE_URI),
                new TopicRoleModel(sectionTopic.getId(), CHILD_ROLE_TYPE_URI));
        Association edge = dms.createAssociation(aggregationEdge, clientState);
    }

    private void createAggregatingSectionEdge (Topic sectionTopic, Topic itemTopic, ClientState clientState) {
        AssociationModel aggregationEdge = new AssociationModel(AGGREGATION_TYPE_URI,
                new TopicRoleModel(sectionTopic.getId(), PARENT_ROLE_TYPE_URI),
                new TopicRoleModel(itemTopic.getId(), CHILD_ROLE_TYPE_URI));
        Association edge = dms.createAssociation(aggregationEdge, clientState);
    }

    private Topic createMoodleCourseTopic(JSONObject object, ClientState clientState) {
        try {
            if (object.getInt("visible") == 0) return null;
            long courseId = object.getLong("id");
            String shortName = object.getString("shortname");
            String fullName = object.getString("fullname");
            CompositeValueModel model = new CompositeValueModel();
            model.put(MOODLE_COURSE_NAME_URI, fullName);
            model.put(MOODLE_COURSE_SHORT_NAME_URI, shortName);
            TopicModel course = new TopicModel(ISIS_COURSE_URI_PREFIX + courseId, MOODLE_COURSE_URI, model);
            course.setUri(ISIS_COURSE_URI_PREFIX + courseId);
            Topic result = dms.createTopic(course, null); // null makes sure that a topic does not get assigned to a WS
            assignToMoodleWorkspace(result);
            return result;
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Topic updateMoodleCourseTopic(Topic course, JSONObject object, ClientState clientState) {
        try {
            boolean update_this = false;
            String new_name = object.getString("shortname");
            String new_fullname = object.getString("fullname");
            if (!course.getCompositeValue().getString(MOODLE_COURSE_SHORT_NAME_URI).equals(new_name) ||
                !course.getCompositeValue().getString(MOODLE_COURSE_NAME_URI).equals(new_fullname)) {
                update_this = true;
            }
            // Update (if there were changes)
            if (update_this) {
                CompositeValueModel model = new CompositeValueModel();
                model.put(MOODLE_COURSE_NAME_URI, new_name);
                model.put(MOODLE_COURSE_SHORT_NAME_URI, new_fullname);
                dms.updateTopic(new TopicModel(course.getId(), model), clientState);
                // todo: add to usage report
            }
            return course;
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Topic createMoodleSectionTopic(JSONObject object, int nr, ClientState clientState) {
        try {
            if (object.getInt("visible") == 0) return null; // item is hidden, do not create it
            long sectionId = object.getLong("id");
            String name = object.getString("name");
            String summary = object.getString("summary");
            CompositeValueModel model = new CompositeValueModel();
            model.put(MOODLE_SECTION_NAME_URI, name);
            model.put(MOODLE_SECTION_SUMMARY_URI, summary);
            model.put(MOODLE_SECTION_ORDINAL_NR, nr);
            TopicModel section = new TopicModel(ISIS_SECTION_URI_PREFIX + sectionId, MOODLE_SECTION_URI, model);
            // OWNER AND CREATOR will be the (logged in user) who triggered this method IN ANY CASE
            Topic result = dms.createTopic(section, null); //regardless of null, a username might be assigned to this
            setDefaultMoodleAdminACLEntries(result); // just "admin" can edit these
            assignToMoodleWorkspace(result);
            return result;
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Topic updateMoodleSectionTopic(Topic section, JSONObject object, ClientState clientState) {
        try {
            boolean update_this = false;
            String new_name = object.getString("name");
            String new_summary = object.getString("summary");
            // Custom comparison of values
            if (!section.getCompositeValue().getString(MOODLE_SECTION_NAME_URI).equals(new_name) ||
                !section.getCompositeValue().getString(MOODLE_SECTION_SUMMARY_URI).equals(new_summary)) {
                update_this = true;
            }
            // Update (if there were changes)
            if (update_this) {
                CompositeValueModel model = new CompositeValueModel();
                model.put(MOODLE_SECTION_NAME_URI, new_name);
                model.put(MOODLE_SECTION_SUMMARY_URI, new_summary);
                // Attached operations *need* Directives
                section.setCompositeValue(model, clientState, new Directives());
            }
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Topic createMoodleItemTopic(JSONObject object, Topic hashtag, ClientState clientState) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            if (object.getInt("visible") == 0) return null; // item is hidden, do not create it
            long itemId = object.getLong("id");
            String name = object.getString("name");
            String iconPath = object.getString("modicon");
            String type = object.getString("modname");
            String description = "", href = "";
            CompositeValueModel model = new CompositeValueModel();
            model.put(MOODLE_ITEM_NAME_URI, name);
            model.put(MOODLE_ITEM_ICON_URI, iconPath);
            if (type.equals("label")) {
                description = object.getString("description");
            } else {
                href = object.getString("url");
            }
            model.put(MOODLE_ITEM_DESC_URI, description);
            model.put(MOODLE_ITEM_HREF_URI, href);
            model.put(MOODLE_ITEM_TYPE_URI, type);
            // ..) initiliaze neutral review-score on every moodle item
            model.put(REVIEW_SCORE_URI, 0);
            JSONArray contents = null;
            if (object.has("contents")) {
                contents = object.getJSONArray("contents");
                for (int i = 0; i < contents.length(); i++) { // (actually never in use, but possible)
                    JSONObject resource = contents.getJSONObject(i);
                    fillUpItemModelWithResource(tx, model, resource);
                }
            }
            // ..) equip every moodle item with the courses default hashtag
            model.addRef(TAG_URI, hashtag.getId());
            // else if (contents["type"].equals("File") || equals("url")
            TopicModel item = new TopicModel(ISIS_ITEM_URI_PREFIX + itemId, MOODLE_ITEM_URI, model);
            Topic result = dms.createTopic(item, null);
            assignToMoodleWorkspace(result);
            // sendTopicNotification("New Moodle Item", result);
            tx.success();
            return result;
        } catch (JSONException ex) {
            tx.failure();
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            tx.finish();
        }
        return null;
    }

    private Topic updateMoodleItemTopic(Topic item, Topic courseTopic, JSONObject object, ClientState clientState) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // long itemId = object.getLong("id");
            String name = object.getString("name");
            String iconPath = object.getString("modicon");
            String type = object.getString("modname");
            String description = "", href = "";
            CompositeValueModel model = new CompositeValueModel();
            model.put(MOODLE_ITEM_NAME_URI, name);
            model.put(MOODLE_ITEM_ICON_URI, iconPath);
            if (type.equals("label")) {
                description = object.getString("description");
            } else {
                href = object.getString("url");
            }
            model.put(MOODLE_ITEM_DESC_URI, description);
            model.put(MOODLE_ITEM_HREF_URI, href);
            model.put(MOODLE_ITEM_TYPE_URI, type);
            JSONArray contents = null;
            boolean update_this = false;
            long last_modified_in_moodle = 0;
            if (object.has("contents")) {
                contents = object.getJSONArray("contents");
                for (int i = 0; i < contents.length(); i++) { // (actually never in use, but possible)
                    JSONObject resource = contents.getJSONObject(i);
                    fillUpItemModelWithResource(tx, model, resource);
                    // get last modification time from moodle-system response
                    if (resource.has("timemodified") && !resource.isNull("timemodified")) {
                        last_modified_in_moodle = resource.getLong("timemodified");
                    }
                }
            }
            // start to perform update checks on contents of this item
            if (!item.getSimpleValue().toString().equals(model.getString(MOODLE_ITEM_NAME_URI))) {
                log.info("MoodleServiceClient: The name of the item has changed: ITEM is TO_BE_UPDATED");
                update_this = true;
            } else if (item.getCompositeValue().getString(MOODLE_ITEM_TYPE_URI).equals("url")) {
                update_this = false;
                // on "url"-items we perform a comparison by-value (cause timestamps are always missing)
                if (!item.getCompositeValue().getString(MOODLE_ITEM_REMOTE_URL_URI)
                        .equals(model.getString(MOODLE_ITEM_REMOTE_URL_URI))) {
                    log.info("MoodleServiceClient: The url-value changed: ITEM is TO_BE_UPDATED");
                    update_this = true;
                }
            } else if (item.getCompositeValue().has(MOODLE_ITEM_MODIFIED_URI)) { // sometimes int sometimes long (webclient!)
                // get last modification times from our DB
                long existing_timestamp = item.getCompositeValue().getLong(MOODLE_ITEM_MODIFIED_URI);
                if (existing_timestamp < last_modified_in_moodle) { // lets write new contents to our \"Moodle Item\"
                    log.info("MoodleServiceClient: The remote Last-Modified-Timestamp indicates: ITEM is TO_BE_UPDATED");
                    update_this = true;
                }
            }
            if (update_this) {
                dms.updateTopic(new TopicModel(item.getId(), model), clientState);
                sendClientNotification("Changed Moodle Item in \""
                        + courseTopic.getSimpleValue().toString() + "\"", item);
            }
            tx.success();
            return item;
        } catch (JSONException ex) {
            tx.failure();
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            tx.finish();
        }
        return null;
    }

    private CompositeValueModel fillUpItemModelWithResource(DeepaMehtaTransaction tx,
            CompositeValueModel model, JSONObject resource) {
        try {
            String resourceType = resource.getString("type");
            String fileUrl = "", fileName = "", author = "", license = "";
            long last_modified = 0, time_created = 0;
            // 1) Fill up either web resource or file-type
            if (resourceType.equals("url")) { // Moodle Resource is an URL
                fileUrl = resource.getString("fileurl");
                // 1.1) parse youtube and replace /watch?v=id with /embed/id in the url
                if (fileUrl.indexOf("youtube") != -1 || fileUrl.indexOf("youtu.be") != -1) {
                    // turn youtube-share url into /embed/-url
                    fileUrl = createYoutubeEmbedUrl(fileUrl);
                }
                model.put(MOODLE_ITEM_REMOTE_URL_URI, fileUrl);
                // add it to the (to be created) Moodle Item
                model.put(MOODLE_ITEM_TYPE_URI, resourceType);
                // alternatively: model.put(MOODLE_ITEM_HREF_URI, fileurl);
            } else if (resourceType.equals("file")) { // Moodle Resource is an URL
               // pages _and_ documents are of type file
                fileName = resource.getString("filename");
                fileUrl = resource.getString("fileurl");
                if (!resource.isNull("license")) {
                    license = resource.getString("license");
                    model.put(MOODLE_ITEM_LICENSE_URI, license);
                }
                if (!resource.isNull("author")) {
                    author = resource.getString("author");
                    model.put(MOODLE_ITEM_AUTHOR_URI, author);
                }
                long fileSize = resource.getLong("filesize");
                model.put(MOODLE_ITEM_NAME_URI, fileName);
                model.put(MOODLE_ITEM_REMOTE_URL_URI, fileUrl);
                model.put(MOODLE_ITEM_SIZE_URI, fileSize);
                String file_type = JavaUtils.getFileType(fileName);
                // 1.1) Try to determine file media_type with the help of DM-Utils
                if (file_type == null) file_type = "Unknown"; // Maybe null (e.g. for .ODT-Documents)
                model.put(MOODLE_ITEM_MEDIA_TYPE_URI, file_type);
                model.put(MOODLE_ITEM_TYPE_URI, resourceType);
            }
            // 2) Check for timestamp "Last Modified"
            if (resource.has("timemodified") && !resource.isNull("timemodified")) {
                last_modified = resource.getLong("timemodified") * 1000; // addding three zeros
            } else { // e.g. contents of type "url" NEVER have any timestamp set, setting it to NOW
                last_modified = new Date().getTime();
            }
            // 3) Check for timestamp "Created"
            if (resource.has("timecreated") && !resource.isNull("timecreated")) {
                time_created = resource.getLong("timecreated")  * 1000; // addding three zeros;
            } else { // e.g. contents of type "url" NEVER have any timestamp set, setting it to NOW
                time_created = new Date().getTime();
            }
            model.put(MOODLE_ITEM_MODIFIED_URI, last_modified);
            model.put(MOODLE_ITEM_CREATED_URI, time_created);
        } catch (JSONException ex) {
            tx.failure();
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        // The new model will just be written to topic/db if our custom update-check (see caller) succeeds
        return model;
    }

    private String createYoutubeEmbedUrl(String fileUrl) {
        String new_url = fileUrl;
        // 0) Skip transformation if url already contains the "/embed/"-part
        if (fileUrl.indexOf("/embed") != -1) return new_url;
        // 1) Transform URL Scheme: http://www.youtube.com/watch?v=A9b9bxUyK0o
        if (fileUrl.indexOf("/watch") != -1) {
            String[] parts = fileUrl.split("v=");
            // Strip of everything behind & e.g.: &feature=youtube_gdata
            String new_url_part = "";
            int indexOfParameters = parts[1].indexOf("&");
            if (parts[1] != null && indexOfParameters != -1) {
                new_url_part = parts[1].substring(0, indexOfParameters);
                new_url = "//youtube.com/embed/" + new_url_part;
            } else if (parts[1] != null) {
                new_url = "//youtube.com/embed/" + parts[1];
            }
        }
        // 2) Transform URL Scheme: http://youtu.be/A9b9bxUyK0o
        if (fileUrl.indexOf("youtu.be/") != -1) {
            String[] parts = fileUrl.split(".be/");
                        String new_url_part = "";
            int indexOfParameters = parts[1].indexOf("&");
            if (parts[1] != null && indexOfParameters != -1) {
                new_url_part = parts[1].substring(0, indexOfParameters);
                new_url = "//youtube.com/embed/" + new_url_part;
            } else if (parts[1] != null) {
                new_url = "//youtube.com/embed/" + parts[1];
            }
        }
        return new_url;
    }

    private Topic getUserAccountTopic(String username) {
        Topic accountTopic = null;
        Topic userTopic = dms.getTopic(USER_NAME_TYPE_URI, new SimpleValue(username), true);
        accountTopic = userTopic.getRelatedTopic(COMPOSITION_TYPE_URI, CHILD_ROLE_TYPE_URI, PARENT_ROLE_TYPE_URI,
                USER_ACCOUNT_TYPE_URI, true, false);
        return accountTopic;
    }

    private long getMoodleUserId(Topic userAccount) {
        if (userAccount.hasProperty(MOODLE_USER_ID_URI)) {
            String id = (String) userAccount.getProperty(MOODLE_USER_ID_URI);
            long moodle_user_id = Long.parseLong(id);
            return moodle_user_id;
        }
        return -1;
    }

    private Topic getMoodleServiceUrl() {
        return dms.getTopic("uri", new SimpleValue(SERVICE_ENDPOINT_TYPE_URI), true);
    }

    private ResultList<RelatedTopic> getMoodleCoursesByUser(Topic user) {
        ResultList<RelatedTopic> courses = user.getRelatedTopics(MOODLE_PARTICIPANT_EDGE, DEFAULT_ROLE_TYPE_URI,
                DEFAULT_ROLE_TYPE_URI, MOODLE_COURSE_URI, false, false, 0);
        if (courses != null && courses.getSize() > 1) return courses;
        return null;
    }

    private String getMoodleSecurityKeyWithoutAuthCheck(Topic userAccount) {
        if (userAccount.hasProperty(MOODLE_SECURITY_KEY_URI)) {
            String token = (String) userAccount.getProperty(MOODLE_SECURITY_KEY_URI);
            return token;
        }
        return null;
    }

    private boolean hasParticipantEdge (Topic course, Topic user) {
        boolean value = false;
        Topic userAccount = course.getRelatedTopic(MOODLE_PARTICIPANT_EDGE, DEFAULT_ROLE_TYPE_URI,
                DEFAULT_ROLE_TYPE_URI, USER_ACCOUNT_TYPE_URI, true, true);
        if (userAccount != null && user.getId() == userAccount.getId()) return value = true;
        return value;
    }

    private boolean hasAggregatingCourseParentEdge (Topic child, Topic parent) {
        boolean value = false;
        if (child == null) return true;
        Topic topic = child.getRelatedTopic(AGGREGATION_TYPE_URI, CHILD_ROLE_TYPE_URI,
                PARENT_ROLE_TYPE_URI, MOODLE_COURSE_URI, true, true);
        if (topic != null && parent.getId() == topic.getId()) return value = true;
        return value;
    }

    private boolean hasAggregatingSectionParentEdge (Topic child, Topic parent) {
        boolean value = false;
        if (child == null) return true;
        Topic topic = child.getRelatedTopic(AGGREGATION_TYPE_URI, CHILD_ROLE_TYPE_URI,
                PARENT_ROLE_TYPE_URI, MOODLE_SECTION_URI, true, true);
        if (topic != null && parent.getId() == topic.getId()) return value = true;
        return value;
    }

    private Topic getMoodleCourseTopic(long courseId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_COURSE_URI_PREFIX + courseId), true);
    }

    private Topic getMoodleSectionTopic(long sectionId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_SECTION_URI_PREFIX + sectionId), true);
    }

    private Topic getMoodleItemTopic(long itemId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_ITEM_URI_PREFIX + itemId), true);
    }

    // === ACL Fix ===

    private DeepaMehtaObject setDefaultMoodleGroupACLEntries(DeepaMehtaObject item) {
        // Let's repair broken/missing ACL-Entries
        ACLEntry writeList = new ACLEntry(Operation.WRITE, UserRole.MEMBER, UserRole.CREATOR, UserRole.OWNER);
        aclService.setACL(item, new AccessControlList(writeList));
        aclService.setCreator(item, USERNAME_OF_SETTINGS_ADMINISTRATOR);
        aclService.setOwner(item, USERNAME_OF_SETTINGS_ADMINISTRATOR);
        return item;
    }

    private DeepaMehtaObject setDefaultMoodleAdminACLEntries(DeepaMehtaObject item) {
        // Let's repair broken/missing ACL-Entries
        ACLEntry writeEntry = new ACLEntry(Operation.WRITE, UserRole.CREATOR, UserRole.OWNER);
        aclService.setACL(item, new AccessControlList(writeEntry));
        aclService.setCreator(item, USERNAME_OF_SETTINGS_ADMINISTRATOR);
        aclService.setOwner(item, USERNAME_OF_SETTINGS_ADMINISTRATOR);
        return item;
    }

    // === Workspace Assignments ===

    private void assignToMoodleWorkspace(Topic topic) {
        if (hasAnyWorkspace(topic)) {
            log.warning("NOT assigning to Moodle Workspace since topic HAS some assigned already ..");
            return;
        }
        Topic moodleWorkspace = dms.getTopic("uri", new SimpleValue(WS_MOODLE_URI), false);
        dms.createAssociation(new AssociationModel(AGGREGATION_TYPE_URI,
            new TopicRoleModel(topic.getId(), PARENT_ROLE_TYPE_URI),
            new TopicRoleModel(moodleWorkspace.getId(),CHILD_ROLE_TYPE_URI)
        ), null);
    }

    private boolean hasAnyWorkspace(Topic topic) {
        return topic.getRelatedTopics(AGGREGATION_TYPE_URI, PARENT_ROLE_TYPE_URI, CHILD_ROLE_TYPE_URI,
            "dm4.workspaces.workspace", false, false, 0).getSize() > 0;
    }

    private boolean hasMoodleWorkspace(Topic topic) {
        ResultList<RelatedTopic> workspaces = topic.getRelatedTopics(AGGREGATION_TYPE_URI, PARENT_ROLE_TYPE_URI, CHILD_ROLE_TYPE_URI,
            "dm4.workspaces.workspace", false, false, 0);
        Topic moodleWs = null;
        for (RelatedTopic ws : workspaces) {
            if (ws.getSimpleValue().toString().equals(WS_MOODLE_NAME)) moodleWs = ws;
        }
        return (moodleWs != null) ? true : false;
    }

    private Association assignDefaultAuthorship(Topic item) {
        Topic author = dms.getTopic("dm4.accesscontrol.username", new SimpleValue(USERNAME_OF_SETTINGS_ADMINISTRATOR), false);
        // org.deepamehta.tagging-resources - Constants
        String CREATOR_EDGE_URI = "org.deepamehta.resources.creator_edge";
        // String CONTRIBUTOR_EDGE_URI = "org.deepamehta.resources.contributor_edge";
        if (associationExists(CREATOR_EDGE_URI, item, author)) return null;
        return dms.createAssociation(new AssociationModel(CREATOR_EDGE_URI,
                new TopicRoleModel(item.getId(), PARENT_URI),
                new TopicRoleModel(author.getId(), CHILD_URI)), null);
    }

    private boolean associationExists(String edge_type, Topic item, Topic user) {
        List<Association> results = dms.getAssociations(item.getId(), user.getId(), edge_type);
        return (results.size() > 0) ? true : false;
    }

}
