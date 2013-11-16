
/**
 * A page_renderer that renders a moodle item into the PagePanel.
 *
 * @see PageRenderer interface (/de.deepamehta.webclient/script/interfaces/page_renderer.js).
 */

(function() {

    dm4c.add_page_renderer("org.deepamehta.moodle.item_page_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(topic) {

            //
            GET("/moodle/key", function(status, response) {

                var token = response

                if (status === 401) {

                    render('<div class="field-label">Issue with Moodle Connection</div>')
                    render('<div class="field-item">You need to be logged in to access "Moodle Items".</div>')
                    return function () {}

                } else if (status === 204) {

                    render('<div class="field-label">Issue with Moodle Connection</div>')
                    render('<div class="field-item">We could not find a "Security Key" related '
                        + 'to your "User Account". </div>')
                    render_security_key_form()
                    return function () {}

                } else if (status === 200) {

                    // Render Moodle Item Page ..

                    if (topic.composite['org.deepamehta.moodle.item_type'].value === "file" ||
                        topic.composite['org.deepamehta.moodle.item_type'].value === "url") {

                        render($('<div class="field-label">Moodle Item</div>'))

                        var remote_resource = topic.composite['org.deepamehta.moodle.item_url'].value

                        // .. with a moodle webresource-item
                        if (topic.composite['org.deepamehta.moodle.item_type'].value === "url") {

                            render($("<iframe>").attr({src: remote_resource, width: "99%",
                                    height: dm4c.page_panel.height, frameborder: 0}))

                        // .. with a moodle file-item
                        } else if (topic.composite['org.deepamehta.moodle.item_type'].value === "file") {

                            var media_type = topic.composite['org.deepamehta.moodle.item_media_type'].value
                                remote_resource = remote_resource + "&token=" + token

                            if (media_type) {
                                if (media_type == "text/plain") {
                                    render($("<pre>").text(dm4c.restc.get_file(path)))
                                    return
                                } else if (js.begins_with(media_type, "image/")) {
                                    render($("<img>").attr("src", remote_resource))
                                    return
                                } else if (media_type == "application/pdf") {
                                    render($("<embed>").attr({src: remote_resource, type: media_type,
                                        width: "99%", height: 0.9 * dm4c.page_panel.height}))
                                    return
                                } else if (js.begins_with(media_type, "audio/")) {
                                    render($("<embed>").attr({src: remote_resource, width: "95%", height: 64, bgcolor: "#ffffff"})
                                        .css("margin-top", "2em"))
                                    return
                                } else if (js.begins_with(media_type, "video/")) {
                                    // Note: default embed element is used
                                    // var content = "<video controls=\"\" src=\"" + remote_resource + "\"></video>"
                                } else if (media_type === "text/html") {
                                    // The following triggers a direct file-download in the browser ..
                                    /** render($("<iframe>").attr({src: remote_resource, width: "100%",
                                        height: dm4c.page_panel.height, frameborder: 0})) **/
                                    render($('<div class="field-label">Access source</div>'))
                                    render(dm4c.ui.button(function(e) {
                                        render($("<iframe>").attr({src: remote_resource, width: "99%",
                                        height: dm4c.page_panel.height, frameborder: 0}))
                                    }, "Download HTML Page"))
                                    return
                                } else {
                                    console.info("Note: default embed element is used")
                                    // throw "media type \"" + media_type + "\" is not supported"
                                }
                            }
                            render($("<embed>").attr({src: remote_resource, type: media_type, width: "99%",
                                height: dm4c.page_panel.width, bgcolor: "#ffffff"}))

                        }

                    // .. with a unsupported type of "Moodle item"
                    } else {

                        var type = topic.composite['org.deepamehta.moodle.item_type'].value
                        var href = topic.composite['org.deepamehta.moodle.item_href'].value
                        render($('<div class="field-label">Moodle Item (' + type + ')</div>'))
                        render($('<div class="field-item">' + topic.value + '</div>'))
                        // The following triggers a "Load denied by X-Frame-Options: ../moodle/mod/forum/view.php?id=15
                        // does not permit cross-origin framing."-Error
                        /** render(dm4c.ui.button(function(e) {
                            render($("<iframe>").attr({src: href, width: "100%",
                                height: dm4c.page_panel.height, frameborder: 0}))
                        }, "View source")) **/
                        //
                        render($('<div class="field-label">Access source</div>'))
                        render($('<div class="field-item"><a class="go-to-source" target="_blank" '
                            + 'href="'+href+'">Open in new window</a></div>'))
                        // We cannot access this moodle items, should instead render their metadata and insert a button
                        // allowing to "View in moodle"
                        return
                    }
                }
            }) // End of token GET request handler

            //

            function render(content_element) {
                $('#page-content').append(content_element)
            }

        },

        render_form: function(topic) {

            console.warn("Page Form Renderer we cannot update (remotely stored) Moodle Items")
            // TOOD: Allow tagging
            return function() {
                // dm4c.do_update_topic(dm4c.render.page_model.build_object_model(page_model))
                topic
            }
        }
    })

    function GET (uri, callback) {
        //
        $.ajax({
            type: "GET",
            url: uri,
            processData: false,
            async: true,
            success: function(data, text_status, jq_xhr) {
                callback(jq_xhr.status, data)
            },
            error: function(jq_xhr, text_status, error_thrown) {
                callback(jq_xhr.status, error_thrown)
            }
        })
    }

})()