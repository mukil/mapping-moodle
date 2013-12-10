package org.deepamehta.plugins.moodle;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.*;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.core.util.JavaUtils;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import de.deepamehta.plugins.accesscontrol.model.ACLEntry;
import de.deepamehta.plugins.accesscontrol.model.AccessControlList;
import de.deepamehta.plugins.accesscontrol.model.Operation;
import de.deepamehta.plugins.accesscontrol.model.UserRole;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
 * Note: No update mechanism present.
 *
 */

@Path("/moodle")
public class MoodleServiceClient extends PluginActivator {

    private static Logger log = Logger.getLogger(MoodleServiceClient.class.getName());

    private AccessControlService aclService;

    private String DEFAULT_ROLE_TYPE_URI = "dm4.core.default";
    private String CHILD_ROLE_TYPE_URI = "dm4.core.child";
    private String PARENT_ROLE_TYPE_URI = "dm4.core.parent";
    private String AGGREGATION_TYPE_URI = "dm4.core.aggregation";
    private String COMPOSITION_TYPE_URI = "dm4.core.composition";
    private String USER_ACCOUNT_TYPE_URI = "dm4.accesscontrol.user_account";
    private String USER_NAME_TYPE_URI = "dm4.accesscontrol.username";
    // private String WEB_RESOURCE_TYPE_URI = "dm4.webbrowser.web_resource";

    public static final String MOODLE_PARTICIPANT_EDGE = "org.deepamehta.moodle.course_participant";

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
    // public static final String MOODLE_ITEM_CREATED_URI = "org.deepamehta.moodle.item_created";
    // public static final String MOODLE_ITEM_AUTHOR_URI = "org.deepamehta.moodle.item_author";
    // public static final String MOODLE_ITEM_LICENSE_URI = "org.deepamehta.moodle.item_license";
    public static final String MOODLE_ITEM_SIZE_URI = "org.deepamehta.moodle.item_size";

    public static final String SERVICE_ENDPOINT_TYPE_URI = "org.deepamehta.config.moodle_service_url";
    public static final String USERNAME_OF_SETTINGS_ADMINISTRATOR = "admin";

    public static final String MOODLE_SECURITY_KEY_URI = "org.deepamehta.moodle.security_key";
    public static final String MOODLE_USER_ID_URI = "org.deepamehta.moodle.user_id";

    public static final String MOODLE_SERVICE_NAME = "eduzen_web_service";
    public static final String MOODLE_SERVICE_FORMAT = "moodlewsrestformat=json";

    public static final String ISIS_COURSE_URI_PREFIX = "de.tu-berlin.course.";
    public static final String ISIS_SECTION_URI_PREFIX = "de.tu-berlin.section.";
    public static final String ISIS_ITEM_URI_PREFIX = "de.tu-berlin.item.";



    @Override
    public void init() {
        if (aclService != null) {
            Topic serviceEndpointUri = getMoodleServiceUrl();
            aclService.setCreator(serviceEndpointUri, USERNAME_OF_SETTINGS_ADMINISTRATOR);
            aclService.setOwner(serviceEndpointUri, USERNAME_OF_SETTINGS_ADMINISTRATOR);
            AccessControlList aclist = new AccessControlList()
                    .addEntry(new ACLEntry(Operation.WRITE, UserRole.CREATOR));
            aclService.setACL(serviceEndpointUri, aclist);
        }
    }

