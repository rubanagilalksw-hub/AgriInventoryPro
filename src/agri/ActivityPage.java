package agri;

import java.util.*;
import java.util.List;

/** Activity log (who changed what) - Admin only. */
public class ActivityPage extends CrudPage {
    public ActivityPage() { super(new String[]{"#", "Time", "User", "Action"}, false, false); }

    @Override protected List<Object[]> rows(String f) {
        List<Object[]> l = new ArrayList<>();
        int n = 0;
        for (Models.Activity a : DataStore.activities) {
            n++;
            if (!f.isEmpty() && !(a.user.toLowerCase().contains(f) || a.action.toLowerCase().contains(f))) continue;
            l.add(new Object[]{n, a.time, a.user, a.action});
        }
        return l;
    }
}
