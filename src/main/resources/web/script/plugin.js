
(function ($, dm4c) {

    dm4c.add_plugin('org.deepamehta.moodle-plugin', function () {

        function doIndexMoodle (menu_item, pos_x, pos_y) {
            var username = getUsername()
            if (username == "") return
            var requestUri = '/moodle/synchronize/' + username
            //
            $.ajax({
                type: "GET", url: requestUri,
                dataType: "text", processData: false,
                async: true,
                success: function(data, text_status, jq_xhr) {
                    $('#page-content').html('<div class="field-label moodle-course-update">'
                        + 'Started to index new and upate existing materials of your moodle courses.<br/><br/>'
                        + 'We are doing so in the background.<br/><br/>'
                        + '<img src="/org.deepamehta.moodle-plugin/images/mlogo_60.png">'
                    + '</div>')
                },
                error: function(jq_xhr, text_status, error_thrown) {
                    throw new Error("MoodleServiceClient-Plugin has problems to start synch-thread... ")
                },
                complete: function(jq_xhr, text_status) {
                    var status = text_status
                }
            })

            $('#page-content').html('<div class="field-label moodle-course-update">'
                + 'Synchronizing materials across all your moodle courses ... </div>')
        }

        function isLoggedIn() {
            var requestUri = '/accesscontrol/user'
            //
            var response = false
            $.ajax({
                type: "GET", url: requestUri,
                dataType: "text", processData: true, async: false,
                success: function(data, text_status, jq_xhr) {
                    if (typeof data === "undefined") return false // this seems to match (new) response semantics
                    if (data != "") response = true
                },
                error: function(jq_xhr, text_status, error_thrown) {
                    throw new Error("MoodleServiceClient-Plugin has problems to authenticate you... ")
                    response = false
                }
            })
            return response
        }

        function getUsername() {
            var requestUri = '/accesscontrol/user'
            //
            var response = false
            $.ajax({
                type: "GET", url: requestUri,
                dataType: "text", processData: true, async: false,
                success: function(data, text_status, jq_xhr) {
                    if (typeof data === "undefined") return "" // this seems to match (new) response semantics
                    if (data != "") response = data
                },
                error: function(jq_xhr, text_status, error_thrown) {
                    throw new Error("MoodleServiceClient-Plugin has problems to authenticate you... ")
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
                        {"moodle_key" : input, "user_id" : user_id})
                    if (response == undefined) throw new Error("Something mad happened.")
                }

            })
        }

        dm4c.add_listener('topic_commands', function (topic) {
            var commands = []
            if (topic.type_uri === 'org.deepamehta.moodle.course') {
                commands.push({is_separator: true, context: 'context-menu'})
                commands.push({
                    label: 'Synchronize Moodle',
                    handler: doIndexMoodle,
                    context: ['context-menu', 'detail-panel-show']
                })
            } else if (topic.type_uri === 'dm4.accesscontrol.user_account') {

                var is_logged_in = isLoggedIn() // why is this called twice ..

                if (is_logged_in) {
                    commands.push({is_separator: true, context: 'context-menu'})
                    commands.push({
                        label: 'Synchronize Moodle',
                        handler: doIndexMoodle,
                        context: ['context-menu', 'detail-panel-show']
                    })
                    commands.push({is_separator: true, context: 'context-menu'})
                    commands.push({
                        label: 'Set Moodle key',
                        handler: setMoodleKey,
                        context: ['context-menu', 'detail-panel-show']
                    })
                }

            }
            return commands

        })

        /** var ws = new WebSocket("ws://localhost:8081", "org.deepamehta.moodle-plugin")

        ws.onopen = function(e) {
            console.log("Opening WebSocket connection to " + e.target.url, e)
            ws.send("Hello WebSockets server!")
        }
        ws.onmessage = function(e) {
            var response = JSON.parse(e.data)
            show_notification(response.message, response.topic)
        }
        ws.onclose = function(e) {
            console.log("Closing WebSocket connection to " + e.target.url + " (" + e.reason + ")", e)
        }

        function show_notification (message, topic) {
            // create
            var $message = $('<div class="message">').text(message)
            var $button = $('<div class="button" id="' +topic.id+ '">').text("\"" +topic.value + "\" - Reveal now")
                $button.click(function(e) {
                    dm4c.show_topic(dm4c.fetch_topic(e.target.id, true), "show", {}, true)
                })
            // assemble
            var $notification = $('<div class="moodle-notification">')
                $notification.append($message).append($button)
            var $close = $('<span class="remove">').text("x")
                $close.click(function (e) { $notification.remove() })
                $notification.append($close)
            // append to notification container
            var $container = $('div.notification-container')
            if ($container.length <= 0) {
                $container = $('<div class="notification-container">')
                $('body').append($container)
            }
            $container.prepend($notification)
            // animate
            $notification.fadeIn(2500, function () {
                $(this).delay(5000).fadeOut(2500)
            })
        } **/

        // show_notification("Changed Moodle Item", {value : "Mein Name ist..", id: 1})
        // show_notification("New Moodle Item", {value : "Mein Name ist..", id: 2})

    })

}(jQuery, dm4c))
