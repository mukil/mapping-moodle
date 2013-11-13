/*global jQuery, dm4c*/
(function ($, dm4c) {

    dm4c.add_plugin('org.deepamehta.moodle-plugin', function () {

        function getCourseContents () {
            var requestUri = '/moodle/course/' + dm4c.selected_object.id + '/content'
            //
            $.ajax({
                type: "GET", url: requestUri,
                dataType: "json", processData: false,
                async: true,
                success: function(data, text_status, jq_xhr) {
                    dm4c.do_select_topic(data.id, true)
                },
                error: function(jq_xhr, text_status, error_thrown) {
                    dm4c.page_panel.refresh()
                    throw "RESTClientError: GET request failed (" + text_status + ": " + error_thrown + " - Hint: "
                        + " Most probably this user is not a participant of this course, or has not set a security key."
                },
                complete: function(jq_xhr, text_status) {
                    var status = text_status
                }
            })

            $('#page-content').html('<div class="field-label moodle-course-update">'
                + 'Asking Moodle Installation... </div>')
        }

        function getMyCourses () {
            var requestUri = '/moodle/courses'

            //
            $.ajax({
                type: "GET", url: requestUri,
                dataType: "json", processData: false,
                async: true,
                success: function(data, text_status, jq_xhr) {
                    dm4c.do_select_topic(data.id, true)
                },
                error: function(jq_xhr, text_status, error_thrown) {
                    dm4c.page_panel.refresh()
                    throw "RESTClientError: GET request failed (" + text_status + ": " + error_thrown + " - Hint: "
                        + " Most probably this user has not set a security key / token yet."
                },
                complete: function(jq_xhr, text_status) {
                    var status = text_status
                }
            })

            $('#page-content').html('<div class="field-label moodle-course-update">'
                + 'Asking moodle for my courses ... </div>')
        }

        function isLoggedIn() {
            var requestUri = '/accesscontrol/user'
            //
            var response = false
            $.ajax({
                type: "GET", url: requestUri,
                dataType: "text", processData: true, async: false,
                success: function(data, text_status, jq_xhr) {
                    // dm4c.do_select_topic(data.id, true)
                    if (data != "") response = true
                },
                error: function(jq_xhr, text_status, error_thrown) {
                    console.log("Error performing GET request.. ")
                    response = false
                }
            })

            return response
        }

        function setMoodleKey() {
            var title = "Security key for Moodle"
            var input_label = "Your Moodle security key"
            var button_label = "Save"
            dm4c.ui.prompt(title, input_label, button_label, function (input) {

                var user_id = dm4c.selected_object.id
                //
                if (input !== "" && input !== " ") {
                    var response = dm4c.restc.request("POST", "/moodle/key/" + user_id,
                        { "moodle_key" : input, "user_id" : user_id })
                    if (response == undefined) throw new Error("Something mad happened.")
                }

            })
        }

        // some (developer) commands
        dm4c.add_listener('topic_commands', function (topic) {
            /** if (!dm4c.has_create_permission('org.deepamehta.moodle.course')) {
                return
            } **/
            var commands = []
            if (topic.type_uri === 'org.deepamehta.moodle.course') {
                commands.push({is_separator: true, context: 'context-menu'})
                commands.push({
                    label: 'Load course',
                    handler: getCourseContents,
                    context: ['context-menu', 'detail-panel-show']
                })
            } else if (topic.type_uri === 'dm4.accesscontrol.user_account') {

                var is_logged_in = isLoggedIn() // why is this called twice ..

                if (is_logged_in) {
                    commands.push({is_separator: true, context: 'context-menu'})
                    commands.push({
                        label: 'Set Moodle Key',
                        handler: setMoodleKey,
                        context: ['context-menu', 'detail-panel-show']
                    })
                    commands.push({is_separator: true, context: 'context-menu'})
                    commands.push({
                        label: 'Ask moodle for my courses',
                        handler: getMyCourses,
                        context: ['context-menu', 'detail-panel-show']
                    })
                }

            }
            return commands

        })

    })

}(jQuery, dm4c))
