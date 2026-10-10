import backend.FinancialService;
import frontend.LoginUI;
import frontend.style.Theme;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

public final class Main {
    /** The tallest page needs about this many device pixels of window height at 100% zoom. */
    private static final double TALLEST_PAGE = 1010;

    public static void main(String[] args) {
        if (relaunchZoomedOut()) return;
        SwingUtilities.invokeLater(() -> {
            try {
                Theme.install();
                new LoginUI(FinancialService.getInstance()).setVisible(true);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null,exception.getMessage(),"Startup error",JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Java reads its UI scale once at start-up. On short screens the app restarts itself
     * zoomed out, so every page fits without scrolling. Returns true when it did so.
     */
    private static boolean relaunchZoomedOut() {
        if (System.getProperty("sun.java2d.uiScale") != null || GraphicsEnvironment.isHeadless()) return false;
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        double system = environment.getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleY();
        Rectangle usable = environment.getMaximumWindowBounds();
        double fit = usable.height * system / TALLEST_PAGE;
        if (fit >= system) return false;
        try {
            String java = ProcessHandle.current().info().command().orElse(null);
            if (java == null) return false;
            List<String> command = new ArrayList<>(List.of(java,
                "-Dsun.java2d.uiScale=" + String.format(Locale.US, "%.2f", Math.max(fit, 0.6))));
            if (System.getProperty("fms.data") != null) command.add("-Dfms.data=" + System.getProperty("fms.data"));
            command.addAll(List.of("-cp", System.getProperty("java.class.path"), "Main"));
            new ProcessBuilder(command).inheritIO().start();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
