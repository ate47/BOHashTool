package fr.atesab.bo4hash.ui;

import fr.atesab.bo4hash.Main;
import fr.atesab.bo4hash.Searcher;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.util.Properties;

public class LoadingFrame extends JFrame implements Searcher.IndexListener {
    public record ResultSearcher(Searcher searcher, String text) {
    }
    public static ResultSearcher loadSearcher(Properties prop) {
        String path = prop.getProperty(Main.CFG_PATH);
        Searcher searcher = new Searcher();
        String result;
        if (path != null) {
            LoadingFrame loading = new LoadingFrame();
            result = searcher.load(path, loading);
            loading.setVisible(false);
            loading.dispose();
        } else {
            result = "Waiting for GSC files";
        }
        return new ResultSearcher(searcher, result);
    }

    private final JTextField area;

    public LoadingFrame() {
        super("Loading scripts");
        setIconImage(HashSearcherFrame.ICON);
        JPanel panel = new JPanel(null);

        area = new JTextField();
        area.setEditable(false);
        area.setBorder(null);
        area.setBounds(15, 70, 360, 30);
        area.setBackground(Color.WHITE);
        area.setAutoscrolls(true);

        panel.add(area);
        setContentPane(panel);
        setResizable(false);
        setSize(400, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    @Override
    public void notification(String text) {
        System.out.println(text);
        area.setText(text);
    }
}
