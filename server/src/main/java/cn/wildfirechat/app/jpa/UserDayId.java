package cn.wildfirechat.app.jpa;

import javax.persistence.*;
import java.io.Serializable;

public class UserDayId implements Serializable {
	public String userId;
	public long day;

	public UserDayId() {
	}

	public UserDayId(String userId, long day) {
		this.userId = userId;
		this.day = day;
	}
}
