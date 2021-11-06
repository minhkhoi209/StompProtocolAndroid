package ua.naiksoftware.stompclientexample;


/**
 * Created by Naik on 24.02.17.
 */
public class ChatMessageReq {

    private String sender;
    private String timeSend;
    private String message;
    private String linkImage;
    private String linkVideo;

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTimeSend() {
        return timeSend;
    }

    public void setTimeSend(String timeSend) {
        this.timeSend = timeSend;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLinkImage() {
        return linkImage;
    }

    public void setLinkImage(String linkImage) {
        this.linkImage = linkImage;
    }

    public String getLinkVideo() {
        return linkVideo;
    }

    public void setLinkVideo(String linkVideo) {
        this.linkVideo = linkVideo;
    }
}
