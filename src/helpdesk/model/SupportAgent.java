package helpdesk.model;

public class SupportAgent extends User {
    public SupportAgent(long id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public void performAction() {
        System.out.println(getName() + " обрабатывает заявку");
    }
}