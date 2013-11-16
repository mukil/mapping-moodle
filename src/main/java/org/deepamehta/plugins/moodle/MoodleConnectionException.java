
package org.deepamehta.plugins.moodle;

public class MoodleConnectionException extends Throwable {

    String message;
    int status;

    public MoodleConnectionException(String message, int status) {
        this.message = message;
        this.status = status;
    }

}
