
dm4c.add_multi_renderer('org.deepamehta.moodle.item_multi_renderer', {

    render_info: function (page_models, $parent) {

        // sort_page_models()

        var list = $('<ul class="moodle-items">')
        for (var i = 0; i < page_models.length; i++) {
            //
            // var section = page_models[i].parent.object
            var item = page_models[i].object
            if (typeof item !== "undefined") {
                if (item.id !== -1) {
                    /** var url = ""
                    if (item.childs.hasOwnProperty('org.deepamehta.moodle.item_href')) {
                        url = item.childs['org.deepamehta.moodle.item_href'].value
                    } **/
                    var iconSrc = ""
                    if (item.childs.hasOwnProperty('org.deepamehta.moodle.item_icon')) {
                        iconSrc = item.childs['org.deepamehta.moodle.item_icon'].value
                    }
                    var name = ""
                    if (item.hasOwnProperty('value')) {
                        name = item.value
                    }
                    // give info-item some behaviour
                    var $listItem = $('<div id="' +item.id+ '">')
                        $listItem.click(function(e) {
                            var topicId = this.id
                            // dm4c.do_reveal_related_topic(section.id, "show")
                            dm4c.do_reveal_related_topic(topicId, "show")
                        })
                        $listItem.append('<img src="' + iconSrc + '">' +name)
                    // + ' <small>' +new Date(timestamp).toGMTString()+ '</small>'
                    list.append($('<li class="moodle-item">').html($listItem))
                }
            }
        }
        $parent.append('<div class="field-label">Moodle Items</div>').append(list)

    },

    render_form: function (page_models, $parent) {

        // user cannot edit aggregated tweets of a twitter-search within page panel

        return function () {
            var values = []
            // returning (and referencing) all previously aggregated items back in our submit-function
            for (var item in page_models) {
                var topic_id = page_models[item].object.id
                if (topic_id !== -1) {
                    values.push(dm4c.REF_PREFIX + topic_id)
                }
            }
            return values
        }
    }
})
