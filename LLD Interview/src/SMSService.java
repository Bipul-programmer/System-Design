public class SMSService implements NotificationService{
    public void Notification(User user , Meeting meeting) {
        System.out.printf("Notification of the %s sent to user by SMS%n" , meeting.getMeetingId());
    }
}
