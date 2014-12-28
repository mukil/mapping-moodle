
/**
 * A page_renderer that renders a moodle item into the PagePanel.
 *
 * @see PageRenderer interface (/de.deepamehta.webclient/script/interfaces/page_renderer.js).
 */

(function() {

    TAG_URI = "dm4.tags.tag"

    dm4c.add_page_renderer("org.deepamehta.moodle.item_page_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(topic) {

            //
            GET("/moodle/key", function(status, response) {

                var token = response

                if (status === 401) {

                    render('<div class="field-label">Issue with Moodle Connection</div>')
                    render('<div class="field-item"><img src="/org.deepamehta.moodle-plugin/images/mlogo_60.png"><br/>'
                        + ' You need to be logged in to have full access to <i>Moodle Items</i>.</div>')
                    return function () {}

                } else if (status === 204) {

                    render('<div class="field-label">Issue with Moodle Connection</div>')
                    render('<div class="field-item"><img src="/org.deepamehta.moodle-plugin/images/mlogo_60.png"><br/>'
                        + 'We could not find a <i>Security Key</i> related to your DeepaMehta <i>User Account</i>.</div>')
                    return function () {}

                } else if (status === 200) {

                    // Render Moodle Item Page ..
                    empty_page() // workaround when deep links are used cause render_info is then called twice

                    if (topic.childs['org.deepamehta.moodle.item_type'].value === "file" ||
                        topic.childs['org.deepamehta.moodle.item_type'].value === "url") {

                        render_item_tags(topic)
                        render($('<div class="field-label moodle-item">Moodle Item</div>'))

                        var remote_resource = topic.childs['org.deepamehta.moodle.item_url'].value

                        // .. with a moodle webresource-item
                        if (topic.childs['org.deepamehta.moodle.item_type'].value === "url") {

                            render($("<iframe>").attr({src: remote_resource, width: "99%",
                                    height: dm4c.page_panel.height, frameborder: 0}))

                        // .. with a moodle file-item
                        } else if (topic.childs['org.deepamehta.moodle.item_type'].value === "file") {

                            var media_type = topic.childs['org.deepamehta.moodle.item_media_type'].value
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
                                        width: "99%", height: 0.86 * dm4c.page_panel.height}))
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

                        var type = topic.childs['org.deepamehta.moodle.item_type'].value
                        var href = topic.childs['org.deepamehta.moodle.item_href'].value
                        var description = ""
                        if (topic.childs.hasOwnProperty('org.deepamehta.moodle.item_description')) {
                            description = topic.childs['org.deepamehta.moodle.item_description'].value
                        }
                        render_item_tags(topic)
                        render($('<div class="field-label">Moodle Item (' + type + ')</div>'))
                        render($('<div class="field-item">' + topic.value + '</div>'))
                        render($('<div class="field-item">' + description + '</div>'))
                        // The following triggers a "Load denied by X-Frame-Options: ../moodle/mod/forum/view.php?id=15
                        // does not permit cross-origin framing."-Error
                        /** render(dm4c.ui.button(function(e) {
                            render($("<iframe>").attr({src: href, width: "100%",
                                height: dm4c.page_panel.height, frameborder: 0}))
                        }, "View source")) **/
                        //
                        if (type !== "label") {
                            render($('<div class="field-label">Access source</div>'))
                            render($('<div class="field-item">'
                                + '<a class="go-to-source" target="_blank" href="' + href + '">Open in new window</a>'
                                + '</div>'))
                        }
                        return
                    }

                }
            }) // End of token GET request handler

            //

            function render(content_element) {
                $('#page-content').append(content_element)
            }

            function render_item_tags(topic) {

                if (typeof topic.childs['dm4.tags.tag'] !== "undefined") {
                    //
                    render($('<div class="field-label">Tags</div>'))
                    if (topic.childs['dm4.tags.tag'].length > 0) {
                        for (var tag_index in topic.childs['dm4.tags.tag']) {
                            var tag = topic.childs['dm4.tags.tag'][tag_index]
                            var $tag_view = $('<div class="tag-item-view" id="'+tag.id+'" title="Show tag: '+tag.value+'">'
                                + '<img alt="Tag icon" src="/de.deepamehta.tags/images/tag_32.png" width="20">'
                                + '<span class="tag-name">' + tag.value + '</span>')
                                $tag_view.click(function(e) {
                                    var tagId = this.id
                                    dm4c.do_reveal_related_topic(tagId, "show")
                                })
                            render($tag_view)
                        }
                    }
                }

            }

            function empty_page() {
                $('#page-content').empty()
            }

        },

        render_form: function(topic) {

            var $parent = $('#page-content')

            // note: The following code is a page_renderer adaptation of the `dm4-tags` multi_renderer implementation

            var existingTags = topic.childs['dm4.tags.tag']
            var allAvailableTags = getAllAvailableTags()
            var inputValue = ""
            var commaCount = 1
            for (var existingTag in existingTags) {
                var element = existingTags[existingTag]
                inputValue += element.value
                if (commaCount < existingTags.length) inputValue += ", "
                commaCount++
            }

            $parent.append('<div class="field-label">Tag this moodle item (comma separated)</div>').append(
                '<input type="text" class="tags" value="' +inputValue+ '"></input>')

            setupTagFieldControls('input.tags')

            return function () {

                var tags = []
                var enteredTags = getTagsSubmitted("input.tags")
                var tagsToReference = []

                // 0) create new and collect existing tags
                for (var label in enteredTags) {
                    var name = enteredTags[label]
                    var tag = getLabelContained(name, allAvailableTags)
                    if (typeof tag === "undefined") {
                        var newTag = dm4c.create_topic(TAG_URI, {"dm4.tags.label": name, "dm4.tags.definition" : ""})
                        tagsToReference.push(newTag)
                    } else {
                        tagsToReference.push(tag)
                    }
                }

                // 1) identify all tags to be deleted
                for (var existingTag in existingTags) {
                    var element = existingTags[existingTag].value // this differs here from multi_renderer (no .object)
                    var elementId = existingTags[existingTag].id // this differs here from multi_renderer (no .object)
                    if (typeof getLabelContained(element, tagsToReference) === "undefined") {
                        tags.push( dm4c.DEL_PREFIX + elementId ) // not tags.push({"id" : dm4c.DEL_PREFIX + elementId})
                    }
                }

                // 2) returning reference all new and existing tags
                for (var item in tagsToReference) {
                    var topic_id = tagsToReference[item].id
                    if (topic_id !== -1) {
                        tags.push( dm4c.REF_PREFIX + topic_id ) // not tags.push({"id" : dm4c.REF_PREFIX + topic_id})
                    }
                }

                // 3) assemble new topic model
                topic.childs['dm4.tags.tag'] = tags
                // where this array contents simply look like this (no json-objects to be constructed)
                // [ "del_id:40190", "ref_id:51291", "ref_id:51131", "ref_id:11318"]
                // 4) this call is needed when implementing page_renderers
                dm4c.do_update_topic(topic)

            }

            function setupTagFieldControls (identifier) {

                $(identifier).bind("keydown", function( event ) {
                    if ( event.keyCode === $.ui.keyCode.TAB && $( this ).data( "ui-autocomplete" ).menu.active ) {
                        event.preventDefault();
                    } else if (event.keyCode === $.ui.keyCode.ENTER) {
                        // fixme: event.preventDefault();
                        event.stopPropagation()
                    }
                }).autocomplete({minLength: 0,
                    source: function( request, response ) {
                        // delegate back to autocomplete, but extract the last term
                        response( $.ui.autocomplete.filter( allAvailableTags, extractLast( request.term ) ) );
                    },
                    focus: function() {
                        // prevent value inserted on focus
                        return false;
                    },
                    select: function( event, ui ) {
                        var terms = split( this.value );
                        // remove the current input
                        terms.pop();
                        // add the selected item
                        terms.push( ui.item.value );
                        // add placeholder to get the comma-and-space at the end
                        terms.push( "" );
                        this.value = terms.join( ", " );
                        return false;
                    }
                });

                function split( val ) {return val.split( /,\s*/ );}

                function extractLast( term ) {return split( term ).pop();}

            }

            function getAllAvailableTags() {
                return dm4c.restc.get_topics(TAG_URI, false, false, 0).items
            }

            function getLabelContained(label, listOfTagTopics) {
                for (var item in listOfTagTopics) {
                    var tag = listOfTagTopics[item]
                    if (tag.value.toLowerCase() === label.toLowerCase()) return tag
                }
                return undefined
            }

            function getTagsSubmitted (fieldIdentifier) {
                if (typeof $(fieldIdentifier).val() === "undefined") return undefined
                var tagline = $(fieldIdentifier).val().split( /,\s*/ )
                if (typeof tagline === "undefined") throw new Error("Tagging field got somehow broken.. ")
                var qualifiedTags = []
                for (var i=0; i < tagline.length; i++) {
                    var tag = tagline[i]
                    // credits for the regexp go to user Bracketworks in:
                    // http://stackoverflow.com/questions/154059/how-do-you-check-for-an-empty-string-in-javascript#154068
                    if (tag.match(/\S/) != null) { // remove empty strings
                        // remove possibly entered duplicates from submitted tags
                        var qualified = true
                        for (var k=0; k < qualifiedTags.length; k++) {
                            var validatedTag = qualifiedTags[k]
                            if (validatedTag.toLowerCase() === tag.toLowerCase()) qualified = false
                        }
                        if (qualified) qualifiedTags.push(tag)
                    }
                }
                return qualifiedTags
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