import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// Hachage des mots de passe (PBKDF2). Format stocké : pbkdf2$iterations$sel$hache
final class MotDePasse {
    private static final String PREFIXE = "pbkdf2";
    private static final int ITERATIONS = 210_000;
    private static final int TAILLE_SEL = 16;
    private static final int TAILLE_HACHE = 32;

    private MotDePasse() {}

    static boolean estHache(String stocke) {
        return stocke != null && stocke.startsWith(PREFIXE + "$");
    }

    static String hacher(char[] mdp) {
        byte[] sel = new byte[TAILLE_SEL];
        new SecureRandom().nextBytes(sel);
        byte[] hache = deriver(mdp, sel, ITERATIONS, TAILLE_HACHE);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIXE + "$" + ITERATIONS + "$" + b64.encodeToString(sel) + "$" + b64.encodeToString(hache);
    }

    // Accepte aussi les anciens mots de passe enregistrés en clair (voir BaseDonnees.authentifier).
    static boolean verifier(char[] mdp, String stocke) {
        if (stocke == null) return false;
        if (!estHache(stocke)) {
            return MessageDigest.isEqual(new String(mdp).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                         stocke.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        String[] parties = stocke.split("\\$");
        if (parties.length != 4) return false;
        try {
            int iterations = Integer.parseInt(parties[1]);
            byte[] sel = Base64.getDecoder().decode(parties[2]);
            byte[] attendu = Base64.getDecoder().decode(parties[3]);
            return MessageDigest.isEqual(attendu, deriver(mdp, sel, iterations, attendu.length));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] deriver(char[] mdp, byte[] sel, int iterations, int octets) {
        PBEKeySpec spec = new PBEKeySpec(mdp, sel, iterations, octets * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }
}
