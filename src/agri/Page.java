package agri;

import javax.swing.*;
import java.awt.*;

/** Base class of every screen (page). */
public abstract class Page extends JPanel {
    public Page() { super(new BorderLayout(0, 14)); setOpaque(false); }
    /** Reloads the data when the page is shown. */
    public abstract void refresh();
}
