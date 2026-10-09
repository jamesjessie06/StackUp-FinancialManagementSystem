package backend;

import java.io.Serializable;
/** ENCAPSULATION: the password is never stored, only a salted hash that stays package-private. */
public final class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username, role, salt, hash;

    public Account(String username, String role, String salt, String hash) {
        this.username = username;
        this.role = role;
        this.salt = salt;
        this.hash = hash;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
    String getSalt() {
        return salt;
    }
    String getHash() {
        return hash;
    }
}
