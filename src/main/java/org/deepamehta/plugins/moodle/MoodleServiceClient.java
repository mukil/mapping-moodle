package org.deepamehta.plugins.moodle;

import de.deepamehta.core.Association;
import de.deepamehta.core.CompositeValue;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.*;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.plugins.accesscontrol.model.ACLEntry;
import de.deepamehta.plugins.accesscontrol.model.AccessControlList;
import de.deepamehta.plugins.accesscontrol.model.Operation;
import de.deepamehta.plugins.accesscontrol.model.UserRole;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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
    private String WEB_RESOURCE_TYPE_URI = "dm4.webbrowser.web_resource";

    private String MOODLE_PARTICIPANT_EDGE = "org.deepamehta.moodle.course_participant";

    private String MOODLE_COURSE_URI = "org.deepamehta.moodle.course";
    private String MOODLE_COURSE_NAME_URI = "org.deepamehta.moodle.course_name";
    private String MOODLE_COURSE_SHORT_NAME_URI = "org.deepamehta.moodle.course_short_name";

    private String MOODLE_SECTION_URI = "org.deepamehta.moodle.section";
    private String MOODLE_SECTION_NAME_URI = "org.deepamehta.moodle.section_name";
    private String MOODLE_SECTION_SUMMARY_URI = "org.deepamehta.moodle.section_summary";

    private String MOODLE_ITEM_URI = "org.deepamehta.moodle.item";
    private String MOODLE_ITEM_NAME_URI = "org.deepamehta.moodle.item_name";
    private String MOODLE_ITEM_ICON_URI = "org.deepamehta.moodle.item_icon";
    private String MOODLE_ITEM_DESC_URI = "org.deepamehta.moodle.item_description";
    private String MOODLE_ITEM_HREF_URI = "org.deepamehta.moodle.item_href";
    private String MOODLE_ITEM_TYPE_URI = "org.deepamehta.moodle.item_type";
    private String MOODLE_ITEM_MODIFIED_URI = "org.deepamehta.moodle.item_modified";
    private String MOODLE_ITEM_CREATED_URI = "org.deepamehta.moodle.item_created";
    private String MOODLE_ITEM_AUTHOR_URI = "org.deepamehta.moodle.item_author";
    private String MOODLE_ITEM_LICENSE_URI = "org.deepamehta.moodle.item_license";

    private String MOODLE_FILE_URI = "org.deepamehta.moodle.file";
    private String MOODLE_FILE_NAME_URI = "org.deepamehta.moodle.file_name";
    private String MOODLE_FILE_PATH_URI = "org.deepamehta.moodle.file_path";
    private String MOODLE_FILE_URL_URI = "org.deepamehta.moodle.file_url";
    private String MOODLE_FILE_SIZE_URI = "org.deepamehta.moodle.file_size";

    private String MOODLE_SECURITY_KEY_URI = "org.deepamehta.moodle.security_key";

    private String MOODLE_SERVICE_NAME = "eduzen_web_service";
    private String MOODLE_SERVICE_FORMAT = "moodlewsrestformat=json";

    private String ISIS_COURSE_URI_PREFIX = "de.tu-berlin.course.";
    private String ISIS_SECTION_URI_PREFIX = "de.tu-berlin.section.";
    private String ISIS_ITEM_URI_PREFIX = "de.tu-berlin.item.";

    private String filerepoPath = "/home/malt/Desktop/";

    @Override
    public void postInstall() {
        if (aclService != null) { // panic check (allPluginsMustBeActive)
            Topic serviceEndpointUri = getMoodleServiceUrl();
            aclService.setCreator(serviceEndpointUri.getId(), "Malte");
            aclService.setOwner(serviceEndpointUri.getId(), "Malte");
            AccessControlList aclist = new AccessControlList()
                    .addEntry(new ACLEntry(Operation.WRITE, UserRole.CREATOR));
            aclService.setACL(serviceEndpointUri.getId(), aclist);
        }
    }

    @Override
    public void init() {
        String configuredPath = System.getProperty("dm4.filerepo.path");
        if (configuredPath != null || !configuredPath.equals("")) filerepoPath = configuredPath + "/";
        log.info("Mapping Moodle Plugin set to run on => \"" + filerepoPath + "\"");
    }

    /**
     * @return list of all <code>courses</code> our user is currently enrolled in
     */
    @GET
    @Path("/courses")
    @Produces("application/json")
    public Topic getMyMoodleCourses(@HeaderParam("Cookie") ClientState clientState) {

        Topic userAccount = checkAuthorization();
        String token = getMoodleSecurityKey(userAccount);
        if (token == null) throw new WebApplicationException(new RuntimeException("User has no security key."), 500);

        long userId = getMoodleUserId(userAccount); // fixme: how to get the current userId
        if (userId == -1) throw new WebApplicationException(new RuntimeException("Unkown moodle user id."), 500);

        String parameter = "userid=" + userId;
        String data = callMoodle(token, "core_enrol_get_users_courses", parameter);
        // fixme: token and userId are not matched up against each other in this request, changing userId => exploit
        try {
            JSONArray response = new JSONArray(data.toString());
            // log.info("My (UserId: " +userId+ ", Token: " +token+ ") Moodle Courses are: \r\n " + response.toString());
            for (int i = 0; i < response.length(); i++) {
                JSONObject course = response.getJSONObject(i);
                Topic courseTopic = getMoodleCourseTopic(course.getLong("id"));
                if (courseTopic == null) {
                    courseTopic = createMoodleCourseTopic(course, clientState);
                    // log.info("Created at new Course => \r\n " + courseTopic.toJSON().toString());
                } else {
                    log.warning("NOT IMPLEMENTED YET => UPDATING EXISTING COURSE");
                    // todo: allow update of course name, shortname topics
                }
                if (!hasParticipantEdge(courseTopic, userAccount)) {
                    createParticipantEdge(courseTopic, userAccount, clientState);
                }
            }
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
            log.info("Going on, parsing Moodle Exception:");
            try {
                JSONObject exception = new JSONObject(data.toString());
                log.warning("MoodleException: " + exception.getString("message"));
            } catch (JSONException ex1) {
                Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        return userAccount;
    }

    @GET
    @Path("/course/{topicId}/content")
    @Produces("application/json")
    public Topic getCourseContents(@PathParam("topicId") int topicId, @HeaderParam("Cookie") ClientState clientState) {

        Topic userAccount = checkAuthorization();
        String token = getMoodleSecurityKey(userAccount);
        if (token == null) throw new WebApplicationException(new RuntimeException("User has no security key."), 500);

        long courseId = -1;
        Topic courseTopic = dms.getTopic(topicId, true, clientState);
        courseId = Long.parseLong(courseTopic.getUri().replaceAll(ISIS_COURSE_URI_PREFIX, ""));
        String parameter = "courseid=" + courseId;
        String data = callMoodle(token, "core_course_get_contents", parameter);
        // DEBUG-Information:
        // [ sections { id, name, summary, modules [{ id, name, description (just for label), modname, modicon, availablefrom, availableuntil, contents [
            // {type (either url or file), description (label),
            // resource: filename, filepath, filesize, author, license, fileurl, description
            // supported modules (by modname) shall be:
            // resource(file) (name, contents), url(fileurl) (name, contents), label (name, description), page(htmlfile) (name, contents)
            // unsupported modules (by modname) will be (though we'll keep the name and the url for each):
            // folder, quiz, assign, choice, book, glossary, forum
        // ] }] } ]
        // skipping summary, summaryformat, visible (?), availablefrom, availableuntil, indent,
        // if your not a participant of the requested course, moodle answers with a:
            // {"exception":"moodle_exception","errorcode":"errorcoursecontextnotvalid",
            // "message":"You cannot execute functions in the course context (course id:3).
            // The context error message was: Course or activity not accessible."}
        try {
            JSONArray response = new JSONArray(data.toString());
            for (int i = 0; i < response.length(); i++) {
                JSONObject section = response.getJSONObject(i);
                Topic sectionTopic = getMoodleSectionTopic(section.getLong("id"));
                if (sectionTopic == null) {
                    sectionTopic = createMoodleSectionTopic(section, clientState);
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
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
            try {
                JSONObject exception = new JSONObject(data.toString());
                log.warning("MoodleException: " + exception.getString("message"));
            } catch (JSONException ex1) {
                Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        return courseTopic;
    }

    @GET
    @Path("/user")
    public Topic setMoodleUserId(@HeaderParam("Cookie") ClientState clientState) throws WebApplicationException {

        Topic userAccount = checkAuthorization();
        String token = getMoodleSecurityKey(userAccount);
        if (token == null) throw new WebApplicationException(new RuntimeException("User has no security key."), 500);

        String parameter = "serviceshortnames[0]=" + MOODLE_SERVICE_NAME;
        String data = callMoodle(token, "core_webservice_get_site_info", parameter);
        try {
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
        }
        return userAccount;
    }

    @GET
    @Path("/file/{itemTopicId}")
    @Produces("application/json")
    public String getMoodleFile(@PathParam("itemTopicId") int itemId) {

        Topic userAccount = checkAuthorization();
        String token = getMoodleSecurityKey(userAccount);
        if (token == null) throw new WebApplicationException(new RuntimeException("User has no security key."), 500);

        long userId = getMoodleUserId(userAccount); // fixme: how to get the current userId
        if (userId == -1) throw new WebApplicationException(new RuntimeException("Unkown moodle user id."), 500);

        Topic moodleItem = dms.getTopic(itemId, true, null);
        // String moodleItemId = getParentMoodleItemId(moodleFile);
        Topic moodleFile = getMoodleFileTopic(moodleItem);
        String result = "";
        if (moodleFile != null) {
            // fixme: create "File"-Topic and drop "Moodle File" Item
            // and use: String mediaType = JavaUtils.getFileType(file.getName()) to leverage file-display
            // String filePath = moodleFile.getModel().getCompositeValueModel().getString(MOODLE_FILE_PATH_URI);
            String fileName = moodleFile.getModel().getCompositeValueModel().getString(MOODLE_FILE_NAME_URI);
            String fileUrl = moodleFile.getModel().getCompositeValueModel().getString(MOODLE_FILE_URL_URI);
            String localFilePath = filerepoPath;
            result = callMoodleFile(fileUrl, token, fileName, localFilePath); // store file temporarily
            log.info("Debug File Response Data => " + result);
        } else {
            log.warning("Could not fetch moodlefile-topic from Item => " + moodleItem.getId());
        }
        return result;
    }

    @POST
    @Path("/set/key/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String setMoodleKey(@PathParam("userId") int userId) {

            Topic userAccount = checkAuthorization();
            if (userAccount.getId() != userId) throw new WebApplicationException(new RuntimeException("Get a job."), 401);
        	/** Topic user = ;
            String isisKey = "...";
            user.setProperty(MOODLE_SECURITY_KEY_URI, isisKey, false);		// addToIndex=false **/
            return "";
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

    private String callMoodle (String key, String functionName, String params) {

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
            return response.toString();
        } catch (Exception ex) {
            throw new WebApplicationException(new RuntimeException(ex.getCause()), 500);
        }
    }

    private String callMoodleFile (String fileUrl, String token, String fileName, String filePath) {

        String queryUrl = fileUrl + "&token=" + token;
        log.info("MOODLE File Query URL => " + queryUrl);
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(queryUrl).openConnection();
            con.setRequestMethod("GET");
            con.setDoOutput(false);
            con.setUseCaches (false);
            con.setDoInput(true);

            //Get Response
            InputStream is = con.getInputStream();
            OutputStream outputStream = new FileOutputStream(new File(filePath + fileName));

            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = is.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            log.info("Done! Wrote File " + filePath + fileName);
            return "OK";
        } catch (Exception ex) {
            throw new WebApplicationException(new RuntimeException(ex.getCause()), 500);
        }
    }

    private boolean hasParticipantEdge (Topic course, Topic user) {
        boolean value = false;
        Topic userAccount = course.getRelatedTopic(MOODLE_PARTICIPANT_EDGE, DEFAULT_ROLE_TYPE_URI,
                DEFAULT_ROLE_TYPE_URI, USER_ACCOUNT_TYPE_URI, true, true, null);
        if (userAccount != null && user.getId() == userAccount.getId()) return value = true;
        return value;
    }

    private boolean hasAggregatingCourseParentEdge (Topic child, Topic parent) {
        boolean value = false;
        Topic topic = child.getRelatedTopic(AGGREGATION_TYPE_URI, CHILD_ROLE_TYPE_URI,
                PARENT_ROLE_TYPE_URI, MOODLE_COURSE_URI, true, true, null);
        if (topic != null && parent.getId() == topic.getId()) return value = true;
        return value;
    }

    private boolean hasAggregatingSectionParentEdge (Topic child, Topic parent) {
        boolean value = false;
        Topic topic = child.getRelatedTopic(AGGREGATION_TYPE_URI, CHILD_ROLE_TYPE_URI,
                PARENT_ROLE_TYPE_URI, MOODLE_SECTION_URI, true, true, null);
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

    private Topic getMoodleFileTopic (Topic item) {
        return item.getRelatedTopic(AGGREGATION_TYPE_URI, PARENT_ROLE_TYPE_URI,
                CHILD_ROLE_TYPE_URI, MOODLE_FILE_URI, true, true, null);
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

    private Topic getMoodleCourseTopic(long courseId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_COURSE_URI_PREFIX + courseId), true, null);
    }

    private Topic getMoodleSectionTopic(long sectionId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_SECTION_URI_PREFIX + sectionId), true, null);
    }

    private Topic getMoodleItemTopic(long itemId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_ITEM_URI_PREFIX + itemId), true, null);
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
            Topic result = dms.createTopic(course, clientState);
            return result;
        } catch (JSONException ex) {
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Topic createMoodleSectionTopic(JSONObject object, ClientState clientState) {
        try {
            long sectionId = object.getLong("id");
            String name = object.getString("name");
            String summary = object.getString("summary");
            CompositeValueModel model = new CompositeValueModel();
            model.put(MOODLE_SECTION_NAME_URI, name);
            model.put(MOODLE_SECTION_SUMMARY_URI, summary);
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
            JSONArray contents = null;
            if (object.has("contents")) {
                contents = object.getJSONArray("contents");
                for (int i = 0; i < contents.length(); i++) {
                    JSONObject resource = contents.getJSONObject(i);
                    /* = "", filename = "", filepath = "", fileurl = "",
                            userid = "", author = "", license = "";
                    long filesize = 0, timecreated = 0, timemodified = 0; */
                    String resourceType = resource.getString("type");
                    String fileUrl = "";
                    long last_modified = 0;
                    if (resourceType.equals("url")) { // Moodle Resource is an URL
                        fileUrl = resource.getString("fileurl");
                        CompositeValueModel webModel = new CompositeValueModel();
                        // parse youtube and replace /watch?v=id with /embed/id in the url
                        // fixme: existence check of Web Resource by URL
                        webModel.put("dm4.webbrowser.url", fileUrl);
                        webModel.put("dm4.webbrowser.web_resource_description", "");
                        TopicModel web = new TopicModel(WEB_RESOURCE_TYPE_URI, webModel);
                        Topic webResource = dms.createTopic(web, clientState);
                        // add it to the (to be created) Moodle Item
                        model.putRef(WEB_RESOURCE_TYPE_URI, webResource.getId());
                        model.put(MOODLE_ITEM_TYPE_URI, resourceType);
                        if (resource.has("timemodified")) {
                            last_modified = resource.getLong("timemodified");
                        }
                        // urls timestamps (modified, created) are often null
                        model.put(MOODLE_ITEM_MODIFIED_URI, last_modified);
                        // alternatively: model.put(MOODLE_ITEM_HREF_URI, fileurl);
                    } else if (resourceType.equals("file")) { // Moodle Resource is an URL
                        // pages _and_ documents are of type file
                        // todo: existence check of file-item by, e.g. fileurl? (no id is present)
                        Topic moodleFile = createMoodleFileTopic(resource, clientState);
                        // add it to the (to be created) Moodle Item
                        model.putRef(MOODLE_FILE_URI, moodleFile.getId());
                        model.put(MOODLE_ITEM_TYPE_URI, resourceType);
                        // we use "timemodified" (if not null) instead of "timecreated"
                        if (resource.has("timemodified")) {
                            last_modified = resource.getLong("timemodified");
                        }
                        model.put(MOODLE_ITEM_MODIFIED_URI, last_modified);
                    }
                }
            }
            // else if (contents["type"].equals("File") || equals("url")
            TopicModel item = new TopicModel(ISIS_ITEM_URI_PREFIX + itemId, MOODLE_ITEM_URI, model);
            Topic result = dms.createTopic(item, clientState);
            log.info("CREATED ITEM => " + item.toJSON().toString());
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

    private Topic createMoodleFileTopic(JSONObject content, ClientState clientState) {
        DeepaMehtaTransaction bx = dms.beginTx();
        String resourceType = "", filename = "", filepath = "", fileurl = "",
            userid = "", author = "", license = "";
        long filesize = 0, timecreated = 0, timemodified = 0;
        try {
            CompositeValueModel fileComposite = new CompositeValueModel();
            filename = content.getString("filename");
            fileurl = content.getString("fileurl");
            filepath = content.getString("filepath");
            filesize = content.getLong("filesize");
            // we currently skip "userid", "sortorder", "author", "license"
            // put values into child topics.. here and then create the topicmodel
            fileComposite.put(MOODLE_FILE_NAME_URI, filename);
            fileComposite.put(MOODLE_FILE_PATH_URI, filepath);
            fileComposite.put(MOODLE_FILE_URL_URI, fileurl); // get?
            fileComposite.put(MOODLE_FILE_SIZE_URI, filesize);
            TopicModel fileModel = new TopicModel(MOODLE_FILE_URI, fileComposite);
            Topic moodleFile = dms.createTopic(fileModel, clientState);
            log.info("CREATED Moodle FILE => " + moodleFile.toJSON().toString());
            bx.success();
            return moodleFile;
        } catch (JSONException ex) {
            bx.failure();
            Logger.getLogger(MoodleServiceClient.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            bx.finish();
        }
        return null;
    }


    private Topic getUserAccountTopic(String username) {
        Topic accountTopic = null;
        Topic userTopic = dms.getTopic(USER_NAME_TYPE_URI, new SimpleValue(username), true, null);
        accountTopic = userTopic.getRelatedTopic(COMPOSITION_TYPE_URI, CHILD_ROLE_TYPE_URI, PARENT_ROLE_TYPE_URI,
                USER_ACCOUNT_TYPE_URI, true, false, null);
        return accountTopic;
    }

    /** fixme: treat this as a secret */
    private String getMoodleSecurityKey(Topic userAccount) {
        String token = null;
        CompositeValueModel userModel = userAccount.getModel().getCompositeValueModel();
        if (userModel.has(MOODLE_SECURITY_KEY_URI)) {
            token = userModel.getString(MOODLE_SECURITY_KEY_URI);
        }
        return token;
    }

    private long getMoodleUserId(Topic userAccount) {
        long id = -1;
        CompositeValueModel userModel = userAccount.getModel().getCompositeValueModel();
        if (userModel.has("org.deepamehta.moodle.user_id")) {
            id = userModel.getLong("org.deepamehta.moodle.user_id");
        }
        return id;
    }

    private boolean setMoodleUserId(Topic userAccount, long userId) {
        CompositeValueModel userModel = userAccount.getCompositeValue().getModel();
        userModel.put("org.deepamehta.moodle.user_id", userId);
        userAccount.setCompositeValue(userModel, null, new Directives());
        CompositeValue composite = userAccount.getCompositeValue();
        if (composite.has("org.deepamehta.moodle.user_id") &&
            composite.getLong("org.deepamehta.moodle.user_id") == userId) {
            return true;
        }
        return false;
    }

    private Topic getMoodleServiceUrl() {
        return dms.getTopic("uri", new SimpleValue("org.deepamehta.config.moodle_service_url"), true, null);
    }

    private Topic checkAuthorization() throws WebApplicationException {
        String username = aclService.getUsername();
        if (username == null) throw new WebApplicationException(new RuntimeException("Please log in first."), 500);
        return getUserAccountTopic(username);
    }

}
