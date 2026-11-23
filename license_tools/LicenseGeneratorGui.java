package license_tools;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

public class LicenseGeneratorGui extends JFrame {

    private final JTextField idField;
    private final JTextField nameField;
    private final JComboBox<String> durationCombo;
    private final JTextField customDaysField;
    private final JSpinner maxWorkspacesSpinner;
    private final JCheckBox excelCheck;
    private final JCheckBox appLockCheck;
    private final JCheckBox bgSyncCheck;
    private final JTextArea outputArea;
    private final JLabel statusLabel;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public LicenseGeneratorGui() {
        super("أداة توليد تراخيص - بحث آمن");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 750);
        setLocationRelativeTo(null);

        // Apply RTL Orientation
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        loadKeys();

        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        rootPanel.setBorder(new EmptyBorder(16, 16, 16, 16));
        rootPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // Header Title
        JLabel titleLabel = new JLabel("توليد كود الترخيص الرقمي (ECDSA P-256)");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rootPanel.add(titleLabel);
        rootPanel.add(Box.createVerticalStrut(14));

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.weightx = 1.0;

        int row = 0;

        // 1. Installation ID
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        JLabel idLabel = new JLabel("معرّف التثبيت (Installation ID): *");
        idLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(idLabel, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        idField = new JTextField();
        idField.setFont(new Font("Monospaced", Font.BOLD, 14));
        idField.setToolTipText("مثال: BA-A2B3-C4D5-E6F7");
        formPanel.add(idField, gbc);

        // 2. Client Name
        gbc.gridx = 0; gbc.gridy = row;
        JLabel nameLabel = new JLabel("اسم العميل / الجهة (اختياري):");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        nameField = new JTextField();
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(nameField, gbc);

        // 3. Validity Duration
        gbc.gridx = 0; gbc.gridy = row;
        JLabel durLabel = new JLabel("مدة صلاحية الترخيص:");
        durLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(durLabel, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        String[] durations = {"دائم (بلا انتهاء)", "7 أيام", "30 يوماً", "90 يوماً", "سنة (365 يوماً)", "عدد أيام مخصص..."};
        durationCombo = new JComboBox<>(durations);
        durationCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(durationCombo, gbc);

        // Custom Days (conditional)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel customDaysLabel = new JLabel("عدد الأيام المخصص:");
        customDaysLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(customDaysLabel, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        customDaysField = new JTextField("60");
        customDaysField.setEnabled(false);
        formPanel.add(customDaysField, gbc);

        durationCombo.addActionListener(e -> {
            boolean isCustom = durationCombo.getSelectedIndex() == 5;
            customDaysField.setEnabled(isCustom);
        });

        // 4. Max Workspaces
        gbc.gridx = 0; gbc.gridy = row;
        JLabel mwLabel = new JLabel("الحد الأقصى للمجلدات (0 = غير محدود):");
        mwLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(mwLabel, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        maxWorkspacesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        maxWorkspacesSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(maxWorkspacesSpinner, gbc);

        // 5. Advanced Permissions
        gbc.gridx = 0; gbc.gridy = row;
        JLabel permLabel = new JLabel("الصلاحيات المشمولة:");
        permLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(permLabel, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel checksPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        checksPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        excelCheck = new JCheckBox("تصدير Excel", true);
        appLockCheck = new JCheckBox("قفل بالبصمة", true);
        bgSyncCheck = new JCheckBox("المزامنة التلقائية", true);
        checksPanel.add(excelCheck);
        checksPanel.add(appLockCheck);
        checksPanel.add(bgSyncCheck);
        formPanel.add(checksPanel, gbc);

        rootPanel.add(formPanel);
        rootPanel.add(Box.createVerticalStrut(12));

        // Action Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        JButton genBtn = new JButton("توليد كود الترخيص");
        genBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        genBtn.setPreferredSize(new Dimension(180, 40));

        JButton copyBtn = new JButton("نسخ الكود");
        copyBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        copyBtn.setPreferredSize(new Dimension(140, 40));

        btnPanel.add(genBtn);
        btnPanel.add(copyBtn);
        rootPanel.add(btnPanel);
        rootPanel.add(Box.createVerticalStrut(10));

        // Status Label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rootPanel.add(statusLabel);
        rootPanel.add(Box.createVerticalStrut(6));

        // Output Area
        outputArea = new JTextArea(8, 50);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        rootPanel.add(scrollPane);

        add(rootPanel);

        // Action Handlers
        genBtn.addActionListener(e -> generateLicense());
        copyBtn.addActionListener(e -> copyLicenseToClipboard());
    }

    private void loadKeys() {
        try {
            File privFile = new File("license_tools/private_key.pem");
            File pubFile = new File("license_tools/public_key.txt");

            if (!privFile.exists() || !pubFile.exists()) {
                KeyGen.main(new String[]{});
            }

            String privPem = new String(Files.readAllBytes(privFile.toPath()), StandardCharsets.UTF_8);
            String privBase64 = privPem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] privBytes = Base64.getDecoder().decode(privBase64);
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privBytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            this.privateKey = kf.generatePrivate(privSpec);

            String pubBase64 = new String(Files.readAllBytes(pubFile.toPath()), StandardCharsets.UTF_8).trim();
            byte[] pubBytes = Base64.getDecoder().decode(pubBase64);
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubBytes);
            this.publicKey = kf.generatePublic(pubSpec);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "خطأ أثناء تحميل مفاتيح التوقيع: " + e.getMessage(),
                    "خطأ فادح", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generateLicense() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("يرجى إدخال معرّف التثبيت (Installation ID).");
            return;
        }

        try {
            String name = nameField.getText().trim();
            long iat = Instant.now().getEpochSecond();
            long exp = 0L;

            int durationIdx = durationCombo.getSelectedIndex();
            switch (durationIdx) {
                case 0: // Permanent
                    exp = 0L;
                    break;
                case 1: // 7 days
                    exp = iat + (7L * 24 * 3600);
                    break;
                case 2: // 30 days
                    exp = iat + (30L * 24 * 3600);
                    break;
                case 3: // 90 days
                    exp = iat + (90L * 24 * 3600);
                    break;
                case 4: // 365 days
                    exp = iat + (365L * 24 * 3600);
                    break;
                case 5: // Custom
                    long days = Long.parseLong(customDaysField.getText().trim());
                    exp = iat + (days * 24 * 3600);
                    break;
            }

            int mw = (Integer) maxWorkspacesSpinner.getValue();
            int ex = excelCheck.isSelected() ? 1 : 0;
            int lk = appLockCheck.isSelected() ? 1 : 0;
            int bg = bgSyncCheck.isSelected() ? 1 : 0;

            StringBuilder json = new StringBuilder("{");
            json.append("\"v\":1,");
            json.append("\"id\":\"").append(id).append("\",");
            if (!name.isEmpty()) {
                json.append("\"n\":\"").append(name.replace("\"", "\\\"")).append("\",");
            }
            json.append("\"iat\":").append(iat).append(",");
            json.append("\"exp\":").append(exp).append(",");
            json.append("\"mw\":").append(mw).append(",");
            json.append("\"ex\":").append(ex).append(",");
            json.append("\"lk\":").append(lk).append(",");
            json.append("\"bg\":").append(bg);
            json.append("}");

            byte[] payloadBytes = json.toString().getBytes(StandardCharsets.UTF_8);

            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(privateKey);
            signer.update(payloadBytes);
            byte[] sigBytes = signer.sign();

            // Self-verify signature
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(payloadBytes);
            if (!verifier.verify(sigBytes)) {
                throw new IllegalStateException("فشل التحقق الذاتي من التوقيع الرقمي!");
            }

            Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
            String payloadB64 = urlEncoder.encodeToString(payloadBytes);
            String sigB64 = urlEncoder.encodeToString(sigBytes);

            String fullLicenseCode = "BA1." + payloadB64 + "." + sigB64;

            outputArea.setText(fullLicenseCode);
            statusLabel.setForeground(new Color(0, 130, 50));
            statusLabel.setText("تم توليد كود الترخيص وتوثيقه بنجاح!");
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("فشل التوليد: " + e.getMessage());
        }
    }

    private void copyLicenseToClipboard() {
        String code = outputArea.getText().trim();
        if (code.isEmpty()) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("لا يوجد كود لنسخه.");
            return;
        }

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
        statusLabel.setForeground(new Color(0, 130, 50));
        statusLabel.setText("تم نسخ كود الترخيص إلى الحافظة بنجاح!");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new LicenseGeneratorGui().setVisible(true);
        });
    }
}
