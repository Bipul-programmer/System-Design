import javax.xml.transform.Source;

public class EmailService implements NotificationService{

    public void Notification(User user , Meeting meeting) {
        System.out.printf("Notification of the %s sent to user by Email%n" , meeting.getMeetingId());
    }

}
