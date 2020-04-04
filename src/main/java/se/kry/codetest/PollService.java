package se.kry.codetest;

import java.time.LocalDate;

public class PollService {
    private String name;
    private String url;
    private Status status;
    private LocalDate creationDate;

    public PollService(String name, String url, Status status, LocalDate creationDate) {
        this.name = name;
        this.url = url;
        this.status = status;
        this.creationDate = creationDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }
}
