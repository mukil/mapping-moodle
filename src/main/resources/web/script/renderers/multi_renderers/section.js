
dm4c.add_multi_renderer('org.deepamehta.moodle.section_multi_renderer', {

    render_info: function (page_models, $parent) {

        page_models.sort(function sort_by_date_title(a, b) {

            var timeA =  a.object.composite['org.deepamehta.moodle.section_ordinal_nr'].value
            var timeB =  b.object.composite['org.deepamehta.moodle.section_ordinal_nr'].value
            //
            if (timeA > timeB) return 1
            if (timeA < timeB) return -1
            //
            return 0 //default return value (no sorting)

        })

        dm4c.get_multi_renderer("dm4.webclient.default_multi_renderer").render_info(page_models, $parent)


        /* var list = $('<div class="moodle-sections">')
        for (var i = 0; i < page_models.length; i++) {
            // var section = page_models[i].object
            dm4c.page_panel.render_page(page_models[i])
        }
            /*
            if (section != undefined) {
                if (section.id != -1) {
                    /** var url = ""
                    if (section.composite.hasOwnProperty('org.deepamehta.moodle.item_href')) {
                        url = section.composite['org.deepamehta.moodle.item_href'].value
                    }
                    var timestamp = ""
                    if (section.composite.hasOwnProperty('org.deepamehta.moodle.item_modified')) {
                        timestamp = "modified at " + section.composite['org.deepamehta.moodle.item_modified'].value + ""
                    }
                    var iconSrc = ""
                    if (section.composite.hasOwnProperty('org.deepamehta.moodle.item_icon')) {
                        iconSrc = section.composite['org.deepamehta.moodle.item_icon'].value
                    }
                    var name = ""
                    if (section.hasOwnProperty('value')) {
                        name = section.value
                    }
                    // give info-item some behaviour
                    $listItem = $('<div id="' +section.id+ '" class="moodle-section">')
                    $listItem.click(function(e) {
                        var topicId = this.id
                        dm4c.do_reveal_related_topic(topicId, "show")
                    })

                    $listItem.append(section.value)
                    list.append($listItem)
                }
            }
        }
        $parent.append('<div class="field-label">Moodle Sections</div>').append(list) **/

    },

    render_form: function (page_models, $parent) {

        // user cannot edit aggregated tweets of a twitter-search within page panel

        return function () {
            var values = []
            // returning (and referencing) all previously aggregated items back in our submit-function
            for (var item in page_models) {
                var topic_id = page_models[item].object.id
                if (topic_id != -1) {
                    values.push(dm4c.REF_PREFIX + topic_id)
                }
            }
            return values
        }
    }
})
