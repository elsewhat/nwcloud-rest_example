package org.persistence;

import javax.persistence.*;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
@Entity
@Table(name = "T_FEEDENTRY")
@NamedQuery(name = "AllFeedEntries", query = "select f from FeedEntry f")
public class FeedEntry {

	@Id
	@GeneratedValue
	private long id;
	@Basic
	private String senderName;
	@Basic
	private String feedText;
	@Basic
	private String senderEmail;

	@Basic
	private boolean isComment;
	@Basic
	private String parent;
	@Temporal(TemporalType.TIMESTAMP)
	@Basic
	private Date timeCreated;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setSenderName(String param) {
		this.senderName = param;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setFeedText(String param) {
		this.feedText = param;
	}

	public String getFeedText() {
		return feedText;
	}

	public void setSenderEmail(String param) {
		this.senderEmail = param;
	}

	public String getSenderEmail() {
		return senderEmail;
	}

	public void setIsComment(boolean param) {
		this.isComment = param;
	}

	public boolean isIsComment() {
		return isComment;
	}

	public void setParent(String param) {
		this.parent = param;
	}

	public String getParent() {
		return parent;
	}

	public void setTimeCreated(Date param) {
		this.timeCreated = param;
	}

	public Date getTimeCreated() {
		return timeCreated;
	}

}