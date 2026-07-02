package ui;

/**
 * Plain entry point that launches {@link MainApp}.
 *
 * <p>When the class with {@code main} extends {@code javafx.application.Application} and
 * JavaFX is on the classpath (not the module path), the JDK launcher fails with
 * "JavaFX runtime components are missing". Launching from a class that does <em>not</em>
 * extend {@code Application} avoids that, so the app runs straight from the IDE's Run
 * button without extra VM arguments.
 */
public class Launcher {

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
