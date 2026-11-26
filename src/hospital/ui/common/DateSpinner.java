package hospital.ui.common;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * 只选日期的微调框，格式 yyyy-MM-dd，对外用 LocalDate 交互。
 */
public class DateSpinner extends JSpinner {

    private static final String PATTERN = "yyyy-MM-dd";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 26;

    public DateSpinner() {
        this(LocalDate.now());
    }

    public DateSpinner(LocalDate date) {
        super(new SpinnerDateModel(toDate(date), null, null, Calendar.DAY_OF_MONTH));
        setEditor(new JSpinner.DateEditor(this, PATTERN));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
    }

    public LocalDate getLocalDate() {
        Object value = getValue();
        if (!(value instanceof Date)) {
            return LocalDate.now();
        }
        return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public void setLocalDate(LocalDate date) {
        setValue(toDate(date));
    }

    private static Date toDate(LocalDate date) {
        LocalDate value = date == null ? LocalDate.now() : date;
        return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
