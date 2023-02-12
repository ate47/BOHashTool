package fr.atesab.bo4hash;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Main {
    public static Properties readLastLoad() {
        Properties prop = new Properties();
        try (BufferedReader r = Files.newBufferedReader(Path.of("bohash.cfg"))) {
            prop.load(r);
            return prop;
        } catch (IOException e) {
            return new Properties();
        }
    }

    public static void saveLoad(Properties prop) {
        try (BufferedWriter r = Files.newBufferedWriter(Path.of("bohash.cfg"))) {
            prop.store(r, "bohash tool cfg");
        } catch (IOException ignore) {
        }
    }

    public static final String CFG_PATH = "cfg.path";

    public static void main(String[] args) throws IOException {
        BufferedImage res = ImageIO.read(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("logo.png")));
        System.out.println(Files.readString(Path.of("H:\\Vuze Downloads\\GSC\\t8-src\\scripts\\zm_common\\zm_zonemgr.gsc")));

        JFrame frame = new JFrame("COD Hash tool");
        frame.setIconImage(res);
        JPanel panel = new JPanel(null);
        Searcher searcher = new Searcher();
        AtomicReference<String> loadText = new AtomicReference<>("Waiting for loading");
        Properties prop = readLastLoad();

        final int width = 800;
        final int height = 480;

        JTextField path = new JTextField(Objects.requireNonNullElse(prop.getProperty(CFG_PATH), ""));
        JTextField text = new JTextField();
        JTextField hashString = new JTextField();
        JTextField hashObject = new JTextField();
        JLabel pathLabel = new JLabel("path: ");
        JLabel textLabel = new JLabel("unhashed: ");
        JLabel hashStringLabel = new JLabel("hash string: ");
        JLabel hashObjectLabel = new JLabel("hash object: ");
        JTextArea notificationField = new JTextArea();
        JButton loadPath = new JButton("Load");
        JButton copyText = new JButton("Copy");
        JButton copyHashString = new JButton("Copy");
        JButton copyHashObject = new JButton("Copy");
        JButton copyNotification = new JButton("Copy");

        path.setBounds(width / 2 - 300, 44, 580, 20);
        text.setBounds(width / 2 - 300, 44 + 30, 580, 20);
        hashString.setBounds(width / 2 - 300, 44 + 30 * 2, 580, 20);
        hashObject.setBounds(width / 2 - 300, 44 + 30 * 3, 580, 20);
        notificationField.setBounds(20, 44 + 30 * 4, width - 40, height - 80 - (44 + 30 * 4));
        copyNotification.setBounds(20, height - 70, width - 40, 20);

        pathLabel.setLabelFor(path);
        pathLabel.setHorizontalAlignment(JLabel.RIGHT);
        pathLabel.setVerticalAlignment(JLabel.CENTER);

        textLabel.setLabelFor(text);
        textLabel.setHorizontalAlignment(JLabel.RIGHT);
        textLabel.setVerticalAlignment(JLabel.CENTER);

        hashStringLabel.setLabelFor(hashString);
        hashStringLabel.setHorizontalAlignment(JLabel.RIGHT);
        hashStringLabel.setVerticalAlignment(JLabel.CENTER);

        hashObjectLabel.setLabelFor(hashObject);
        hashObjectLabel.setHorizontalAlignment(JLabel.RIGHT);
        hashObjectLabel.setVerticalAlignment(JLabel.CENTER);

        notificationField.setEditable(false);
        notificationField.setAlignmentX(JTextArea.CENTER_ALIGNMENT);
        notificationField.setBorder(new LineBorder(Color.LIGHT_GRAY));
        notificationField.setBackground(Color.WHITE);

        path.setBorder(new LineBorder(Color.DARK_GRAY));
        path.setBackground(Color.WHITE);
        text.setBorder(new LineBorder(Color.DARK_GRAY));
        text.setBackground(Color.WHITE);
        hashString.setBorder(new LineBorder(Color.DARK_GRAY));
        hashString.setBackground(Color.WHITE);
        hashString.setEditable(false);
        hashObject.setBorder(new LineBorder(Color.DARK_GRAY));
        hashObject.setBackground(Color.WHITE);
        hashObject.setEditable(false);

        pathLabel.setBounds(path.getX() - 100, path.getY(), 100, path.getHeight());
        textLabel.setBounds(text.getX() - 100, text.getY(), 100, text.getHeight());
        hashStringLabel.setBounds(hashString.getX() - 100, hashString.getY(), 100, hashString.getHeight());
        hashObjectLabel.setBounds(hashObject.getX() - 100, hashObject.getY(), 100, hashObject.getHeight());

        loadPath.setBounds(path.getX() + path.getWidth() + 10, path.getY(), 80, path.getHeight());
        copyText.setBounds(text.getX() + text.getWidth() + 10, text.getY(), 80, text.getHeight());
        copyHashString.setBounds(hashString.getX() + hashString.getWidth() + 10, hashString.getY(), 80, hashString.getHeight());
        copyHashObject.setBounds(hashObject.getX() + hashObject.getWidth() + 10, hashObject.getY(), 80, hashObject.getHeight());

        loadPath.setBackground(Color.WHITE);
        loadPath.setForeground(Color.BLACK);
        loadPath.setBorder(new LineBorder(Color.LIGHT_GRAY));

        copyText.setBackground(Color.WHITE);
        copyText.setForeground(Color.BLACK);
        copyText.setBorder(new LineBorder(Color.LIGHT_GRAY));

        copyHashString.setBackground(Color.WHITE);
        copyHashString.setForeground(Color.BLACK);
        copyHashString.setBorder(new LineBorder(Color.LIGHT_GRAY));

        copyHashObject.setBackground(Color.WHITE);
        copyHashObject.setForeground(Color.BLACK);
        copyHashObject.setBorder(new LineBorder(Color.LIGHT_GRAY));

        copyNotification.setBackground(Color.WHITE);
        copyNotification.setForeground(Color.BLACK);
        copyNotification.setBorder(new LineBorder(Color.LIGHT_GRAY));


        text.getDocument().addDocumentListener(new DocumentListener() {
            private void loadText() {
                String textContent = text.getText();
                String hashStringValue = Long.toUnsignedString(Hash.hashRes(textContent), 16).toLowerCase();
                String hashObjectValue = Long.toUnsignedString(Hash.hashComp(textContent), 16).toLowerCase();
                hashString.setText(hashStringValue);
                hashObject.setText(hashObjectValue);
                List<Searcher.Obj> obj = searcher.search(hashStringValue, hashObjectValue);
                if (obj.isEmpty()) {
                    notificationField.setText("no find");
                } else {
                    notificationField.setText(obj.stream().map(o -> textContent + "," + o.element()).collect(Collectors.joining("\n")));
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                loadText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                loadText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                loadText();
            }
        });

        if (!path.getText().isEmpty()) {
            notificationField.setText("loading...");
            notificationField.setText(searcher.load(path.getText()));
        } else {
            notificationField.setText("no dataset loaded");
        }

        loadPath.addActionListener(e -> {
            notificationField.setText("loading...");
            notificationField.setText(searcher.load(path.getText()));
            prop.setProperty(CFG_PATH, path.getText());
            saveLoad(prop);
        });
        copyText.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text.getText()), null));
        copyHashString.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(hashString.getText()), null));
        copyHashObject.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(hashObject.getText()), null));
        copyNotification.addActionListener(l -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(notificationField.getText()), null));

        panel.add(path);
        panel.add(text);
        panel.add(hashString);
        panel.add(hashObject);
        panel.add(pathLabel);
        panel.add(textLabel);
        panel.add(hashStringLabel);
        panel.add(hashObjectLabel);
        panel.add(loadPath);
        panel.add(copyText);
        panel.add(copyHashString);
        panel.add(copyHashObject);
        panel.add(notificationField);
        panel.add(copyNotification);

        panel.setBackground(Color.WHITE);
        frame.setContentPane(panel);
        frame.setSize(width, height);
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
// bf29ce484222325