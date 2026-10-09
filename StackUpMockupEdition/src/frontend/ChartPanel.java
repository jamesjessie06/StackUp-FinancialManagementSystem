package frontend;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/** Real-data charts: decorative mockup bars are never saved as transactions. */
public final class ChartPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public enum Mode { CASH_FLOW, MONTHLY, TOTALS, REVENUE_CATEGORIES, EXPENSE_CATEGORIES }
    private final List<FinancialRecord> records;
    private final Mode mode;
    private final int year;
    private final Color[] colors={Theme.GREEN,Theme.RED,Theme.BLUE};
    public ChartPanel(List<FinancialRecord> records, Mode mode, int year) {
        this.records=List.copyOf(records); this.mode=mode; this.year=year; setOpaque(false);
    }
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D pen=(Graphics2D)graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        pen.setFont(new Font("Nimbus Sans",Font.PLAIN,10));
        if (mode==Mode.REVENUE_CATEGORIES || mode==Mode.EXPENSE_CATEGORIES) donut(pen);
        else if(mode==Mode.CASH_FLOW) cashFlow(pen);
        else bars(pen);
        pen.dispose();
    }
    /** Stacked monthly inflow/outflow bars; the stack represents total activity, not profit. */
    private void cashFlow(Graphics2D pen) {
        BigDecimal[][] data=ChartData.monthly(records,year);
        double maximum=1;
        for(int month=0;month<12;month++)maximum=Math.max(maximum,data[0][month].doubleValue()+data[1][month].doubleValue());
        String[] names={"Revenue","Expenses"};Color[] palette={Theme.TEAL,Theme.BLACK};
        for(int i=0;i<2;i++){pen.setColor(palette[i]);pen.fillRoundRect(8+i*95,3,7,7,3,3);pen.setColor(Theme.MUTED);pen.drawString(names[i],20+i*95,10);}
        int left=40,right=getWidth()-6,top=28,bottom=getHeight()-25;
        for(int i=0;i<5;i++){int y=top+(bottom-top)*i/4;pen.setColor(Theme.LINE);pen.drawLine(left,y,right,y);pen.setColor(Theme.MUTED);pen.drawString(String.format(java.util.Locale.US,maximum<=1?"%.2f":"%.0f",maximum*(4-i)/4),0,y+3);}
        String[] months={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        for(int m=0;m<12;m++){
            int x=left+(int)((right-left)*(m+.5)/12);int width=Math.max(4,(right-left)/12-7);int base=bottom;
            for(int part=1;part>=0;part--){int h=(int)(data[part][m].doubleValue()/maximum*(bottom-top));if(h>0){pen.setColor(palette[part]);pen.fillRoundRect(x-width/2,base-h,width,Math.max(1,h-2),5,5);base-=h;}}
            pen.setColor(Theme.MUTED);center(pen,months[m],x,bottom+16);
        }
        if(records.isEmpty()){pen.setColor(Theme.MUTED);center(pen,"No transactions recorded yet",(left+right)/2,(top+bottom)/2);}
    }
    private void donut(Graphics2D pen) {
        String type=mode==Mode.REVENUE_CATEGORIES ? "Revenue" : "Expense";
        Map<String,BigDecimal> data=ChartData.categories(records,type);
        BigDecimal total=ChartData.total(records,type);
        int diameter=Math.max(40,Math.min(getHeight()-35,getWidth()/2));
        int x=data.isEmpty() ? (getWidth()-diameter)/2 : 4;
        int y=(getHeight()-diameter)/2;
        pen.setColor(Theme.LINE); pen.fillOval(x,y,diameter,diameter);
        int angle=90,index=0;
        for (Map.Entry<String,BigDecimal> entry:data.entrySet()) {
            double share=entry.getValue().doubleValue()/total.doubleValue();
            int arc=(int)Math.round(share*360);
            pen.setColor(colors[index%3]); pen.fillArc(x,y,diameter,diameter,angle,arc);
            int ly=18+index*27;
            pen.fillOval(diameter+18,ly-7,7,7); pen.setColor(Theme.MUTED);
            String name=entry.getKey(); if(name.length()>16) name=name.substring(0,15)+"…";
            pen.drawString(name,diameter+31,ly);
            pen.drawString(String.format(java.util.Locale.US,"%.1f%%",share*100),diameter+31,ly+12);
            angle+=arc; index++;
        }
        pen.setColor(Color.WHITE);
        int hole=(int)(diameter*.68); pen.fillOval(x+(diameter-hole)/2,y+(diameter-hole)/2,hole,hole);
        pen.setColor(Theme.INK); center(pen,Theme.money(total),x+diameter/2,y+diameter/2);
        if(data.isEmpty()) { pen.setColor(Theme.MUTED); center(pen,"No records yet",getWidth()/2,getHeight()-4); }
    }
    private void bars(Graphics2D pen) {
        String[] names={"Revenue","Expenses","Profit"};
        for(int s=0;s<3;s++) {
            pen.setColor(colors[s]); pen.fillOval(10+s*92,4,6,6);
            pen.setColor(Theme.MUTED); pen.drawString(names[s],20+s*92,10);
        }
        BigDecimal[][] monthly=ChartData.monthly(records,year);
        double[] totals={ChartData.total(records,"Revenue").doubleValue(),ChartData.total(records,"Expense").doubleValue(),0};
        totals[2]=totals[0]-totals[1];
        double max=records.isEmpty()?1:1,min=0;
        if(mode==Mode.MONTHLY) for(BigDecimal[] series:monthly) for(BigDecimal value:series) {
            max=Math.max(max,value.doubleValue()); min=Math.min(min,value.doubleValue());
        }
        else for(double value:totals) { max=Math.max(max,value); min=Math.min(min,value); }
        int left=32,right=getWidth()-10,top=28,bottom=getHeight()-24;
        for(int i=0;i<5;i++) {
            int y=top+(bottom-top)*i/4; pen.setColor(Theme.LINE); pen.drawLine(left,y,right,y);
            pen.setColor(Theme.MUTED);
            pen.drawString(String.format(java.util.Locale.US,max<=1?"%.2f":"%.0f",max-(max-min)*i/4),0,y+3);
        }
        int zero=position(0,min,max,top,bottom);
        int groups=mode==Mode.MONTHLY?12:3;
        String[] months={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        for(int group=0;group<groups;group++) {
            int center=left+(int)((right-left)*(group+.5)/groups);
            int width=Math.max(3,Math.min(15,(right-left)/groups/4));
            for(int series=0;series<(mode==Mode.MONTHLY?3:1);series++) {
                double value=mode==Mode.MONTHLY?monthly[series][group].doubleValue():totals[group];
                int y=position(value,min,max,top,bottom);
                pen.setColor(colors[mode==Mode.MONTHLY?series:group]);
                if(value!=0) pen.fillRoundRect(center+(mode==Mode.MONTHLY?(series-1)*width:-width),
                    Math.min(zero,y),mode==Mode.MONTHLY?width-1:width*2,Math.max(1,Math.abs(zero-y)),6,6);
            }
            pen.setColor(Theme.MUTED); center(pen,mode==Mode.MONTHLY?months[group]:names[group],center,bottom+17);
        }
        if(records.isEmpty()) {
            int cx=(left+right)/2,cy=(top+bottom)/2;
            pen.setColor(new Color(238,249,243));
            pen.fillRoundRect(cx-105,cy-27,210,54,18,18);
            pen.setColor(Theme.TEAL);
            center(pen,"Your chart starts with your data",cx,cy-3);
            pen.setColor(Theme.MUTED);
            center(pen,"No transactions recorded yet",cx,cy+13);
        }
    }
    private int position(double value,double min,double max,int top,int bottom) {
        return bottom-(int)((value-min)/(max-min)*(bottom-top));
    }
    private void center(Graphics2D pen,String text,int x,int y) {
        Font original=pen.getFont();
        if(text.contains("₱"))pen.setFont(new Font("DejaVu Sans",original.getStyle(),original.getSize()));
        pen.drawString(text,x-pen.getFontMetrics().stringWidth(text)/2,y);
        pen.setFont(original);
    }
}
