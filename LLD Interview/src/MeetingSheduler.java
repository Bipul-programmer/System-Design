import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class MeetingSheduler {
    private final List<MeetingRoom> room;
    private final NotificationService notificationService;

    public MeetingSheduler(List<MeetingRoom> room , NotificationService notificationService) {
        this.room = room;
        this.notificationService = notificationService;
    }

    public MeetingRoom MeetingSec(LocalDateTime start , LocalDateTime end , int count) {
        for (MeetingRoom meetings : room) {
            if (meetings.canAccommodate(count) && meetings.isAvailable(start , end)) {
                return meetings;
            }
        }
        return null;
    }
}
