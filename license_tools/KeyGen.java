package license_tools;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public class KeyGen {
    public static void main(String[] args) {
        try {
            File dir = new File("license_tools");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File privFile = new File(dir, "private_key.pem");
            File pubFile = new File(dir, "public_key.txt");

            if (privFile.exists() && pubFile.exists()) {
                System.out.println("Keys already exist in license_tools/");
                return;
            }

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
            kpg.initialize(ecSpec, new SecureRandom());
            KeyPair kp = kpg.generateKeyPair();

            byte[] privDer = kp.getPrivate().getEncoded();
            byte[] pubDer = kp.getPublic().getEncoded();

            String privBase64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(privDer);
            String privPem = "-----BEGIN PRIVATE KEY-----\n" + privBase64 + "\n-----END PRIVATE KEY-----\n";

            String pubBase64 = Base64.getEncoder().encodeToString(pubDer);

            try (FileOutputStream fos = new FileOutputStream(privFile)) {
                fos.write(privPem.getBytes(StandardCharsets.UTF_8));
            }

            try (FileOutputStream fos = new FileOutputStream(pubFile)) {
                fos.write(pubBase64.getBytes(StandardCharsets.UTF_8));
            }

            System.out.println("Generated keys successfully:");
            System.out.println("Private Key: " + privFile.getAbsolutePath());
            System.out.println("Public Key: " + pubFile.getAbsolutePath());
            System.out.println("Public Key (Base64 X.509): " + pubBase64);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
