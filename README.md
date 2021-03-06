
# DeepaMehta 4 Mapping Moodle Module

A webservice client based on [DeepaMehta 4](https://www.deepamehta.de/) enabling users to *map* [Moodle](http://download.moodle.org/) courses and their work materials in collaboration. 

This web service client uses the "Users as clients with token" option to authorize requests on behalf of certain users (as described on the "Web servcies" > "Overview" page in your moodle installation.

With *mapping* we mean *free placement* of items on an *infinite canvas* to create *structured visualizations* representing *personal views* on "Resources" and "Activities" of many "Courses". The DeepaMehta user interface facilitates research activities such as *active reading* as well as *communication* and *collaboration* with fellow learners.

Additionally DeepaMehta allows users to express new relations between moodle items and thus the creation of *visible* and *navigatable* paths for fellow users. Furthermore it allows users to read materials (such as PDFs) on demand at the right side of the screen while maintaining a visual working context on the left side of the screen.


# Usage & Setup

A Moodle installation (at least Version 2.4 or higher) is needed with an active "Web Services"-Plugin (see "Advanced Features") and the following service definition set up.

The "External Service"-Configuration needs to allow requests to the following 3 functions: 

* 'core_enrol_get_users_courses'
* 'core_course_get_contents'
* 'core_webservice_get_site_info'. 

The "External service" must also have the option "Can download files" checked, the option "Required capability" set to "No required capability". For Moodle versions 2.4 the data-interchange format must explicitly be set to "JSON".

To be able to connect the Topicmap UI on their behalf users must have the capabilities to 

* "Use REST protocol" and 
* "Create a web service token" 

so they can access their "Security Key" via "My profile settings" -> "Security Keys". This secret key needs to be passed on to their DeepaMehta "User Account".

The best way (I could figure out) to grant these capabilities in Moodle is to create a new role under "Site administration" > "Users" > "Permissions" > "Define roles" > "Add new role". Set role archetype to "None" and context of role to "System". Finally, add the two above mentioned permissions to this role and click "Create this role".

   Note: Just "Moodle Course"-Topics with a given "Tag" are synchronized. 
   That's the current "editorial workflow" to 
   (I) not syncronize all courses on first contact and 
   (II) map all items of each course automatically under a unique label (Tag).

## GNU Public License

This software is released under the terms of the GNU General Public License in Version 3.0, 2007.

## Icons

Moodle "Item" and "Section" icon are both under [Creative Commons - Attribution 3.0 United States](http://creativecommons.org/licenses/by/3.0/us/) designed by [FatCow Web Hosting](http://www.fatcow.com/).

## Ideas & Possibilities

As of Moodle 2.5 forum and discussions and managing notes, contacts and calendar events are available via the Moodle Web service API.

As of Moodle 2.6 the managament of assignments is available via the Moodle Web service API.

(see also [Moodle Web service Roadmap](https://docs.moodle.org/dev/Web_services_Roadmap))

## Changelog

1.2.1, Dec 28, 2014

- Tested against Moodle 2.8
- A moodle course can be synced under many 'tags'
- Compatible with DeepaMehta 4.4

Known Issue:
- JKS-Passsword and Username are stored insecure
- DeepaMehta 4 Tags and org.deepamehta-Reviews plugin are requirements

1.2, Apr 4, 2014

- Introduced new options to configure the Moodle HTTP Client via GUI
- Do not reveal intermediary section topic anymore
- Allowing communications to HTTPS
- Compatible with DeepaMehta 4.2
- Background-Synchronization Thread
- Synchronization builds on the new postLogin()-DM4 Hook
- Improved Moodle Item update detection

1.1.2-SNAPSHOT, Nov 12 2013

Compatible with DeepaMehta 4.1.2

Features:
- Simplified application model
  ("Moodle Items" now map "files", "urls" and "file-urls" (=pages))
- "Moodle Items" come now with a custom "Page Renderer" 
  (allowing users to access/read certain materials directly in DMs Detail Panel)
- Secure storage and retrieval for "Moodle Security Keys" per "User Account"
- Security Key can be set via Webclient (Topic command of "User Account")

Fixes:
- Sorting of "Moodle sections" in DMs "Page panel" is now correct
- Moodle User Id is requested automatically (always when a security key is set)

Known Issues:
- Materials & Activities which are "hidden" in Moodle appear in DeepaMehta
- Updating items is not yet implemented


1.0-SNAPSHOT, 28 Jun 2013

- A basically functional proof-of-system. 
  (see commit message for set-up instructions)

-------------------------------
Author: Malte Reißig, 2013-2014

