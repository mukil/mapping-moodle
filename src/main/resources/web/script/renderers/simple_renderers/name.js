dm4c.add_simple_renderer('org.deepamehta.moodle.name_renderer', {

    render_info: function (model, $parent) {
        if (model.object.value !== "") {
            $parent.append('<span class="name">' + model.object.value + '</span>')
        }
    },

    render_form: function (model, $parent) {

        $parent.append('<div id="' +model.object.value+ '" class="field-item name"></div>')

        return function () {
            return model.object.value // set dummy field after edit
        }
    }

})