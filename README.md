
# Mapping Moodle

A webservice client based on [DeepaMehta 4](https://www.deepamehta.de/) enabling users to *map* [Moodle](http://download.moodle.org/) courses in collaboration.

Where with *mapping* I mean *free placement* of items on an infinite canvas to create structured visualizations representing *personal views* on "Materials" and "Activities" of many "Courses". The DeepaMehta user interface facilitates research activities such as *active reading* as well as *communication* and *collaboration* with fellow learners.

Additionally DeepaMehta allows users to express new relations between moodle items and thus the creation of *visible* and *navigatable* paths for fellow users. Furthermore it allows users to read materials (such as PDFs) on demand at the right side of the screen while maintaining a visual working context on the left side of the screen.


# Usage Requirements

A Moodle installation (at least Version 2.4 or higher) is needed with an active "Web Services"-Plugin and the following service definition set up:

The "External Service"-Configuration needs to allow requests to the following 3 functions: 

* 'core_enrol_get_users_courses'
* 'core_course_get_contents'
* 'core_webservice_get_site_info'. 

The "External service" must also have the option "Can download files" checked, the option "Required capability" set to "No required capability". The data-interchange format must be set to "JSON".

Additonally users must have the capability to "Use REST protocol" and "Create a web service token" to access their "Security Key" under "My profile settings" -> "Security Keys" and pass this on to their DeepaMehta "User Account".


## GNU Public License

This software is released under the terms of the GNU General Public License in Version 3.0, 2007.

## Icons

Moodle "Item" and "Section" icon are both under [Creative Commons - Attribution 3.0 United States](http://creativecommons.org/licenses/by/3.0/us/) designed by [FatCow Web Hosting](http://www.fatcow.com/).

## Changelog

1.2, Apr 4, 2014

- Introduced new options to configure the Moodle HTTP Client via GUI
- Do not reveal intermediary section topic anymore
- Allowing communications to HTTPS
- Compatible with DeepaMehta 4.2
- Background-Synchronization Thread
- Synchronization builds on the new postLogin()-DM4 Hook
- Improved Moodle Item update detection

Known Issue:
- JKS-Passsword and Username are stored insecure
- DeepaMehta 4 Tags and org.deepamehta-Reviews plugin are requirements

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

Know Issues:
- Materials & Activities which are "hidden" in Moodle appear in DeepaMehta
- Updating items is not yet implemented


1.0-SNAPSHOT, 28 Jun 2013

- A basically functional proof-of-system. 
  (see commit message for set-up instructions)

