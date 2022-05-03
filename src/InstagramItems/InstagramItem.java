package InstagramItems;

import java.util.Date;

public abstract class InstagramItem {

    private Date date;
    private String user;
    private long pk;

    public Date getDate() { return date; }
    public String getUser() { return user; }
    public long getPk() { return pk; }

    public void setDate(long takenAt) { this.date = new Date(takenAt*1000); }
    public void setUser(String user) { this.user = user; }
    public void setPk(long pk) { this.pk = pk; }
}