    /**
     * @return list of all <code>courses</code> our user is currently enrolled in
     */
    @GET
    @Path("/courses")
    @Produces("application/json")
    public Topic getMoodleCourses(@HeaderParam("Cookie") ClientState clientState) {

        // A 401 WebApplicationException thrown in private does turn into a 500 (?)
        Topic userAccount = checkAuthorization();
        String token = getMoodleSecurityKey(userAccount);
        if (token == null) throw new WebApplicationException(new RuntimeException("User has no security key."), 500);
        long userId = getMoodleUserId(userAccount); // fixme: how to get the current userId
        if (userId == -1) throw new WebApplicationException(new RuntimeException("Unkown moodle user id."), 500);

        String parameter = "userid=" + userId;
        String data = "";
        // fixme: token and userId are not matched up against each other in this request, changing userId => exploit
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
                    courseTopic = createMoodleCourseTopic(course, clientState);
                    log.info("Created at new MoodleCourse \"" + courseTopic.getSimpleValue() + "\"");
                } else {
                    log.warning("NOT IMPLEMENTED YET => UPDATING EXISTING COURSE");
                    // todo: allow update of course name, shortname topics
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

    @GET
    @Path("/course/{topicId}/content")
    @Produces("application/json")
    public Topic getCourseContents(@PathParam("topicId") int topicId, @HeaderParam("Cookie") ClientState clientState) {

        Topic userAccount = checkAuthorization();
        String token = getMoodleSecurityKey(userAccount);
        if (token == null) throw new WebApplicationException(new RuntimeException("User has no security key."), 500);

        long courseId = -1;
        Topic courseTopic = dms.getTopic(topicId, true);
        courseId = Long.parseLong(courseTopic.getUri().replaceAll(ISIS_COURSE_URI_PREFIX, ""));
        // fixme: workaround #34
        String parameter = "courseid=" + courseId;
        String data = "";
        // DEBUG-Information:
        // [ sections { id, name, summary, modules [{ id, name, description (just for label), modname, modicon,
            // availablefrom, availableuntil, contents [ {type (either url or file), description (label),
            // resource: filename, filepath, filesize, author, license, fileurl, description
            // supported modules (by modname) shall be:
            //  resource(file) (name, contents), url(fileurl) (name, contents), label (name, description),
            //  page(htmlfile) (name, contents)
            // unsupported modules (by modname) will be (though we'll keep the name and the url for each):
            // folder, quiz, assign, choice, book, glossary, forum
        // ] }] } ]
        // skipping summary, summaryformat, visible (?), availablefrom, availableuntil, indent,
        // if your not a participant of the requested course, moodle answers with a:
            // {"exception":"moodle_exception","errorcode":"errorcoursecontextnotvalid",
            // "message":"You cannot execute functions in the course context (course id:3).
            // The context error message was: Course or activity not accessible."}
        try {
            data = callMoodle(token, "core_course_get_contents", parameter);
            JSONArray response = new JSONArray(data.toString());
            for (int i = 0; i < response.length(); i++) {
                JSONObject section = response.getJSONObject(i);
                Topic sectionTopic = getMoodleSectionTopic(section.getLong("id"));
                if (sectionTopic == null) {
                    sectionTopic = createMoodleSectionTopic(section, i, clientState);
                } else {
                    // todo: allow update of section name, summary-topics
                    log.warning("NOT IMPLEMENTED YET => UPDATING SECTION \r\n " + sectionTopic.toJSON().toString());
                }
                if (!hasAggregatingCourseParentEdge(sectionTopic, courseTopic)) {
                    createAggregatingCourseEdge(courseTopic, sectionTopic, clientState);
                }
                JSONArray modules = section.getJSONArray("modules");
                for (int k = 0; k < modules.length(); k++) {
                    JSONObject item = modules.getJSONObject(k);
                    Topic itemTopic = getMoodleItemTopic(item.getLong("id"));
                    if (itemTopic == null) {
                        itemTopic = createMoodleItemTopic(item, clientState);
                    } else {
                        // todo: allow update of items contents
                        log.warning("NOT IMPLEMENTED YET => UPDATING ITEM \r\n " + itemTopic.toJSON().toString());
                    }
                    if (!hasAggregatingSectionParentEdge(itemTopic, sectionTopic)) {
                        createAggregatingSectionEdge(sectionTopic, itemTopic, clientState);
                    }
                }
            }
            log.info("Loaded materials for course \""+courseTopic.getSimpleValue()+"\"");
            // Workaround to update (internal) "Last modifed" value of our just altered Course-Topic
            dms.updateTopic(new TopicModel(courseTopic.getId(), courseTopic.getUri(), null, null, null), null);
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
            if (userAccount.getId() != id) throw new WebApplicationException(new RuntimeException("Get a job."), 401);
            Topic user = dms.getTopic(id, true);
            JSONObject payload = new JSONObject(input);
            String moodle_key = payload.getString("moodle_key");
            user.setProperty(MOODLE_SECURITY_KEY_URI, moodle_key, false);		// addToIndex=false **/
            // 2) Fetch user_id from moodle installation (to be able to start querying the service)
            // And it looks like our new security key is already written to DB at this point
            // (otherwise the following request would fail).
            fetchAndSetMoodleUserId(); // This could cause a rollback (so that no security key is written).
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

    // ---

    @Override
    @ConsumesService("de.deepamehta.plugins.accesscontrol.service.AccessControlService")
    public void serviceArrived(PluginService service) {
        aclService = (AccessControlService) service;
    }

    @Override
    public void serviceGone(PluginService service) {
        aclService = null;
    }

    // ---

    /** Fetches and relates the internal moodle-user-id to our currently logged-in user-account. **/
    private Topic fetchAndSetMoodleUserId() throws WebApplicationException {

        Topic userAccount = checkAuthorization();
        String token = getMoodleSecurityKey(userAccount);
        if (token == null) throw new WebApplicationException(new RuntimeException("User has no security key."), 500);
        String parameter = "serviceshortnames[0]=" + MOODLE_SERVICE_NAME;
        String data = "";
        try {
            data = callMoodle(token, "core_webservice_get_site_info", parameter);
            JSONObject response = new JSONObject(data.toString());
            long userId = response.getLong("userid");
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

    private boolean hasParticipantEdge (Topic course, Topic user) {
        boolean value = false;
        Topic userAccount = course.getRelatedTopic(MOODLE_PARTICIPANT_EDGE, DEFAULT_ROLE_TYPE_URI,
                DEFAULT_ROLE_TYPE_URI, USER_ACCOUNT_TYPE_URI, true, true);
        if (userAccount != null && user.getId() == userAccount.getId()) return value = true;
        return value;
    }

    private boolean hasAggregatingCourseParentEdge (Topic child, Topic parent) {
        boolean value = false;
        Topic topic = child.getRelatedTopic(AGGREGATION_TYPE_URI, CHILD_ROLE_TYPE_URI,
                PARENT_ROLE_TYPE_URI, MOODLE_COURSE_URI, true, true);
        if (topic != null && parent.getId() == topic.getId()) return value = true;
        return value;
    }

    private boolean hasAggregatingSectionParentEdge (Topic child, Topic parent) {
        boolean value = false;
        Topic topic = child.getRelatedTopic(AGGREGATION_TYPE_URI, CHILD_ROLE_TYPE_URI,
                PARENT_ROLE_TYPE_URI, MOODLE_SECTION_URI, true, true);
        if (topic != null && parent.getId() == topic.getId()) return value = true;
        return value;
    }

    /** private String getParentMoodleItemId (Topic child) {
        String id = "";
        Topic topic = child.getRelatedTopic(AGGREGATION_TYPE_URI, CHILD_ROLE_TYPE_URI,
                PARENT_ROLE_TYPE_URI, MOODLE_ITEM_URI, true, true, null);
        id = (topic != null) ? topic.getUri().substring(ISIS_ITEM_URI_PREFIX.length()) : "0";
        return id;
    } **/

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

    private Topic getMoodleCourseTopic(long courseId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_COURSE_URI_PREFIX + courseId), true);
    }

    private Topic getMoodleSectionTopic(long sectionId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_SECTION_URI_PREFIX + sectionId), true);
    }

    private Topic getMoodleItemTopic(long itemId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_ITEM_URI_PREFIX + itemId), true);
    }

    private Topic createMoodleCourseTopic(JSONObject object, ClientState clientState) {
        try {
            long courseId = object.getLong("id");
            String shortName = object.getString("shortname");
            String fullName = object.getString("fullname");
            CompositeValueModel model = new CompositeValueModel();
            model.put(MOODLE_COURSE_NAME_URI, fullName);
            model.put(MOODLE_COURSE_SHORT_NAME_URI, shortName);
            TopicModel course = new TopicModel(ISIS_COURSE_URI_PREFIX + courseId, MOODLE_COURSE_URI, model);
            course.setUri(ISIS_COURSE_URI_PREFIX + courseId);
            Topic result = dms.createTopic(course, clientState);
            return result;
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Topic createMoodleSectionTopic(JSONObject object, int nr, ClientState clientState) {
        try {
            long sectionId = object.getLong("id");
            String name = object.getString("name");
            String summary = object.getString("summary");
            CompositeValueModel model = new CompositeValueModel();
            model.put(MOODLE_SECTION_NAME_URI, name);
            model.put(MOODLE_SECTION_SUMMARY_URI, summary);
            model.put(MOODLE_SECTION_ORDINAL_NR, nr);
            TopicModel section = new TopicModel(ISIS_SECTION_URI_PREFIX + sectionId, MOODLE_SECTION_URI, model);
            Topic result = dms.createTopic(section, clientState);
            return result;
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Topic createMoodleItemTopic(JSONObject object, ClientState clientState) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
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
            JSONArray contents = null;
            if (object.has("contents")) {
                contents = object.getJSONArray("contents");
                // fixme: per Moodle Item we currently can have just 1 FILE resp. URL // the last one takes it all //
                if (contents.length() > 1) log.warning("MoodleItem ("+itemId+") has more contents than we can process");
                for (int i = 0; i < contents.length(); i++) {
                    JSONObject resource = contents.getJSONObject(i);
                    /* = "", filename = "", filepath = "", fileurl = "",
                            userid = "", author = "", license = "";
                    long filesize = 0, timecreated = 0, timemodified = 0; */
                    String resourceType = resource.getString("type");
                    String fileUrl = "", fileName = "";
                    long last_modified = 0;
                    if (resourceType.equals("url")) { // Moodle Resource is an URL
                        fileUrl = resource.getString("fileurl");
                        // ### parse youtube and replace /watch?v=id with /embed/id in the url
                        model.put(MOODLE_ITEM_REMOTE_URL_URI, fileUrl);
                        // add it to the (to be created) Moodle Item
                        model.put(MOODLE_ITEM_TYPE_URI, resourceType);
                        if (resource.has("timemodified") && !resource.isNull("timemodified")) {
                            last_modified = resource.getLong("timemodified");
                        }
                        // urls timestamps (modified, created) are often null
                        model.put(MOODLE_ITEM_MODIFIED_URI, last_modified);
                        // alternatively: model.put(MOODLE_ITEM_HREF_URI, fileurl);
                    } else if (resourceType.equals("file")) { // Moodle Resource is an URL
                        // pages _and_ documents are of type file
                        fileName = resource.getString("filename");
                        fileUrl = resource.getString("fileurl");
                        long fileSize = resource.getLong("filesize");
                        // todo: existence check of file-item by, e.g. fileurl? (no id is present)
                        // Topic dmFile = createMoodleFileTopic(resource, clientState);
                        // add it to the (to be created) Moodle Item
                        // model.putRef(DEEPAMEHTA_FILE_URI, dmFile.getId());
                        model.put(MOODLE_ITEM_NAME_URI, fileName);
                        model.put(MOODLE_ITEM_REMOTE_URL_URI, fileUrl);
                        model.put(MOODLE_ITEM_SIZE_URI, fileSize);
                        String file_type = JavaUtils.getFileType(fileName); // Maybe null (e.g. for .ODT-Documents)
                        if (file_type == null) file_type = "Unknown";
                        model.put(MOODLE_ITEM_MEDIA_TYPE_URI, file_type);
                        model.put(MOODLE_ITEM_TYPE_URI, resourceType);
                        // we use "timemodified" (if not null) instead of "timecreated"
                        if (resource.has("timemodified") && !resource.isNull("timemodified")) {
                            last_modified = resource.getLong("timemodified");
                        }
                        model.put(MOODLE_ITEM_MODIFIED_URI, last_modified);
                    }
                }
            }
            // else if (contents["type"].equals("File") || equals("url")
            TopicModel item = new TopicModel(ISIS_ITEM_URI_PREFIX + itemId, MOODLE_ITEM_URI, model);
            Topic result = dms.createTopic(item, clientState);
            // log.info("CREATED ITEM => " + item.toJSON().toString());
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

    private Topic getUserAccountTopic(String username) {
        Topic accountTopic = null;
        Topic userTopic = dms.getTopic(USER_NAME_TYPE_URI, new SimpleValue(username), true);
        accountTopic = userTopic.getRelatedTopic(COMPOSITION_TYPE_URI, CHILD_ROLE_TYPE_URI, PARENT_ROLE_TYPE_URI,
                USER_ACCOUNT_TYPE_URI, true, false);
        return accountTopic;
    }

    private String getMoodleSecurityKey(Topic userAccount) {
        if (userAccount.hasProperty(MOODLE_SECURITY_KEY_URI)) {
            String token = (String) userAccount.getProperty(MOODLE_SECURITY_KEY_URI);
            return token;
        }
        return null;
    }

    private long getMoodleUserId(Topic userAccount) {
        if (userAccount.hasProperty(MOODLE_USER_ID_URI)) {
            String id = (String) userAccount.getProperty(MOODLE_USER_ID_URI);
            long moodle_user_id = Long.parseLong(id);
            return moodle_user_id;
        }
        return -1;
    }

    private boolean setMoodleUserId(Topic userAccount, long moodleUserId) {
        DeepaMehtaTransaction tx = dms.beginTx();
        userAccount.setProperty(MOODLE_USER_ID_URI, "" + moodleUserId + "", false);
        tx.success();
        tx.finish();
        return true;
    }

    private Topic getMoodleServiceUrl() {
        return dms.getTopic("uri", new SimpleValue(SERVICE_ENDPOINT_TYPE_URI), true);
    }

    private Topic checkAuthorization() {
        String username = aclService.getUsername();
        if (username == null) throw new WebApplicationException(401);
        return getUserAccountTopic(username);
    }

}
