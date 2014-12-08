package org.deepamehta.plugins.moodle;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.*;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.service.event.AllPluginsActiveListener;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.core.util.JavaUtils;
import de.deepamehta.plugins.accesscontrol.event.PostLoginUserListener;
import de.deepamehta.plugins.accesscontrol.model.ACLEntry;
import de.deepamehta.plugins.accesscontrol.model.AccessControlList;
import de.deepamehta.plugins.accesscontrol.model.Operation;
import de.deepamehta.plugins.accesscontrol.model.UserRole;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
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
 * @version 1.2.1-SNAPSHOT
 *
 */

@Path("/moodle")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MoodleServiceClient extends PluginActivator implements PostLoginUserListener,
                                                                    AllPluginsActiveListener {

    private static Logger logger = Logger.getLogger(MoodleServiceClient.class.getName());

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // ### Consume NotifcationService
    @Inject
    private AccessControlService aclService;

    // --- URIs DeepaMehta and all plugins in use

    public static final String WS_MOODLE_NAME = "Moodle";
    public static final String WS_MOODLE_URI = "org.deepamehta.workspaces.moodle";
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
    public static final String MOODLE_ITEM_CREATED_URI = "org.deepamehta.moodle.item_created";
    public static final String MOODLE_ITEM_AUTHOR_URI = "org.deepamehta.moodle.item_author";
    public static final String MOODLE_ITEM_LICENSE_URI = "org.deepamehta.moodle.item_license";
    public static final String MOODLE_ITEM_SIZE_URI = "org.deepamehta.moodle.item_size";

    // --- Tagging Notes Plugin URI

    public static final String PLUGIN_URI_TAGGING_NOTES = "org.deepamehta.eduzen-tagging-notes";

    // --- Data instance URIs for ISIS 2

    public static final String ISIS_COURSE_URI_PREFIX = "de.tu-berlin.course.";
    public static final String ISIS_SECTION_URI_PREFIX = "de.tu-berlin.section.";
    public static final String ISIS_ITEM_URI_PREFIX = "de.tu-berlin.item.";

    // --- ISIS 2 Webservice related URIs

    private final String WEBSERVICE_CLIENT_OPTION_OPTION = "org.deepamehta.moodle.web_service_client_option";
    private final String CLIENT_OPTION_ENDPOINT_URL_URI = "org.deepamehta.moodle.web_service_endpoint_url";
    private final String CLIENT_OPTION_USE_HTTPS = "org.deepamehta.moodle.use_https";
    private final String CLIENT_OPTION_JAVA_KEY_STORE_PATH = "org.deepamehta.moodle.jks_path";
    private final String CLIENT_OPTION_JAVA_KEY_STORE_PASS = "org.deepamehta.moodle.jks_pass";
    // The Username eligible to
    // a) edit the moodle service-settings (endpoint) and all other system-created items, as well is allowed to
    // b) set "Tags" on each "Moodle Course" (which is necessary) before they can be synced
    private final String USERNAME_OF_SETTINGS_ADMINISTRATOR = "Malte";
    private final String MOODLE_SECURITY_KEY_URI = "org.deepamehta.moodle.security_key";
    private final String MOODLE_USER_ID_URI = "org.deepamehta.moodle.user_id";
    private final String MOODLE_SERVICE_NAME = "eduzen_web_service";
    private final String MOODLE_SERVICE_FORMAT = "moodlewsrestformat=json";

    // --- Deepamehta 4 URIs

    private final String URI_URI = "uri";
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


    // ------------------------------------------------------------------------------------------------- Public Methods

    // --
    // --- Hook implementations
    // --

    @Override
    public void init() {
        if (aclService != null) {
            DeepaMehtaTransaction tx = dms.beginTx();
            try {
                // ### remember: topics introduced via migration1 have no ACLs set.
                // 1) correct ACL of migration1 workspace topic
                Topic moodleWs = dms.getTopic(URI_URI, new SimpleValue(WS_MOODLE_URI));
                setDefaultMoodleAdminACLEntries(moodleWs);
                // 2) Correct ACL of topics in migration4
                ResultList<RelatedTopic> client_options = dms.getTopics(WEBSERVICE_CLIENT_OPTION_OPTION, 0);
                for (RelatedTopic client_option : client_options) {
                    setDefaultMoodleAdminACLEntries(client_option);
                }
                tx.success();
            } finally {
                tx.finish();
            }
        }
    }

    @Override
    public void postLoginUser(String username) {

        startMoodleSynchronization(username);

    }

    @Override
    public void allPluginsActive() {

        Properties systemProps = System.getProperties();
        // systemProps.setProperty("javax.net.ssl.trustStore", PATH_TO_JAVA_KEYSTORE);
        // systemProps.setProperty("javax.net.ssl.trustStorePassword", PASS_FOR_JAVA_KEYSTORE);
        logger.info("MoodleCheck: JRE Keystore (Truststore) is at "
                + "\"" + systemProps.getProperty("javax.net.ssl.trustStore") +"\"");

    }

    // ### Revise 
    // Transactions and 
    // Exceptions

    // --
    // --- Moodle Plugin Implementation
    // --

    /**
     * Relates the moodle-security-key to our currently logged-in user-account. 
     * 
     * @param   id      topicId of userAccount
     * @param   input   JSON payload containing { "moodle_key": yourkey }
     **/
    @POST
    @Path("/key/{id}")
    @Transactional
    public String setMoodleSecurityKey(@PathParam("id") int id, final String input ) {
        try {
            // 1) Store security key for this user
            Topic userAccount = checkAuthorization();
            if (userAccount.getId() != id) throw new WebApplicationException(new RuntimeException("Not allowed"), 401);
            Topic user = dms.getTopic(id).loadChildTopics();
            JSONObject payload = new JSONObject(input);
            String moodle_key = payload.getString("moodle_key");
            // 2) prevent setting of keys which do not look like a moodle security key
            if (moodle_key.equals("") || moodle_key.length() != 32) {
                throw new RuntimeException("Sorry but that does not look like a \"Moodle security key.\"");
            }
            user.setProperty(MOODLE_SECURITY_KEY_URI, moodle_key, false); // addToIndex=false **/
            // 3) Fetch user_id from moodle installation (to be able to start querying the service)
            fetchAndSetMoodleUserId(userAccount);
            return "{ \"result\": \"OK\"}";
        } catch (JSONException ex) {
            logger.warning("We could not set your moodle user id because of an error while parsing the response "
                    + ex.getMessage());
            return "{ \"result\": \"FAIL\"}";
        } catch (Exception wex) {
            logger.warning("We could not set your moodle user id " + wex.getMessage()
                    + " (" + wex.getCause().toString() + ")");
            return "{ \"result\": \"FAIL\"}";
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
    @Transactional
    public String startMoodleSynchronization(@PathParam("username") String username) {

        final Topic user = checkAuthorization();
        // 1) Twofold sanity check
        if (!user.getSimpleValue().toString().equals(username)) {
            logger.info("MoodleServiceClient sanity check failed " 
                + username + " != " + user.getSimpleValue().toString());
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

        final String user_name = user.getChildTopics().getString(USER_NAME_TYPE_URI).toString();
        final Topic local_user = user;

        new Thread() {

            @Override
            public void run() {

                logger.info("Started MoodleServiceClient-Synchronization Thread due to a _login-Event .. ");
                // 2) Fetch Moodle Security Key for newly logged in user
                String key = getMoodleSecurityKeyWithoutAuthCheck(local_user);
                if (key != null && key.length() == 32) { // 2) Check if it looks like a legit one for MOODLE
                    // todo: check if we have a MoodleUserID for this user ..
                    logger.info("MoodleServiceclient found a security key for \"" + user_name + "\" which looks legit..");
                } else {
                    throw new RuntimeException("This user has no \"Moodle Security Key\" set no SYNC.. ");
                }
                // 3) Ask for potentially new \"Moodle Courses\" ..
                logger.info("MoodleServiceClient checking for news in all \"Moodle Courses\" of \""+user_name+"\"");
                //    and create them if necessary / yet unknown to our system/installation
                getMoodleCoursesWithoutAuth(local_user, getMoodleUserId(local_user), key);
                // 4) Check for new \"Moodle Items\" in each tagged \"Moodle Course\" our user is "participating"
                ResultList<RelatedTopic> courses = getMoodleCoursesByUser(local_user);
                for (RelatedTopic course : courses) {
                    // 5) excluse Moodle Courses with no "Moodle Course Hashtag" set from synchronziations
                    course.loadChildTopics(TAG_URI);
                    if (course.getChildTopics().has(TAG_URI) &&
                        course.getChildTopics().getTopics(TAG_URI).size() > 0) {
                        List<Topic> courseHashtags = course.getChildTopics().getTopics(TAG_URI);
                        logger.info("MoodleServiceClient SYNC course \"" + course.getSimpleValue() +"\" "
                                + "under " +courseHashtags.size() + " hashtags:");
                        for (Topic hashtag : courseHashtags) {
                            logger.info("\t#" + hashtag.getSimpleValue());
                        }
                        getCourseContentsWithoutAuth(course.getId(), key, courseHashtags);
                    } else {
                        logger.info("MoodleServiceClient waiting with SYNC cause of missing #Hashtag (on \""
                                + course.getSimpleValue() + "\")");
                    }
                }
            }
        }.start();
    }

    /** Fetches and relates the internal moodle-user-id to our currently logged-in user-account. **/
    private void fetchAndSetMoodleUserId(Topic userAccount) throws WebApplicationException {

        String token = getMoodleSecurityKeyWithoutAuthCheck(userAccount);
        if (token == null) throw new WebApplicationException(new RuntimeException("User has no security key."), 500);
        String parameter = "serviceshortnames[0]=" + MOODLE_SERVICE_NAME;
        String data = "";
        try {
            data = callMoodle(token, "core_webservice_get_site_info", parameter);
            JSONObject response = new JSONObject(data.toString());
            long userId = response.getLong("userid");
            logger.info("Set Moodle User ID => " + userId);
            setMoodleUserId(userAccount, userId);
        } catch (JSONException ex) {
            logger.warning("Moodle JSONException " + ex.getMessage().toString());
            try {
                JSONObject exception = new JSONObject(data.toString());
                logger.warning("MoodleResponseException is \"" + exception.getString("message") +"\"");
            } catch (JSONException ex1) {
                throw new RuntimeException(ex1);
            }
        } catch (MoodleConnectionException mc) {
            logger.warning("MoodleConnectionException \"" + mc.message + "\" (" + mc.status + ")");
            throw new WebApplicationException(mc, mc.status);
        }
    }

    private void getMoodleCoursesWithoutAuth(Topic userAccount, long moodleUserId, String token) {
        String parameter = "userid=" + moodleUserId;
        String data = "";
        try {
            data = callMoodle(token, "core_enrol_get_users_courses", parameter);
            if (data.indexOf("webservice_access_exception") != -1) {
                logger.warning("Looks like external service (webservice feature) is not \"Enabled\" or the called function"
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
                    courseTopic = createMoodleCourseTopic(course);
                    if (courseTopic != null) {
                        // 2) Fix ACLEntries (caused by missing request-scope in Thread-local)
                        setDefaultMoodleAdminACLEntries(courseTopic); // just "admin" can edit course-items
                        // sendClientNotification("New Moodle Course", courseTopic);
                    } else {
                        logger.info("OMITTING HIDDEN MoodleCourse \"" + course.getString("shortname") + "\"");
                    }
                } else {
                    // 2) Update existing item
                    updateMoodleCourseTopic(courseTopic, course);
                }
                // 3) Relate to course item
                if (!hasParticipantEdge(courseTopic, userAccount)) {
                    createParticipantEdge(courseTopic, userAccount);
                }
            }
        } catch (JSONException ex) {
            try {
                JSONObject response = new JSONObject(data.toString());
                String exception = response.getString("exception");
                throw new WebApplicationException(new MoodleConnectionException(exception, 500), 500);
            } catch (JSONException ex1) {
                throw new RuntimeException(ex1);
            }
        } catch (MoodleConnectionException mc) {
            throw new WebApplicationException(new Throwable(mc.message), mc.status);
        }
    }

    private Topic getCourseContentsWithoutAuth(long topicId, String token, List<Topic> hashtags) {
        long courseId = -1;
        Topic courseTopic = dms.getTopic(topicId).loadChildTopics();
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
                    sectionTopic = createMoodleSectionTopic(section, i);
                    if (sectionTopic != null) {
                        // Fix Section-ACL-Entries so that "admin" can edit them
                        setDefaultMoodleAdminACLEntries(sectionTopic);
                        // sendClientNotification("New Moodle Section in \"" +
                           //     courseTopic.getSimpleValue().toString() + "\"", sectionTopic);
                    }
                // 2) Update existing \"Moodle section\"
                } else {
                    updateMoodleSectionTopic(sectionTopic, section);
                }
                // 3) Create or Update all \"Moodle Items\"
                if (sectionTopic != null) { // (if section is not null/ hidden)
                    JSONArray modules = section.getJSONArray("modules");
                    for (int k = 0; k < modules.length(); k++) {
                        JSONObject item = modules.getJSONObject(k);
                        Topic itemTopic = getMoodleItemTopic(item.getLong("id"));
                        if (itemTopic == null) {
                            itemTopic = createMoodleItemTopic(item, hashtags);
                            if (itemTopic != null) {
                                // Fix ACL so that all "Moodle"-WS Members can edit these items
                                setDefaultMoodleGroupACLEntries(itemTopic);
                                Association creator_edge = assignDefaultAuthorship(itemTopic);
                                if (creator_edge != null) setDefaultMoodleGroupACLEntries(creator_edge);
                            }
                        } else {
                            updateMoodleItemTopic(itemTopic, courseTopic, item);
                        }
                        if (!hasAggregatingSectionParentEdge(itemTopic, sectionTopic)) {
                            createAggregatingSectionEdge(sectionTopic, itemTopic);
                        }
                    }
                }
                // 4) Assign new \"Moodle section\" to course (if section is not null/hidden)
                if (!hasAggregatingCourseParentEdge(sectionTopic, courseTopic)) {
                    createAggregatingCourseEdge(courseTopic, sectionTopic);
                }
            }
            logger.info("MoodleServiceClient finished loading materials for course \"" 
                + courseTopic.getSimpleValue()+"\"");
        } catch (JSONException ex) {
            logger.warning("# ROLLBACK!");
            logger.warning("Could not get contents for \"Moodle Course\"-Topic: " 
                + ex.getMessage() + "(" + ex.getClass() + ")");
            try {
                JSONObject exception = new JSONObject(data.toString());
                String message = exception.getString("message");
                throw new WebApplicationException(new MoodleConnectionException(message, 500), 500);
            } catch (JSONException ex1) {
                throw new RuntimeException(ex1);
            }
        } catch (MoodleConnectionException mc) {
            throw new WebApplicationException(mc, mc.status);
        }
        return courseTopic;
    }

    private String callMoodle (String key, String functionName, String params) throws MoodleConnectionException {

        Topic serviceUrl = getMoodleEndpointUrl();
        String endpointUri = serviceUrl.getSimpleValue().value().toString();
        String queryUrl = endpointUri + "?wstoken=" + key + "&wsfunction=" + functionName + "&" + MOODLE_SERVICE_FORMAT;
        // "&service=" + MOODLE_SERVICE_NAME +
        try {
            if (isHTTPSConfigured()) {
                // Many thanks to "Bruno" for formulating the following idea/approach at:
                // http://stackoverflow.com/questions/10267968/error-when-opening-https-url-keycertsign-bit-is-not-set
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                KeyStore ks = KeyStore.getInstance("JKS");
                Topic client_jks_option = getJavaKeyStorePath();
                FileInputStream fis = new FileInputStream(client_jks_option.getSimpleValue().toString());
                ks.load(fis, getJavaKeyStorePass().getSimpleValue().toString().toCharArray());
                fis.close();
                tmf.init(ks);
                //
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                //
                HttpsURLConnection con = (HttpsURLConnection) new URL(queryUrl).openConnection();
                con.setSSLSocketFactory(sslContext.getSocketFactory());
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
                    logger.info("MoodleConnection HTTP Status \"" + con.getResponseCode() + "\"");
                    throw new MoodleConnectionException("MoodleConnection has thrown an error", con.getResponseCode());
                }
                // Get Response
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
                        + "are completely deactivated or the function is not part of the \"External services\" "
                            + "definition.";
                    logger.info(message);
                    throw new MoodleConnectionException(message, 204);
                }
                return response.toString();
            } else {
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
                    logger.warning("MoodleConnection HTTP Status \"" + con.getResponseCode() + "\"");
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
                        + "are completely deactivated or the function is not part of the \"External services\" "
                            + "definition.";
                    logger.warning(message);
                    throw new MoodleConnectionException(message, 204);
                }
                return response.toString();
            }
        } catch (KeyManagementException ex) {
            logger.warning("Moodle KeyManagementException " + ex.getMessage());
            throw new RuntimeException(ex);
        } catch (CertificateException ex) {
            logger.warning("Moodle CertificatieException " + ex.getMessage());
            throw new RuntimeException(ex);
        } catch (KeyStoreException ex) {
            logger.warning("Moodle KeyStoreException " + ex.getMessage());
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            logger.warning("Moodle Cypher NoSuchAlgorithmException " + ex.getMessage());
            throw new RuntimeException(ex);
        } catch (MoodleConnectionException ex) {
            logger.warning("Moodle ConnectionException " + ex.message + "(" + ex.status + ")");
            throw new MoodleConnectionException(ex.message, ex.status);
        } catch (MalformedURLException ml) {
            logger.warning("Moodle Malformed URL Exception .. " + ml.getMessage().toString());
            throw new MoodleConnectionException("DeepaMehta could not connect to malformed url: \"" + queryUrl + "\"",
                    404);
        } catch (IOException ex) {
            logger.warning("Moodle I/O Exception .. " + ex.getMessage().toString());
            throw new MoodleConnectionException("DeepaMehta could not connect to \"" + queryUrl + "\"", 500);
        }
    }

    private void createParticipantEdge(Topic courseTopic, Topic userAccount) {
        AssociationModel participantEdge = new AssociationModel(MOODLE_PARTICIPANT_EDGE,
                new TopicRoleModel(courseTopic.getId(), DEFAULT_ROLE_TYPE_URI),
                new TopicRoleModel(userAccount.getId(), DEFAULT_ROLE_TYPE_URI));
        Association edge = dms.createAssociation(participantEdge);
    }

    private void createAggregatingCourseEdge (Topic courseTopic, Topic sectionTopic) {
        AssociationModel aggregationEdge = new AssociationModel(AGGREGATION_TYPE_URI,
                new TopicRoleModel(courseTopic.getId(), PARENT_ROLE_TYPE_URI),
                new TopicRoleModel(sectionTopic.getId(), CHILD_ROLE_TYPE_URI));
        Association edge = dms.createAssociation(aggregationEdge);
    }

    private void createAggregatingSectionEdge (Topic sectionTopic, Topic itemTopic) {
        AssociationModel aggregationEdge = new AssociationModel(AGGREGATION_TYPE_URI,
                new TopicRoleModel(sectionTopic.getId(), PARENT_ROLE_TYPE_URI),
                new TopicRoleModel(itemTopic.getId(), CHILD_ROLE_TYPE_URI));
        Association edge = dms.createAssociation(aggregationEdge);
    }

    private Topic createMoodleCourseTopic(JSONObject object) {
        Topic result = null;
        try {
            if (object.getInt("visible") == 0) return null;
            long courseId = object.getLong("id");
            String shortName = object.getString("shortname");
            String fullName = object.getString("fullname");
            ChildTopicsModel model = new ChildTopicsModel();
            model.put(MOODLE_COURSE_NAME_URI, fullName);
            model.put(MOODLE_COURSE_SHORT_NAME_URI, shortName);
            TopicModel course = new TopicModel(ISIS_COURSE_URI_PREFIX + courseId, MOODLE_COURSE_URI, model);
            course.setUri(ISIS_COURSE_URI_PREFIX + courseId);
            result = dms.createTopic(course);
            assignToMoodleWorkspace(result);
        } catch (JSONException ex) {
            logger.warning("Could not create \"Moodle Course\"-Topic: " 
                + ex.getMessage() + "(" + ex.getClass() + ")");
        }
        return result;
    }

    private Topic updateMoodleCourseTopic(Topic course, JSONObject object) {
        try {
            boolean update_this = false;
            String new_name = object.getString("shortname");
            String new_fullname = object.getString("fullname");
            if (!course.getChildTopics().getString(MOODLE_COURSE_SHORT_NAME_URI).equals(new_name) ||
                !course.getChildTopics().getString(MOODLE_COURSE_NAME_URI).equals(new_fullname)) {
                update_this = true;
            }
            // Update (if there were changes)
            if (update_this) {
                ChildTopicsModel model = new ChildTopicsModel();
                model.put(MOODLE_COURSE_NAME_URI, new_name);
                model.put(MOODLE_COURSE_SHORT_NAME_URI, new_fullname);
                dms.updateTopic(new TopicModel(course.getId(), model));
                // todo: add to usage report
            }
            return course;
        } catch (JSONException ex) {
            logger.warning("Could not update \"Moodle Course\"-Topic: " 
                + ex.getMessage() + "(" + ex.getClass() + ")");
        }
        return null;
    }

    private Topic createMoodleSectionTopic(JSONObject object, int nr) {
        try {
            if (object.getInt("visible") == 0) return null; // item is hidden, do not create it
            long sectionId = object.getLong("id");
            String name = object.getString("name");
            String summary = object.getString("summary");
            ChildTopicsModel model = new ChildTopicsModel();
            model.put(MOODLE_SECTION_NAME_URI, name);
            model.put(MOODLE_SECTION_SUMMARY_URI, summary);
            model.put(MOODLE_SECTION_ORDINAL_NR, nr);
            TopicModel section = new TopicModel(ISIS_SECTION_URI_PREFIX + sectionId, MOODLE_SECTION_URI, model);
            // OWNER AND CREATOR will be the (logged in user) who triggered this method IN ANY CASE
            Topic result = dms.createTopic(section);
            setDefaultMoodleAdminACLEntries(result); // just "admin" can edit these
            assignToMoodleWorkspace(result);
            return result;
        } catch (JSONException ex) {
            logger.warning("Could not create \"Moodle Section\"-Topic: " 
                + ex.getMessage() + "(" + ex.getClass() + ")");
        }
        return null;
    }

    private Topic updateMoodleSectionTopic(Topic section, JSONObject object) {
        try {
            boolean update_this = false;
            String new_name = object.getString("name");
            String new_summary = object.getString("summary");
            // Custom comparison of values
            if (!section.getChildTopics().getString(MOODLE_SECTION_NAME_URI).equals(new_name) ||
                !section.getChildTopics().getString(MOODLE_SECTION_SUMMARY_URI).equals(new_summary)) {
                update_this = true;
            }
            // Update (if there were changes)
            if (update_this) {
                ChildTopicsModel model = new ChildTopicsModel();
                model.put(MOODLE_SECTION_NAME_URI, new_name);
                model.put(MOODLE_SECTION_SUMMARY_URI, new_summary);
                section.setChildTopics(model);
            }
        } catch (JSONException ex) {
            logger.warning("Could not update \"Moodle Section\"-Topic: " 
                + ex.getMessage() + "(" + ex.getClass() + ")");
        }
        return null;
    }
    
    /** ### private Transaction still necessary? */
    private Topic createMoodleItemTopic(JSONObject object, List<Topic> hashtags) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // 0) Checki if given moodle item is actually "hidden" and if not remember moodle-id
            if (object.getInt("visible") == 0) return null; // item is hidden, do not create it
            long itemId = object.getLong("id");
            // 1) Parse some generic information for this item
            ChildTopicsModel model = parseGenericsToItemModel(object);
            // 2) and if it's a material (typeof "resource" or "url") process its list of "contents"
            JSONArray contents = null;
            if (object.has("contents")) {
                contents = object.getJSONArray("contents");
                for (int i = 0; i < contents.length(); i++) { // (actually never in use, but possible)
                    JSONObject resource = contents.getJSONObject(i);
                    parseResourceToItemModel(model, resource);
                    // ..) equip moodle item with timestamp of or resource (e.g. files)
                    parseTimestampsToItemModel(model, resource);
                }
            } else {
                // 3) equip any other item with default timestamps (or the timestamps of the object, not resource)
                parseTimestampsToItemModel(model, object);
            }
            // 4) equip moodle item with every hashtag set on the course
            for (Topic hashtag : hashtags) {
                model.addRef(TAG_URI, hashtag.getId());
            }
            // 5) construct our internal uri for any moodl item
            TopicModel item = new TopicModel(ISIS_ITEM_URI_PREFIX + itemId, MOODLE_ITEM_URI, model);
            Topic result = dms.createTopic(item);
            // 6) fix workspace assignment for shared editing (tagging) of moodle items via webclient
            assignToMoodleWorkspace(result);
            // sendTopicNotification("New Moodle Item", result);
            tx.success();
            return result;
        } catch (JSONException ex) {
            tx.failure();
            logger.warning("Could not create \"Moodle Item\"-Topic: " 
                + ex.getMessage() + "(" + ex.getClass() + ")");
        } finally {
            tx.finish();
        }
        return null;
    }

    private Topic updateMoodleItemTopic(Topic item, Topic courseTopic, JSONObject object) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // 0) parse new generic infos to new model
            ChildTopicsModel model = parseGenericsToItemModel(object);
            // 1) parse infos specific to resources to model
            JSONArray contents = null;
            long last_modified_in_moodle = 0;
            if (object.has("contents")) {
                contents = object.getJSONArray("contents");
                for (int i = 0; i < contents.length(); i++) { // (no observation that this is  >1, but possible)
                    // if list of contents in a module is >1, currently the last one succeeds
                    JSONObject resource = contents.getJSONObject(i);
                    parseResourceToItemModel(model, resource);
                    parseTimestampsToItemModel(model, resource);
                }
            } else {
                // 2) equip ay other item with the new (moodle or our default) timestamps first
                parseTimestampsToItemModel(model, object);
            }
            // 2) start to perform update checks on contents of this (new) item
            boolean update_this = false;
            if (!item.getSimpleValue().toString().equals(model.getString(MOODLE_ITEM_NAME_URI))) { // by item-name
                // log.info("MoodleServiceClient: The name of the item has changed: ITEM is TO_BE_UPDATED");
                update_this = true;
            } else if (item.getChildTopics().getString(MOODLE_ITEM_TYPE_URI).equals("url")) { // by url-value
                // on "url"-items we perform a comparison by-value (cause timestamps are always missing)
                if (!item.getChildTopics().getString(MOODLE_ITEM_REMOTE_URL_URI)
                        .equals(model.getString(MOODLE_ITEM_REMOTE_URL_URI))) {
                    // log.info("MoodleServiceClient: The url-value changed: ITEM is TO_BE_UPDATED");
                    update_this = true;
                }
            } else if (item.getChildTopics().has(MOODLE_ITEM_MODIFIED_URI)) {
                // note: sometimes int sometimes long (webclient!) cause of neo4j and moodles timestamps/1000
                // check the last_modification timestamp from on our topic (in our current DB)
                long existing_timestamp = item.getChildTopics().getLong(MOODLE_ITEM_MODIFIED_URI);
                if (existing_timestamp < last_modified_in_moodle) { // lets write new contents to our \"Moodle Item\"
                    logger.fine("MoodleServiceClient: The remote Last-Modified-Timestamp indicates: "
                            + "ITEM is TO_BE_UPDATED");
                    update_this = true;
                }
            }

            if (update_this) {
                // 3) write update
                dms.updateTopic(new TopicModel(item.getId(), model));
                // sendClientNotification("Changed Moodle Item in \""
                   //     + courseTopic.getSimpleValue().toString() + "\"", item);
            }
            tx.success();
            return item;
        } catch (JSONException ex) {
            tx.failure();
            logger.warning("# ROLLBACK!");
            logger.warning("Could not update \"Moodle Item\"-Topic: " 
                + ex.getMessage() + "(" + ex.getClass() + ")");
        } finally {
            tx.finish();
        }
        return null;
    }

    private ChildTopicsModel parseGenericsToItemModel(JSONObject object) throws JSONException {
        // 0) parse name, icon and type
        String name = object.getString("name");
        String iconPath = object.getString("modicon");
        String type = object.getString("modname");
        String description = "", href = "";
        // 1) check for an optional description (e.g. "choice", or "label")
        if (object.has("description")) {
            description = object.getString("description");
        }
        // 2) check for a moodle item url so we can link directly to the content (into the moodle installation)
        if (object.has("url")) {
            href = object.getString("url");
        }
        // 3) Initialize the values of each moodle item
        ChildTopicsModel model = new ChildTopicsModel();
        model.put(MOODLE_ITEM_NAME_URI, name);
        model.put(MOODLE_ITEM_ICON_URI, iconPath);
        model.put(MOODLE_ITEM_DESC_URI, description);
        model.put(MOODLE_ITEM_HREF_URI, href); // exists always (if item is not of type "label")
        model.put(MOODLE_ITEM_TYPE_URI, type);
        // ..) with a neutral review-score
        model.put(REVIEW_SCORE_URI, 0);
        return model;
    }

    private ChildTopicsModel parseResourceToItemModel(ChildTopicsModel model, JSONObject resource)
            throws JSONException {
        String resourceType = resource.getString("type");
        String fileUrl = "", fileName = "", author = "", license = "";
        // 1) Fill up either web resource or file-type
        if (resourceType.equals("url")) { // Moodle Resource is an URL
            fileUrl = resource.getString("fileurl");
            // 1.1) parse youtube and replace /watch?v=id with /embed/id in the url
            if (fileUrl.contains("youtube") || fileUrl.contains("youtu.be")) {
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
        // The new model will just be written to topic/db if our custom update-check (see caller) succeeds
        return model;
    }

    private ChildTopicsModel parseTimestampsToItemModel(ChildTopicsModel model, JSONObject item)
            throws JSONException {
        long last_modified = 0, time_created = 0;
        // 1) Check for timestamp "Last Modified"
        if (item.has("timemodified") && !item.isNull("timemodified")) {
            last_modified = item.getLong("timemodified") * 1000; // addding three zeros
        } else { // e.g. contents of type "url" NEVER have any timestamp set, setting it to NOW
            last_modified = new Date().getTime();
        }
        // 2) Check for timestamp "Created"
        if (item.has("timecreated") && !item.isNull("timecreated")) {
            time_created = item.getLong("timecreated")  * 1000; // addding three zeros;
        } else { // e.g. contents of type "url" NEVER have any timestamp set, setting it to NOW
            time_created = new Date().getTime();
        }
        model.put(MOODLE_ITEM_MODIFIED_URI, last_modified);
        model.put(MOODLE_ITEM_CREATED_URI, time_created);
        return model;
    }

    private String createYoutubeEmbedUrl(String fileUrl) {
        String new_url = fileUrl;
        // 0) Skip transformation if url already contains the "/embed/"-part
        if (fileUrl.contains("/embed")) return new_url;
        // 1) Transform URL Scheme: http://www.youtube.com/watch?v=A9b9bxUyK0o
        if (fileUrl.contains("/watch")) {
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
        if (fileUrl.contains("youtu.be/")) {
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
        Topic userTopic = dms.getTopic(USER_NAME_TYPE_URI, new SimpleValue(username)).loadChildTopics();
        accountTopic = userTopic.getRelatedTopic(COMPOSITION_TYPE_URI, CHILD_ROLE_TYPE_URI, PARENT_ROLE_TYPE_URI,
                USER_ACCOUNT_TYPE_URI).loadChildTopics();
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

    private Topic getMoodleEndpointUrl() {
        return dms.getTopic("uri", new SimpleValue(CLIENT_OPTION_ENDPOINT_URL_URI)).loadChildTopics();
    }

    private Topic getJavaKeyStorePath() {
        return dms.getTopic("uri", new SimpleValue(CLIENT_OPTION_JAVA_KEY_STORE_PATH)).loadChildTopics();
    }

    private Topic getJavaKeyStorePass() {
        return dms.getTopic("uri", new SimpleValue(CLIENT_OPTION_JAVA_KEY_STORE_PASS)).loadChildTopics();
    }

    private boolean isHTTPSConfigured() {
        Topic string_opt = dms.getTopic("uri", new SimpleValue(CLIENT_OPTION_USE_HTTPS)).loadChildTopics();
        if (string_opt.getSimpleValue().toString().equals("true") || 
            string_opt.getSimpleValue().toString().equals("True")) return true;
        return false;
    }

    private ResultList<RelatedTopic> getMoodleCoursesByUser(Topic user) {
        ResultList<RelatedTopic> courses = user.getRelatedTopics(MOODLE_PARTICIPANT_EDGE, DEFAULT_ROLE_TYPE_URI,
                DEFAULT_ROLE_TYPE_URI, MOODLE_COURSE_URI, 0);
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
                DEFAULT_ROLE_TYPE_URI, USER_ACCOUNT_TYPE_URI);
        if (userAccount != null && user.getId() == userAccount.getId()) return value = true;
        return value;
    }

    private boolean hasAggregatingCourseParentEdge (Topic child, Topic parent) {
        boolean value = false;
        if (child == null) return true;
        Topic topic = child.getRelatedTopic(AGGREGATION_TYPE_URI, CHILD_ROLE_TYPE_URI,
                PARENT_ROLE_TYPE_URI, MOODLE_COURSE_URI);
        if (topic != null && parent.getId() == topic.getId()) return value = true;
        return value;
    }

    private boolean hasAggregatingSectionParentEdge (Topic child, Topic parent) {
        boolean value = false;
        if (child == null) return true;
        Topic topic = child.getRelatedTopic(AGGREGATION_TYPE_URI, CHILD_ROLE_TYPE_URI,
                PARENT_ROLE_TYPE_URI, MOODLE_SECTION_URI);
        if (topic != null && parent.getId() == topic.getId()) return value = true;
        return value;
    }

    private Topic getMoodleCourseTopic(long courseId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_COURSE_URI_PREFIX + courseId)).loadChildTopics();
    }

    private Topic getMoodleSectionTopic(long sectionId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_SECTION_URI_PREFIX + sectionId)).loadChildTopics();
    }

    private Topic getMoodleItemTopic(long itemId) {
        return dms.getTopic("uri", new SimpleValue(ISIS_ITEM_URI_PREFIX + itemId)).loadChildTopics();
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
            logger.warning("NOT assigning to Moodle Workspace since topic HAS some assigned already ..");
            return;
        }
        Topic moodleWorkspace = dms.getTopic("uri", new SimpleValue(WS_MOODLE_URI));
        dms.createAssociation(new AssociationModel(AGGREGATION_TYPE_URI,
            new TopicRoleModel(topic.getId(), PARENT_ROLE_TYPE_URI),
            new TopicRoleModel(moodleWorkspace.getId(),CHILD_ROLE_TYPE_URI)
        ));
    }

    private boolean hasAnyWorkspace(Topic topic) {
        return topic.getRelatedTopics(AGGREGATION_TYPE_URI, PARENT_ROLE_TYPE_URI, CHILD_ROLE_TYPE_URI,
            "dm4.workspaces.workspace", 0).getSize() > 0;
    }

    /** 
     * This methods assigns the "admin" Username Topic as the "Creator" of all imported moodle items.
     * The dm4-notes-gui introduced (and relies) on a "Creator"- and "Contributor"-Edges to representing 
     * users who created or edited a specific resource. 
     * 
     */
    private Association assignDefaultAuthorship(Topic item) {
        try {
            // 0) Check if eduzen-tagging-notes plugin is available
            dms.getPlugin(PLUGIN_URI_TAGGING_NOTES); // ### clear why 
            // 1) If eduzen-tagging-nodes plugin is available
            Topic authorUsername = dms.getTopic("dm4.accesscontrol.username",
                    new SimpleValue(USERNAME_OF_SETTINGS_ADMINISTRATOR));
            // org.deepamehta.tagging-resources - Constants
            String CREATOR_EDGE_URI = "org.deepamehta.resources.creator_edge";
            // String CONTRIBUTOR_EDGE_URI = "org.deepamehta.resources.contributor_edge";
            if (associationExists(CREATOR_EDGE_URI, item, authorUsername)) return null;
            return dms.createAssociation(new AssociationModel(CREATOR_EDGE_URI,
                    new TopicRoleModel(item.getId(), PARENT_URI),
                    new TopicRoleModel(authorUsername.getId(), CHILD_URI)));
        } catch (RuntimeException e) {
            logger.fine("MoodleWebServiceClient assigns no \"Tagging Notes\" (Plugin) Default Authorship");
            return null;
        }
    }

    private boolean associationExists(String edge_type, Topic item, Topic user) {
        List<Association> results = dms.getAssociations(item.getId(), user.getId(), edge_type);
        return (results.size() > 0);
    }

}
