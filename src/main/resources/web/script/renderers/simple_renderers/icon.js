dm4c.add_simple_renderer('org.deepamehta.moodle.icon_renderer', {

    render_info: function (model, $parent) {
        if (model.object.value !== "") {
            $parent.append('<img class="icon" src="'+model.object.value+'" alt="Moodle Item Type Icon">')
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