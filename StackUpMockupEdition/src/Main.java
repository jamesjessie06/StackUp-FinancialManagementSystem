import backend.FinancialService;
import frontend.LoginUI;
import frontend.Theme;
import javax.swing.*;

public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Theme.install();
                new LoginUI(FinancialService.getInstance()).setVisible(true);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null,exception.getMessage(),"Startup error",JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
