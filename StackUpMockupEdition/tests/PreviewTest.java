import backend.*;
import frontend.*;
import frontend.style.*;
import javax.swing.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
public class PreviewTest {
 public static void main(String[] a)throws Exception{
 System.setProperty("fms.data",Files.createTempDirectory("fms-ui-").resolve("data.dat").toString());
 FinancialService s=FinancialService.getInstance();
 SwingUtilities.invokeAndWait(()->{try{Theme.install();LoginUI l=new LoginUI(s);l.setVisible(true);capture(l,"login-first-launch");l.dispose();s.createAccount("Your Admin","password123".toCharArray(),"Admin");LoginUI normal=new LoginUI(s);normal.setVisible(true);capture(normal,"login");normal.dispose();s.login("Your Admin","password123".toCharArray());DashboardUI d=new DashboardUI(s);d.setVisible(true);java.lang.reflect.Method show=DashboardUI.class.getDeclaredMethod("showPage",String.class);show.setAccessible(true);for(String p:new String[]{"Dashboard","Transaction History","Financial Report","Financial Insights","Activity Log","Accounts"}){show.invoke(d,p);d.validate();capture(d,"admin-"+p.replace(" ","-"));}d.dispose();s.createAccount("Your Employee","password456".toCharArray(),"Employee");s.logout();s.login("Your Employee","password456".toCharArray());d=new DashboardUI(s);d.setVisible(true);for(String p:new String[]{"Dashboard","Financial Record","Transaction History","Payments"}){show.invoke(d,p);d.validate();capture(d,"employee-"+p.replace(" ","-"));}d.dispose();System.out.println("PASS: All ten role pages and login rendered");}catch(Exception e){throw new RuntimeException(e);}});System.exit(0);
 }
 static void capture(JFrame f,String name)throws Exception{BufferedImage b=new BufferedImage(f.getWidth(),f.getHeight(),BufferedImage.TYPE_INT_RGB);f.paint(b.getGraphics());ImageIO.write(b,"png",Path.of("previews",name+".png").toFile());}
}
