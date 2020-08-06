package com.intege.mediahand.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class MessageUtil {

    public static void warningAlert(Exception exception) {
        warningAlert(exception, exception.getMessage());
    }

    public static void warningAlert(Exception exception, String message) {
        StringBuilder builder = new StringBuilder();
        if (exception.getCause() != null) {
            builder.append(exception.getCause().getMessage());
            builder.append("\n\n");
        }
        builder.append(message);

        warningAlert(exception.getClass().getTypeName(), builder.toString());
    }

    public static void warningAlert(String header, String message) {
        alert(AlertType.WARNING, "Warning", header, message);
    }

    public static void infoAlert(String header, String message) {
        alert(AlertType.INFORMATION, "Info", header, message);
    }

    public static void alert(AlertType alertType, String title, String header, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
