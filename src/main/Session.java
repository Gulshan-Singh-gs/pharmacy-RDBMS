package main;

/**
 * Session - holds the identity of the currently authenticated user.
 * Populated by Login on success and cleared on logout.
 *
 * @author Gulshan Singh
 */
public final class Session {

    private static int userId = -1;
    private static String userName;

    private Session() {
        // utility class
    }

    /** Records a successful login. */
    public static void login(int id, String name) {
        userId = id;
        userName = name;
    }

    /** Clears the current session. */
    public static void logout() {
        userId = -1;
        userName = null;
    }

    /** Returns the logged-in user's database id, or -1 if not logged in. */
    public static int userId() {
        return userId;
    }

    /** Returns the logged-in user's name, or null if not logged in. */
    public static String userName() {
        return userName;
    }

    /** True if a user is currently logged in. */
    public static boolean isLoggedIn() {
        return userId > 0;
    }
}
