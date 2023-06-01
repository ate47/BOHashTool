package fr.atesab.bo4hash.ui;

import fr.atesab.bo4hash.DataFetcher;
import fr.atesab.bo4hash.Main;
import fr.atesab.bo4hash.Searcher;
import fr.atesab.bo4hash.utils.ExpandTool;
import fr.atesab.bo4hash.utils.HashUtils;
import fr.atesab.bo4hash.utils.ReplacerTool;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HashSearcherFrame extends JFrame {
    public static final Image ICON;
    public static final Image TAB_SEARCH;
    public static final Image TAB_LARGE_SEARCH;
    public static final Image TAB_REPLACE;
    public static final Image TAB_BRUTE;
    public static final Image TAB_LANG;
    public static final Image TAB_DB;
    public static final Image TAB_CONVERT;
    public static final Image TAB_INFO;
    private static final String version;
    private static final Font FONT;

    private static final int width = 800;
    private static final int height = 480;

    static {
        try {
            BufferedImage atlas = ImageIO.read(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("atlas.png")));
            ICON = atlas.getSubimage(0, 0, 128, 128);
            TAB_SEARCH = atlas.getSubimage(128, 0, 32, 32);
            TAB_LARGE_SEARCH = atlas.getSubimage(128 + 32, 0, 32, 32);
            TAB_LANG = atlas.getSubimage(128 + 32 * 2, 0, 32, 32);
            TAB_REPLACE = atlas.getSubimage(128 + 32 * 3, 0, 32, 32);
            TAB_BRUTE = atlas.getSubimage(128, 32, 32, 32);
            TAB_DB = atlas.getSubimage(128 + 32, 32, 32, 32);
            TAB_CONVERT = atlas.getSubimage(128 + 32 * 2, 32, 32, 32);
            TAB_INFO = atlas.getSubimage(128 + 32 * 3, 32, 32, 32);

            version = getManifestVersion();

            try (InputStream is = InfoTabInfo.class.getClassLoader().getResourceAsStream("Manrope.ttf")) {
                if (is == null) {
                    throw new Error("Can't read font!");
                }

                FONT = Font.createFont(Font.TRUETYPE_FONT, is);
                assert FONT != null;
            }
        } catch (Throwable t) {
            throw new Error(t);
        }
    }

    public static Font getHTFont() {
        return FONT;
    }

    private static String getManifestVersion() {
        try {
            Enumeration<URL> resEnum = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (resEnum.hasMoreElements()) {
                URL url = resEnum.nextElement();
                try (InputStream is = url.openStream()) {
                    if (is != null) {
                        Manifest manifest = new Manifest(is);
                        Attributes mainAttribs = manifest.getMainAttributes();
                        String version = mainAttribs.getValue("Implementation-Version");
                        if (version != null) {
                            return version;
                        }
                    }
                } catch (Exception ignore) {
                }
            }
        } catch (IOException e1) {
            // Silently ignore wrong manifests on classpath?
        }
        return "DEV_VERSION";
    }

    public static String getVersion() {
        return version;
    }

    private Searcher searcher;
    private final String startText;
    private final Properties prop;
    private final JTabbedPane tabbedPane;
    private final InfoTabInfo tabInfo = new InfoTabInfo(this);
    private final Map<String, Integer> TAB_INDEX = new HashMap<>();

    public HashSearcherFrame(Properties prop, Searcher searcher, String startText) {
        super("COD Hash tool (" + getVersion() + " by "+ InfoTabInfo.getAuthors() +")");
        this.searcher = searcher;
        this.startText = startText;
        tabInfo.setLoadedInfo(startText == null ? "" : startText);
        this.prop = prop;
        setSize(width, height);
        setFont(HashSearcherFrame.getHTFont());
        setIconImage(ICON);

        tabbedPane = new JTabbedPane();
        TAB_INDEX.put("search", createSearchTab(tabbedPane, width - 20, height - 40));
        TAB_INDEX.put("large-search", createLargeSearchTab(tabbedPane, width - 20, height - 40));
        TAB_INDEX.put("large-hash", createLargeHashTab(tabbedPane, width - 20, height - 40));
        TAB_INDEX.put("replace", createTabReplace(tabbedPane, width - 20, height - 40));
        //TAB_INDEX.put("brute", createBruteTab(tabbedPane, width - 20, height - 40));
        TAB_INDEX.put("extract", createExtractTab(tabbedPane, width - 20, height - 40));
        TAB_INDEX.put("about", createInfoTab(tabbedPane, width - 20, height - 40));


        tabbedPane.setBackground(Color.WHITE);
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setBackgroundAt(i, Color.WHITE);
        }
        tabbedPane.setBorder(null);

        setContentPane(tabbedPane);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(Color.WHITE);
        setVisible(true);
    }

    private int createSearchTab(JTabbedPane pane, int width, int height) {
        JPanel panel = new JPanel(null);

        pane.addTab("Search", panel);
        pane.setIconAt(pane.getTabCount() - 1, new ImageIcon(TAB_SEARCH));

        JTextField path = new JTextField(Objects.requireNonNullElse(prop.getProperty(Main.CFG_PATH), ""));
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

        if (startText != null) {
            notificationField.setText(startText);
        }

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
                String hashStringValue = Long.toUnsignedString(HashUtils.hashFNV(textContent), 16).toLowerCase();
                String hashObjectValue = Long.toUnsignedString(HashUtils.hashIDF(textContent), 16).toLowerCase();
                hashString.setText(hashStringValue);
                hashObject.setText(hashObjectValue);
                String output = String.join("\n", ExpandTool
                        .expand(textContent)
                        .flatMap(k -> HashSearcherFrame.this.searcher.search(k).stream().map(o -> o.element() + "," + k))
                        .parallel()
                        .collect(Collectors.toSet()));
                notificationField.setText(output.isEmpty() ? "no find" : output);
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

        loadPath.addActionListener(e -> {
            setVisible(false);
            new Thread(() -> {
                String loadOutput;
                try {
                    prop.setProperty(Main.CFG_PATH, path.getText());
                    LoadingFrame.ResultSearcher resultSearcher = LoadingFrame.loadSearcher(prop);
                    this.searcher = resultSearcher.searcher();
                    loadOutput = resultSearcher.text();
                    Main.saveLoad(prop);
                } catch (Throwable t) {
                    StringWriter writer = new StringWriter();
                    t.printStackTrace(new PrintWriter(writer));
                    loadOutput = writer.toString();
                }
                notificationField.setText(loadOutput);
                tabInfo.setLoadedInfo(loadOutput);
                setVisible(true);
            }, "Loader").start();
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
        return pane.getTabCount();
    }

    private int createLargeSearchTab(JTabbedPane pane, int width, int height) {
        JPanel panel = new JPanel(null);

        pane.addTab("Large search", panel);
        pane.setIconAt(pane.getTabCount() - 1, new ImageIcon(TAB_LARGE_SEARCH));
        panel.setBackground(Color.WHITE);

        JTextArea hashes = new JTextArea("");
        JScrollPane scrollHashes = new JScrollPane(hashes, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        JTextArea find = new JTextArea("");
        find.setEditable(false);
        JScrollPane scrollFind = new JScrollPane(find, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        int size = (height - 90) / 2;
        scrollHashes.setBounds(15, 4, width - 35, size);
        scrollFind.setBounds(15, 4 + size + 4, width - 35, size);

        JCheckBox expand = new JCheckBox("Expand");
        expand.setBounds(width - 100, height - 70, 100, 20);
        expand.setBackground(Color.WHITE);

        Runnable loadText = () -> {
            String textContent = hashes.getText();
            Stream<String> stream = Arrays.stream(textContent.split("\n"));
            if (expand.isSelected()) {
                stream = ExpandTool.expand(stream);
            }
            String output = String.join("\n", stream
                    .flatMap(k -> searcher.search(k).stream().map(o -> o.element() + "," + k))
                    .parallel()
                    .collect(Collectors.toCollection(TreeSet::new)));
            find.setText(output.isEmpty() ? "no find" : output);
        };

        hashes.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                loadText.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                loadText.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                loadText.run();
            }
        });

        expand.addActionListener((e) -> loadText.run());

        JButton copyFind = new JButton("Copy");
        copyFind.setBounds(20, height - 70, width - 140, 20);
        copyFind.setBackground(Color.WHITE);
        copyFind.setForeground(Color.BLACK);
        copyFind.setBorder(new LineBorder(Color.LIGHT_GRAY));
        copyFind.addActionListener(l -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(find.getText()), null));

        panel.add(scrollHashes);
        panel.add(scrollFind);
        panel.add(expand);
        panel.add(copyFind);
        return pane.getTabCount();
    }

    private int createLargeHashTab(JTabbedPane pane, int width, int height) {
        JPanel panel = new JPanel(null);

        pane.addTab("Large hash", panel);
        pane.setIconAt(pane.getTabCount() - 1, new ImageIcon(TAB_CONVERT));
        panel.setBackground(Color.WHITE);

        JTextArea hashes = new JTextArea("");
        JScrollPane scrollHashes = new JScrollPane(hashes, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        JTextArea hashesConverted = new JTextArea("");
        hashesConverted.setEditable(false);
        JScrollPane scrollHashesConverted = new JScrollPane(hashesConverted, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        int size = (height - 90) / 2;
        scrollHashes.setBounds(15, 4, width - 35, size);
        scrollHashesConverted.setBounds(15, 4 + size + 4, width - 35, size);

        JCheckBox idf = new JCheckBox("Canon. ID");
        idf.setBounds(width - 100, height - 70, 100, 20);
        idf.setBackground(Color.WHITE);

        JCheckBox sort = new JCheckBox("Sort Result");
        sort.setBounds(width - 200, height - 70, 100, 20);
        sort.setBackground(Color.WHITE);


        Runnable loadText = () -> {
            String textContent = hashes.getText();
            String output;

            if (idf.isSelected()) {
                output = String.join("\n",
                        Arrays.stream(textContent.split("\n"))
                                .filter(l -> !l.isBlank())
                                .map(l -> {
                                    l = l.replace('\\', '/');
                                    return Long.toUnsignedString(HashUtils.hashIDF(l), 16) + "," + l;
                                })
                                .collect(Collectors.toCollection(sort.isSelected() ? TreeSet::new : ArrayList::new))
                );
            } else {
                output = String.join("\n",
                        Arrays.stream(textContent.split("\n"))
                                .filter(l -> !l.isBlank())
                                .map(l -> {
                                    l = l.replace('\\', '/');
                                    if (l.startsWith("scripts/")) {
                                        return "script_" + Long.toUnsignedString(HashUtils.hashFNV(l), 16) + "," + l;
                                    } else {
                                        return "hash_" + Long.toUnsignedString(HashUtils.hashFNV(l), 16) + "," + l;
                                    }
                                })
                                .collect(Collectors.toCollection(sort.isSelected() ? TreeSet::new : ArrayList::new))
                );
            }
            hashesConverted.setText(output);
        };

        hashes.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                loadText.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                loadText.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                loadText.run();
            }
        });

        idf.addActionListener(l -> loadText.run());
        sort.addActionListener(l -> loadText.run());

        JButton copyFind = new JButton("Copy");
        copyFind.setBounds(20, height - 70, width - 240, 20);
        copyFind.setBackground(Color.WHITE);
        copyFind.setForeground(Color.BLACK);
        copyFind.setBorder(new LineBorder(Color.LIGHT_GRAY));
        copyFind.addActionListener(l -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(hashesConverted.getText()), null));

        panel.add(scrollHashes);
        panel.add(idf);
        panel.add(sort);
        panel.add(scrollHashesConverted);
        panel.add(copyFind);
        return pane.getTabCount();
    }

    private int createTabReplace(JTabbedPane pane, int width, int height) {
        JPanel panel = new JPanel(null);

        pane.addTab("Replace", panel);
        pane.setIconAt(pane.getTabCount() - 1, new ImageIcon(TAB_REPLACE));
        panel.setBackground(Color.WHITE);

        JTextArea hashes = new JTextArea("");
        JScrollPane scrollHashes = new JScrollPane(hashes, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        scrollHashes.setBounds(15, 4, width - 35, height - 90);

        JButton copyFind = new JButton("Replace");
        copyFind.setBounds(20, height - 70, width - 40, 20);
        copyFind.setBackground(Color.WHITE);
        copyFind.setForeground(Color.BLACK);
        copyFind.setBorder(new LineBorder(Color.LIGHT_GRAY));
        copyFind.addActionListener(l -> {
            String output;
            try {
                output = ReplacerTool.replace(prop.getProperty(Main.CFG_PATH), hashes.getText());
            } catch (IOException e) {
                StringWriter w = new StringWriter();
                e.printStackTrace(new PrintWriter(w));
                output = w.toString();
            }
            JOptionPane.showMessageDialog(this, output);
        });

        panel.add(scrollHashes);
        panel.add(copyFind);
        return pane.getTabCount();
    }

    private int createBruteTab(JTabbedPane pane, int width, int height) {
        JPanel panel = new JPanel(null);

        pane.addTab("Brute", panel);
        pane.setIconAt(pane.getTabCount() - 1, new ImageIcon(TAB_BRUTE));
        panel.setBackground(Color.WHITE);

        return pane.getTabCount();
    }

    private int createExtractTab(JTabbedPane pane, int width, int height) {
        JPanel panel = new JPanel(null);

        pane.addTab("Extractor", panel);
        pane.setIconAt(pane.getTabCount() - 1, new ImageIcon(TAB_DB));
        panel.setBackground(Color.WHITE);

        JTextField path = new JTextField(Objects.requireNonNullElse(prop.getProperty(Main.CFG_PATH), ""));
        JTextField text = new JTextField(Objects.requireNonNullElse(prop.getProperty(Main.CFG_EXTRACT_PATH), ""));
        JLabel pathLabel = new JLabel("path: ");
        JLabel textLabel = new JLabel("output path: ");
        JTextArea notificationField = new JTextArea();
        JButton loadPath = new JButton("Extract");
        JButton copyNotification = new JButton("Copy");

        path.setBounds(width / 2 - 300, 44, 580, 20);
        text.setBounds(width / 2 - 300, 44 + 30, 580, 20);
        notificationField.setBounds(20, 44 + 30 * 2, width - 40, height - 80 - (44 + 30 * 2));
        copyNotification.setBounds(20, height - 70, width - 40, 20);

        pathLabel.setLabelFor(path);
        pathLabel.setHorizontalAlignment(JLabel.RIGHT);
        pathLabel.setVerticalAlignment(JLabel.CENTER);

        textLabel.setLabelFor(text);
        textLabel.setHorizontalAlignment(JLabel.RIGHT);
        textLabel.setVerticalAlignment(JLabel.CENTER);

        notificationField.setEditable(false);
        notificationField.setAlignmentX(JTextArea.CENTER_ALIGNMENT);
        notificationField.setBorder(new LineBorder(Color.LIGHT_GRAY));
        notificationField.setBackground(Color.WHITE);

        path.setBorder(new LineBorder(Color.DARK_GRAY));
        path.setBackground(Color.WHITE);
        text.setBorder(new LineBorder(Color.DARK_GRAY));
        text.setBackground(Color.WHITE);

        pathLabel.setBounds(path.getX() - 100, path.getY(), 100, path.getHeight());
        textLabel.setBounds(text.getX() - 100, text.getY(), 100, text.getHeight());

        loadPath.setBounds(text.getX() + text.getWidth() + 10, text.getY(), 80, text.getHeight());

        loadPath.setBackground(Color.WHITE);
        loadPath.setForeground(Color.BLACK);
        loadPath.setBorder(new LineBorder(Color.LIGHT_GRAY));

        copyNotification.setBackground(Color.WHITE);
        copyNotification.setForeground(Color.BLACK);
        copyNotification.setBorder(new LineBorder(Color.LIGHT_GRAY));

        loadPath.addActionListener(e -> {
            prop.setProperty(Main.CFG_PATH, path.getText());
            prop.setProperty(Main.CFG_EXTRACT_PATH, text.getText());
            notificationField.setText(DataFetcher.fetch(path.getText(), text.getText()));
            Main.saveLoad(prop);
        });
        copyNotification.addActionListener(l -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(notificationField.getText()), null));

        panel.add(path);
        panel.add(text);
        panel.add(pathLabel);
        panel.add(textLabel);
        panel.add(loadPath);
        panel.add(notificationField);
        panel.add(copyNotification);

        panel.setBackground(Color.WHITE);
        return pane.getTabCount();
    }

    private int createInfoTab(JTabbedPane pane, int width, int height) {
        pane.addTab("Help", tabInfo.getArea());
        pane.setIconAt(pane.getTabCount() - 1, new ImageIcon(TAB_INFO));
        return pane.getTabCount();
    }

    public void handleCommand(String cmd) {
        String[] args = cmd.split(" ");
        try {
            switch (args[0]) {
                case "set-page" -> {
                    Integer integer = TAB_INDEX.get(args[1]);
                    if (integer == null) {
                        throw new IllegalArgumentException("Bad page id: " + args[1]);
                    }
                    tabbedPane.getModel().setSelectedIndex(integer - 1);
                }
                case "alert" ->
                        JOptionPane.showMessageDialog(this, Arrays.stream(args).skip(1).collect(Collectors.joining(" ")));
                default -> throw new Exception("Unknown command: " + args[0]);
            }
        } catch (Exception e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            JOptionPane.showMessageDialog(this, w.toString());
        }
    }
}
