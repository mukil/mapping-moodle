dm4c.add_simple_renderer('org.deepamehta.moodle.desc_renderer', {

    render_info: function (model, $parent) {
        if (model.object.value !== "") {
            $parent.append('<div class="field-item description">' + model.object.value + '</div>')
        }
    },

    render_form: function (model, $parent) {

        $parent.append('<div id="' +model.object.value+ '" class="field-item description"></div>')

        return function () {
            return model.object.value // set dummy field after edit
        }
    }

})