import java.io.Serializable;

public class Zap implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int SURNAME_LENGTH = 30;
    public static final int NAME_LENGTH = 20;
    public static final int PATRONYMIC_LENGTH = 30;

    private int id_zachet;
    private String surname;
    private String name;
    private String patronymic;
    private int id_gr;

    public Zap(int id_zachet, int id_gr, String surname, String name, String patronymic) {
        this.id_zachet = id_zachet;
        this.surname = surname;
        this.name = name;
        this.patronymic = patronymic;
        this.id_gr = id_gr;
    }

    public int getId_zachet() {
        return id_zachet;
    }

    public String getSurname() {
        return surname;
    }

    public String getName() {
        return name;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public int getId_gr() {
        return id_gr;
    }

    @Override
    public String toString() {
        return "----------------------------" +
                "\nЗачетка: " + id_zachet +
                "\nГруппа: " + id_gr +
                "\nИмя: " + name +
                "\nФамилия: " + surname +
                "\nОтчество: " + patronymic +
                "\n----------------------------";
    }
}
