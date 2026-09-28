import java.time.LocalDateTime;

public class MeetingRoom {
    private String roomId;
    private int capacity;
    private Calendar calendar;

    public MeetingRoom(String roomId , int capacity , Calendar calendar) {
        this.roomId = roomId;
        this.capacity = capacity;
        this.calendar = calendar;
    }

    public boolean canAccommodate(int people) {
        return capacity >= people;
    }

    public boolean isAvailable(LocalDateTime startTime, LocalDateTime endTime) {
        return calendar.isAvailable(startTime, endTime);
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public String getRoomId() {
        return roomId;
    }
}
