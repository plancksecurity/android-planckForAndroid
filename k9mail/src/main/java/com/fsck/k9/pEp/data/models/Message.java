package com.fsck.k9.pEp.data.models;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Address;

import org.pEp.jniadapter.Rating;

import java.util.Arrays;

public class Message {
    private Address[] from;
    private Address[] to;
    private Address[] CC;
    private Rating PEpRating;
    private CharSequence displayDate;
    private int threadCount;
    private String subject;
    private boolean read;
    private boolean flagged;
    private boolean answered;
    private boolean forwarded;
    private boolean hasAttachments;
    private long uniqueId;
    private String uid;
    private int position;
    private String folderName;
    private String previewTypeString;
    private String preview;
    private long folderId;
    private long rootId;
    private Account account;

    public void setFrom(Address[] from) {
        this.from = from;
    }

    public void setTo(Address[] to) {
        this.to = to;
    }

    public void setCC(Address[] CC) {
        this.CC = CC;
    }

    public void setPEpRating(Rating PEpRating) {
        this.PEpRating = PEpRating;
    }

    public void setDisplayDate(CharSequence displayDate) {
        this.displayDate = displayDate;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    public void setHasAttachments(boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }

    public void setUniqueId(long uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void setPreviewTypeString(String previewTypeString) {
        this.previewTypeString = previewTypeString;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public Address[] getFrom() {
        return from;
    }

    public Address[] getTo() {
        return to;
    }

    public Address[] getCC() {
        return CC;
    }

    public Rating getPEpRating() {
        return PEpRating;
    }

    public CharSequence getDisplayDate() {
        return displayDate;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public String getSubject() {
        return subject;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public boolean isAnswered() {
        return answered;
    }

    public boolean isForwarded() {
        return forwarded;
    }

    public boolean isHasAttachments() {
        return hasAttachments;
    }

    public long getUniqueId() {
        return uniqueId;
    }

    public String getUid() {
        return uid;
    }

    public int getPosition() {
        return position;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getPreviewTypeString() {
        return previewTypeString;
    }

    public String getPreview() {
        return preview;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    public void setRootId(long rootId) {
        this.rootId = rootId;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public long getFolderId() {
        return folderId;
    }

    public long getRootId() {
        return rootId;
    }

    public Account getAccount() {
        return account;
    }

    @Override
    public String toString() {
        return "Message{" +
                "from=" + Arrays.toString(from) +
                ", to=" + Arrays.toString(to) +
                ", CC=" + Arrays.toString(CC) +
                ", PEpRating=" + PEpRating +
                ", displayDate=" + displayDate +
                ", threadCount=" + threadCount +
                ", subject='" + subject + '\'' +
                ", read=" + read +
                ", flagged=" + flagged +
                ", answered=" + answered +
                ", forwarded=" + forwarded +
                ", hasAttachments=" + hasAttachments +
                ", uniqueId=" + uniqueId +
                ", uid='" + uid + '\'' +
                ", position=" + position +
                ", folderName='" + folderName + '\'' +
                ", previewTypeString='" + previewTypeString + '\'' +
                ", preview='" + preview + '\'' +
                ", folderId=" + folderId +
                ", rootId=" + rootId +
                ", account=" + account +
                '}';
    }
}
