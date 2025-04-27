import java.io.IOException;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static HashFile hashFile;

    public static void main(String[] args) {
        try {
            hashFile = new HashFile("students.dat");
            System.out.println("ЛАБА 4: Организация хешированного файла");
            runMainLoop();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка работы с файлом: " + e.getMessage());
        } finally {
            closeResources();
        }
    }

    private static void runMainLoop() throws IOException, ClassNotFoundException {
        while (true) {
            printMenu();
            int choice = readIntInput("Выберите действие: ", 1, 5);

            switch (choice) {
                case 1 -> addStudent();
                case 2 -> findStudent();
                case 3 -> deleteStudent();
                case 4 -> updateStudent();
                case 5 -> {
                    System.out.println("Завершение работы...");
                    return;
                }
            }
        }
    }

    private static void printMenu() {
        System.out.println("\nМЕНЮ");
        System.out.println("1. Добавить студента");
        System.out.println("2. Найти студента");
        System.out.println("3. Удалить студента");
        System.out.println("4. Изменить данные студента");
        System.out.println("5. Выход");
    }

    private static int readIntInput(String prompt, int min, int max) {
        while (true) {
            try {
                System.out.print(prompt);
                int value = Integer.parseInt(scanner.nextLine());
                if (value >= 1 && value <= max) {
                    return value;
                }
                System.out.printf("Введите число от %d до %d!%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите целое число!");
            }
        }
    }



    private static String readStringInput(String prompt, int maxLength, String defaultValue) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.isEmpty() && defaultValue != null) return defaultValue;

        while (input.length() > maxLength || input.isEmpty()) {
            if (input.isEmpty()) {
                System.out.print("Поле не может быть пустым. Введите снова: ");
            } else {
                System.out.printf("Превышена максимальная длина (%d символов)! Повторите ввод: ", maxLength);
            }
            input = scanner.nextLine().trim();
            if (input.isEmpty() && defaultValue != null) return defaultValue;
        }
        return input;
    }



    private static Zap readStudentData(Zap oldData) throws IOException, ClassNotFoundException {
        boolean isNew = (oldData == null);
        int oldId = isNew ? -1 : oldData.getId_zachet();

        int id = readIntInput(
                (isNew ? "Номер зачётки: " : "Новый номер зачётки [" + oldData.getId_zachet() + "]: "),
                1, Integer.MAX_VALUE
        );

        if ((isNew || id != oldId) && hashFile.findStudent(id) != null) {
            System.out.println("Ошибка: студент с таким номером зачётки уже существует!");
            return null;
        }

        int group = readIntInput(
                (isNew ? "Номер группы: " : "Новый номер группы [" + oldData.getId_gr() + "]: "),
                1, Integer.MAX_VALUE
        );

        String surname = readStringInput("Фамилия" + (isNew ? "" : " [" + oldData.getSurname() + "]") + ": ",
                Zap.SURNAME_LENGTH, isNew ? null : oldData.getSurname());

        String name = readStringInput("Имя" + (isNew ? "" : " [" + oldData.getName() + "]") + ": ",
                Zap.NAME_LENGTH, isNew ? null : oldData.getName());

        String patronymic = readStringInput("Отчество" + (isNew ? "" : " [" + oldData.getPatronymic() + "]") + ": ",
                Zap.PATRONYMIC_LENGTH, isNew ? null : oldData.getPatronymic());

        return new Zap(id, group, surname, name, patronymic);
    }



    private static void addStudent() throws IOException, ClassNotFoundException {
        System.out.println("\nДобавление нового студента:");
        Zap student = readStudentData(null);
        if (student == null) return;

        hashFile.addStudent(student);
        System.out.println("Студент успешно добавлен!" );
    }



    private static void updateStudent() throws IOException, ClassNotFoundException {
        System.out.println("\nИзменение данных студента:");
        int id = readIntInput("Введите номер зачётки для изменения: ", 1, Integer.MAX_VALUE);

        Zap existing = hashFile.findStudent(id);
        if (existing == null) {
            System.out.println("Студент с таким номером зачётки не найден!");
            return;
        }
        System.out.println("Текущие данные:");
        System.out.println(existing);

        Zap updated = readStudentData(existing);
        if (updated == null) return;

        hashFile.updateStudent(id, updated);
        System.out.println("Данные студента успешно обновлены!");
    }



    private static void findStudent() throws IOException, ClassNotFoundException {
        int id = readIntInput("\nВведите номер зачётки для поиска: ", 1, Integer.MAX_VALUE);
        Zap student = hashFile.findStudent(id);

        if (student != null) {
            System.out.println("Найден студент:\n" + student);
        } else {
            System.out.println("Студент с таким номером зачётки не найден!");
        }
    }



    private static void deleteStudent() throws IOException, ClassNotFoundException {
        int id = readIntInput("\nВведите номер зачётки для удаления: ", 1, Integer.MAX_VALUE);

        if (hashFile.deleteStudent(id)) {
            System.out.println("Студент успешно удалён!");
        } else {
            System.out.println("Студент с таким номером зачётки не найден!");
        }
    }



    private static void closeResources() {
        try {
            if (hashFile != null) hashFile.close();
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии файла: " + e.getMessage());
        }
        scanner.close();
    }
}
