package librarysystem;

// Member class
public class Member {

    // variables
    private int id;
    private String name;
    private String contactDetails;

    // constructor
    public Member(int id, String name, String contactDetails) {

        this.id = id;
        this.name = name;
        this.contactDetails = contactDetails;
    }

    // getter method
    public int getMemberId() {

        return id;
    }

    public String getName() {
        return name;
    }

    public String getContactDetails() {
        return contactDetails;
    }

    // display member method
    public void displayMember() {

        System.out.println("Member ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Contact: " + contactDetails);
    }

    // method calling another method
    public void display() {

        displayMember();
    }
}