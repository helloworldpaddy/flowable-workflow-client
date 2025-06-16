import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class generate_jow_password {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password123";
        String hash = encoder.encode(password);
        System.out.println("Password hash for '" + password + "': " + hash);
    }
}