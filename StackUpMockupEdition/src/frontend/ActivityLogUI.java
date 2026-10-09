package frontend;

import javax.swing.table.DefaultTableModel;

public final class ActivityLogUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public ActivityLogUI(DashboardUI window) {
        super(window);DefaultTableModel rows=model("Action","User","Details","Date");
        for(String entry:service.getActivity()) {
            String[] parts=entry.split(" \\| ",3);
            if(parts.length==3) {
                String[] action=parts[2].split(": ",2);
                rows.addRow(new Object[]{action[0],parts[1],action.length>1?action[1]:"",parts[0].split("T")[0]});
            }
        }
        add(tableArea(table(rows),"No activity yet."));
    }
}
