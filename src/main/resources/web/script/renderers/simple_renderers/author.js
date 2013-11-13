
dm4c.add_simple_renderer('org.deepamehta.moodle.ordinal_renderer', {

    render_info: function (model, $parent) {

    },

    render_form: function (model, $parent) {
        return function () {
            return model.object.value // cannot be changed
        }
    }

})
