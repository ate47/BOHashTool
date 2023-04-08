package fr.atesab.bo4hash.ui;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InfoTabInfo {
    private static final String TEXT;

    static {
        try {
            try (InputStream is = InfoTabInfo.class.getClassLoader().getResourceAsStream("infoText.html")) {
                if (is == null) {
                    throw new Error("Can't read info text!");
                }

                TEXT = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception t) {
            throw new Error(t);
        }
    }

    private static final String[][] authors = {
            {"ATE47", "https://github.com/ate47"}
    };
    private static final String project = "BOHashTool";
    private static final String projectURL = "https://github.com/ate47/BOHashTool";
    private static final String license = "MIT License";
    private final JEditorPane jep;
    private final Component area;
    private final Map<String, String> dataMap = new HashMap<>();
    private final HashSearcherFrame parent;

    public InfoTabInfo(HashSearcherFrame parent) {
        this.parent = parent;
        jep = new JEditorPane();
        jep.setEditable(false);

        jep.setContentType("text/html");
        jep.setText("<html>Could not load</html>");
        jep.addHyperlinkListener(this::fireHyperLink);
        jep.setFont(HashSearcherFrame.getHTFont());
        area = new JScrollPane(jep);

        dataMap.put("project.name", project);
        dataMap.put("project.version", HashSearcherFrame.getVersion());
        dataMap.put("project.url", projectURL);
        dataMap.put("authors", Arrays.stream(authors).map(s -> "<a href=\"%s\">%s</a>".formatted(s[1], s[0])).collect(Collectors.joining(", ")));
        dataMap.put("license", license);

        updateText();
    }

    private void fireHyperLink(HyperlinkEvent ev) {
        if (ev.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }
        if (ev.getDescription().startsWith("bohashtool:")) {
            parent.handleCommand(ev.getDescription().substring("bohashtool:".length()));
            return;
        }

        URL url = ev.getURL();

        String output = "";
        try {
            if (!Desktop.isDesktopSupported()) {
                output = "Desktop not supported";
                return;
            }
            Desktop.getDesktop().browse(url.toURI());
            output = "";
        } catch (Exception er) {
            StringWriter w = new StringWriter();
            er.printStackTrace(new PrintWriter(w));
            output = w.toString();
        } finally {
            if (!output.isEmpty()) {
                JOptionPane.showMessageDialog(getArea(), output);
            }
        }
    }

    private static final Pattern HTML_VAR_MATCHER = Pattern.compile("\\{\\{([a-zA-Z_\\-.\\d]+)}}");

    private String applyVar(String text, Map<String, String> vars) {
        StringBuilder buffer = new StringBuilder();
        int end = 0;
        Matcher m = HTML_VAR_MATCHER.matcher(text);
        while (m.find()) {
            int mStart = m.start();
            int mEnd = m.end();
            String key = m.group(1);
            if (mStart != end) {
                buffer.append(text, end, mStart);
            }

            String value = vars.get(key);
            if (value != null) {
                buffer.append(value);
                end = mEnd;
            } else {
                end = mStart;
            }
        }
        if (text.length() != end) {
            buffer.append(text, end, text.length());
        }
        return buffer.toString();
    }

    private void updateText() {
        jep.setText(applyVar(TEXT, dataMap));
    }

    public void setLoadedInfo(String loadedInfo) {
        dataMap.put("loader.info", loadedInfo);
        updateText();
    }

    public Component getArea() {
        return area;
    }
}
