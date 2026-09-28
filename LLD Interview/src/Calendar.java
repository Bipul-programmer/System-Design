import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Calendar {
    private final List<Meeting> meetings = new ArrayList<>();
    public boolean isAvailable(
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {

        for (Meeting meeting : meetings) {

            if (isOverlapping(
                    startTime,
                    endTime,
                    meeting.getStartTime(),
                    meeting.getEndTime()
            )) {
                return false;
            }
        }
        return true;
    }

    private boolean isOverlapping(
            LocalDateTime start1,
            LocalDateTime end1,
            LocalDateTime start2,
            LocalDateTime end2
    ) {
        return start1.isBefore(end2)
                && end1.isAfter(start2);
    }

    public void addMeeting(Meeting meeting) {
        meetings.add(meeting);
    }

    public void removeMeeting(Meeting meeting) {
        meetings.remove(meeting);
    }

    public List<Meeting> getMeetings() {
        return Collections.unmodifiableList(meetings);
    }
}