package cn.wildfirechat.app.pojo;

import javax.persistence.Column;
import javax.persistence.Id;
import java.util.List;

public class PojoReport {
    public String userId;
    public long day;
    public String title;
    public String content;
    public String tomorrowPlan;
    public String requirement;
    public List<String> reportTo;
    public List<String> ccTo;
    public List<String> reportToGroup;
    public List<String> ccToGroup;
}
