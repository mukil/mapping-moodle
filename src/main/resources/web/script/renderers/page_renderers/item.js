
/**
* A page renderer that models a page as a set of fields.
*
* @see PageRenderer interface (script/interfaces/page_renderer.js).
*/

(function() {

    dm4c.add_page_renderer("org.deepamehta.moodle.item_page_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(topic) {

            // var page_model = create_page_model(topic, render_mode)
            // dm4c.fire_event("pre_render_page", topic, page_model)
            var remote_resource = topic.composite['org.deepamehta.moodle.item_url'].value
            //
            if (topic.composite['org.deepamehta.moodle.item_type'].value === "url") {

                // render a moodle webresource-item
                console.log("We should render an web browser / iframe ... to show ")
                render($("<iframe>").attr({src: remote_resource, width: "100%",
                        height: dm4c.canvas.canvas_height, frameborder: 0}))

            } else if (topic.composite['org.deepamehta.moodle.item_type'].value === "file") {

                // fetch metadata
                var media_type = topic.composite['org.deepamehta.moodle.item_media_type'].value
                var token = dm4c.restc.request("GET", "/moodle/get/key", undefined, undefined, undefined, "text")
                // set new src
                remote_resource = remote_resource + "&token=" + token

                if (media_type) {
                    // TODO: let plugins render the file content
                    if (media_type == "text/plain") {
                        render($("<pre>").text(dm4c.restc.get_file(path)))
                        return
                    } else if (js.begins_with(media_type, "image/")) {
                        render($("<img>").attr("src", remote_resource))
                        return
                    } else if (media_type == "application/pdf") {
                        render($("<embed>").attr({src: remote_resource, type: media_type,
                            width: "100%", height: dm4c.page_panel.height}))
                        return
                    } else if (js.begins_with(media_type, "audio/")) {
                        render($("<embed>").attr({src: remote_resource, width: "95%", height: 64, bgcolor: "#ffffff"})
                            .css("margin-top", "2em"))
                        return
                    } else if (js.begins_with(media_type, "video/")) {
                        // Note: default embed element is used
                        // var content = "<video controls=\"\" src=\"" + remote_resource + "\"></video>"
                    } else if (media_type === "text/html") {
                        render($("<iframe>").attr({src: remote_resource, width: "100%",
                            height: dm4c.canvas.canvas_height, frameborder: 0}))
                        return
                    } else {
                        console.info("Note: default embed element is used")
                        // throw "media type \"" + media_type + "\" is not supported"
                    }
                }
                render($("<embed>").attr({src: remote_resource, type: media_type, width: "100%",
                    height: 0.75 * dm4c.page_panel.width, bgcolor: "#ffffff"}))


            }

            function render(content_element) {
                $('#page-content').append(content_element)
            }
            /// render_page_model(page_model, render_mode)
            //
            // dm4c.render.topic_associations(topic.id)
        },

        render_form: function(topic) {
            // fixme: show warning
            console.log("Page Form Renderer we cannot update Moodle Items")
            return function() {
                // dm4c.do_update_topic(dm4c.render.page_model.build_object_model(page_model))
            }
        }
    })

})()