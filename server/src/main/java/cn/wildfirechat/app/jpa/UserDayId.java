package cn.wildfirechat.app.jpa;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

public class UserDayId implements Serializable {
	public String userId;
	public long day;

	public UserDayId() {
	}

	public UserDayId(String userId, long day) {
		this.userId = userId;
		this.day = day;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof UserDayId)) {
			return false;
		}
		UserDayId that = (UserDayId) object;
		return day == that.day && Objects.equals(userId, that.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, day);
	}
}
