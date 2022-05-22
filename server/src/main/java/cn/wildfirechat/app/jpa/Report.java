package cn.wildfirechat.app.jpa;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@IdClass(UserDayId.class)
@Table(name = "t_report")
public class Report implements Serializable {
	@Id
	@Column(length = 64)
	public String userId;
	
	@Id
	@Column
	public long day;

	@Column(length = 64)
	public String title;

	@Column(length = 2048)
	public String content;

	@Column(length = 2048)
	public String tomorrowPlan;

	@Column(length = 2048)
	public String requirement;

	@Column(length = 2048)
	public String reportTo;

	@Column(length = 2048)
	public String ccTo;

	@Column(length = 2048)
	public String reportToGroup;

	@Column(length = 2048)
	public String ccToGroup;
	
}
