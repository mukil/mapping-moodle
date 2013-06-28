dm4c.add_simple_renderer('org.deepamehta.moodle.token_renderer', {

    render_info: function (model, $parent) {
        $parent.append('<div id="' +model.object.value+ '" class="field-item token"></div>')
    },

    render_form: function (model, $parent) {

        $parent.append('<div id="' +model.object.value+ '" class="field-item token"></div>')

        return function () {
            return model.object.value // set dummy field after edit
        }
    }

})