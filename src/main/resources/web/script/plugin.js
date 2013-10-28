/*global jQuery, dm4c*/
(function ($, dm4c) {

    dm4c.add_plugin('org.deepamehta.moodle-plugin', function () {

        function getCourseContents () {
            var requestUri = '/moodle/course/' + dm4c.selected_object.id + '/content'

            var response_data_type = response_data_type || "json"
            //
            $.ajax({
                type: "GET", url: requestUri,
                dataType: response_data_type, processData: false,
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

        function getUserId () {
            var requestUri = '/moodle/user'

            var response_data_type = response_data_type || "json"
            //
            $.ajax({
                type: "GET", url: requestUri,
                dataType: response_data_type, processData: false,
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
                + 'Shaking hands with moodle ... </div>')
        }

        function getMoodleFile () {
            var requestUri = '/moodle/file/' + dm4c.selected_object.id

            var response_data_type = response_data_type || "json"
            //
            $.ajax({
                type: "GET", url: requestUri,
                dataType: response_data_type, processData: false,
                async: true,
                success: function(data, text_status, jq_xhr) {
                    // dm4c.do_select_topic(data.id, true)
                    console.log(data)
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
                + 'Reading file ...</div>')
        }

        function getMyCourses () {
            var requestUri = '/moodle/courses'

            var response_data_type = response_data_type || "json"
            //
            $.ajax({
                type: "GET", url: requestUri,
                dataType: response_data_type, processData: false,
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
                commands.push({is_separator: true, context: 'context-menu'})
                commands.push({
                    label: 'Shake hands with moodle',
                    handler: getUserId,
                    context: ['context-menu', 'detail-panel-show']
                })
                commands.push({
                    label: 'Ask moodle for my courses',
                    handler: getMyCourses,
                    context: ['context-menu', 'detail-panel-show']
                })
            } else if (topic.type_uri === 'org.deepamehta.moodle.item') {
                if (topic.composite.hasOwnProperty('org.deepamehta.moodle.item_type')) {
                    var type_of = topic.composite['org.deepamehta.moodle.item_type'].value
                    if (type_of === "file") {
                        commands.push({is_separator: true, context: 'context-menu'})
                        commands.push({
                            label: 'Read on',
                            handler: getMoodleFile,
                            context: ['context-menu', 'detail-panel-show']
                        })
                    }
                }
            }
            return commands
        })

    })

}(jQuery, dm4c))
