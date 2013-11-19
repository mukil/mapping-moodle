
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

        // TODO: replace this default renderer since otherwise it is to cumbersome to realize: when a users clicks on a
        // moodle item, the items' (parent) section shall also be revealed on the canvas. see multi-item.js-renderer
        dm4c.get_multi_renderer("dm4.webclient.default_multi_renderer").render_info(page_models, $parent)

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
