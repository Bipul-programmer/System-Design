import java.time.LocalDateTime;
import java.util.List;

public class Meeting {

    private final String meetingId;
    private final String title;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    private MeetingRoom room;
    private final List<User> participants;

    public Meeting(
            String meetingId,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<User> participants
    ) {
        this.meetingId = meetingId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.participants = participants;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public int getParticipantCount() {
        return participants.size();
    }

    public void assignRoom(MeetingRoom room) {
        this.room = room;
    }
}