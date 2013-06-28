dm4c.add_simple_renderer('org.deepamehta.moodle.href_renderer', {

    render_info: function (model, $parent) {
        $parent.append('<br/><a id="' + model.object.id + '" href="' + model.object.value + '" target="_blank">'
            + model.object.value + '</a>')
    },

    render_form: function (model, $parent) {
        var pluginResults = {};
        $parent.append('<div id="' +model.object.value+ '" class="field-item href"></div>')

        return function () {
            return model.object.value // set dummy field after edit
        }
    }

})