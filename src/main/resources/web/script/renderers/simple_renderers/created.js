dm4c.add_simple_renderer('org.deepamehta.moodle.created_renderer', {

    render_info: function (model, $parent) {
        if (model.object.value !== "" || model.object.value !== 0) {
            $parent.append('<div class="field-item created">' + model.object.value + '</div>')
        }
    },

    render_form: function (model, $parent) {
        var pluginResults = {};
        $parent.append('<div id="' +model.object.value+ '" class="field-item icon"></div>')

        return function () {
            return model.object.value // set dummy field after edit
        }
    }

})