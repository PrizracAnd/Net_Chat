/**
 * Description of class CRUDapplication.
 *
 * @author Andrey Demjanov
 * @version dated Jan 17, 2018
 * @link NULL
 */

/**
 * Authors comments:
 * Доброго времени суток, коллега!
 * На SQL пишу на работе под разными СУБД, так что сам язык мне знаком)) Но опыта написания CRUD приложений, увы, не было((
 * Поэтому попытался что-то "изобразить". Реализовать решил, как и говорилось на занятии, методами. Получилось, что все методы
 * ориентированны на работу с консолью, а само приложение работает только с одной конкретной таблицей.
 * В целях тестирования я специально перенес в данное приложение создание таблицы и двух пользователей.
 * !!! Данное приложение так же, как и MakeDBFile использует интерфейс IConstants.
 * !!! В этот интерфейс я добавил еще одну константу, поэтому так же в архиве с файлами есть измененная версия IConstants.
 */
import java.sql.*;
import java.util.Scanner;

public class CRUDapplication implements IConstants {
    final static String NAME_TABLE = "users";
    final static String SQL_CREATE_TABLE =
            "DROP TABLE IF EXISTS " + NAME_TABLE + ";" +
                    "CREATE TABLE " + NAME_TABLE +
                    "(login  CHAR(6) PRIMARY KEY NOT NULL," +
                    " passwd CHAR(6) NOT NULL);";
    final static String SQL_INSERT_MIKE =
            "INSERT INTO " + NAME_TABLE +
                    " (login, passwd) " +
                    "VALUES ('mike', 'qwe');";
    final static String SQL_INSERT_JONH =
            "INSERT INTO " + NAME_TABLE +
                    " (login, passwd) " +
                    "VALUES ('john', 'rty');";
    final static String SQL_SELECT_ALL = "SELECT * FROM " + NAME_TABLE + ";";
    final static String SQL_UPDATE =
            "UPDATE " + NAME_TABLE +
                " SET ";
//                "WHERE login=?;";
    final static String SQL_DELETE_ALL = "DELETE FROM " + NAME_TABLE + ";";
//    final static String SQL_DELETE = "DELETE FROM " + NAME_TABLE + ";";
    final static String EXEMPLECOMMAND = "Available this commands:\n" +
            "-------------------------------------------------------------------\n" +
            "create loginvalue passwordvalue\t--create record (row);\n" +
            "read                           \t--read all record;\n" +
            "read loginvalue                \t--read certain record;\n" +
            "update loginvalue              \t--update certain record;\n" +
            "delete                         \t--delete all(!!!) records;\n" +
            "delete loginvalue              \t--delete certain record;\n" +
            "man                            \t--output this helps information;\n" +
            "exit                           \t--exit.\n" +
            "-------------------------------------------------------------------\n";
    private static Scanner sc = new Scanner(System.in);


    public static void main(String[] args) {
        try {
            // loads a class, including running its static initializers
            Class.forName(DRIVER_NAME);
            // attempts to establish a connection to the given database URL
            Connection connect = DriverManager.getConnection(SQLITE_DB);
            // сreates an object for sending SQL statements to the database
            Statement stmt = connect.createStatement();

            // create table
            stmt.executeUpdate(SQL_CREATE_TABLE);

            // insert record(s)
            stmt.executeUpdate(SQL_INSERT_MIKE);
            stmt.executeUpdate(SQL_INSERT_JONH);

            // print records
            ResultSet rs = stmt.executeQuery(SQL_SELECT_ALL);
            System.out.println("LOGIN\tPASSWD");
            while (rs.next())
                System.out.println(
                        rs.getString("login") + "\t" +
                                rs.getString(PASSWD_COL));
            rs.close();
            stmt.close();
            //---D---
            System.out.println(EXEMPLECOMMAND);
            String command;
            do{
                System.out.print("command:");
                command = sc.nextLine();
                String[] wds = command.split(" ");
                switch (wds[0]){
                    case "create":
                        createRecord(connect, command);
                        break;
                    case "read":
                        readRecord(connect, command);
                        break;
                    case "update":
                        updateRecord(connect, command);
                        break;
                    case "delete":
                        deleteRecord(connect, command);
                        break;
                    case "man":
                        System.out.println(EXEMPLECOMMAND);
                        break;
                    default:
                        if (!wds[0].equalsIgnoreCase(EXIT_COMMAND))
                            System.out.println("Not available command.");
                }
            } while(!command.equalsIgnoreCase(EXIT_COMMAND));

            connect.close();
        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void createRecord(Connection connect, String usercommand) {

        String[] wds = usercommand.split(" ");
        if (wds.length < 3){
            System.out.println("Not enough values!");
        } else {
            boolean l = true;
            try (Statement stmt = connect.createStatement()) {
//                Class.forName(DRIVER_NAME);
                stmt.executeUpdate(
                        "INSERT INTO " + NAME_TABLE +
                                " (login, passwd) " +
                                "VALUES ('" + wds[1] + "', '" + wds[2] + "');"
                );
                stmt.close();
            } catch (SQLException ex) {
                l = false;
                System.out.println(ex.getMessage());
            }
            if (l) {
                System.out.println("Record created successful:\n");
                readRecord(connect, "read " + wds[1]);
            }
        }
    }

    public  static void readRecord(Connection connect, String usercommand) {
        String[] wds = usercommand.split(" ");
        try{
            ResultSet rs;
            if (wds.length < 2){
                Statement stmt = connect.createStatement();
                rs = stmt.executeQuery(SQL_SELECT_ALL);
                System.out.println("LOGIN\tPASSWD");
                while (rs.next())
                    System.out.println(
                            rs.getString("login") + "\t" +
                                    rs.getString(PASSWD_COL));
                rs.close();
                stmt.close();
            }else {
                PreparedStatement ptmt = connect.prepareStatement(SQL_SELECT);
                ptmt.setString(1, wds[1]);
                rs = ptmt.executeQuery();
                System.out.println("LOGIN\tPASSWD");
                while (rs.next())
                    System.out.println(
                            rs.getString("login") + "\t" +
                                    rs.getString(PASSWD_COL));
                rs.close();
                ptmt.close();
            }


        }catch (SQLException ex){
            System.out.println(ex.getMessage());
        }
    }

    public static void updateRecord(Connection connect, String usercommand) {
        String[] wds = usercommand.split(" ");

        if (wds.length < 2){
            System.out.println("Not enough loginvalue!");
        } else{
            boolean l = true;
            String login = wds[1];
            System.out.println("Please enter number of field to update:\n" +
                    "1 - login;\n" +
                    "2 - password;\n" +
                    "3 - login and password;\n" +
                    "other numbers - cancel. ");
            System.out.print("number:");
            try (Statement stmt = connect.createStatement()){
                switch (sc.nextInt()) {
                    case 1:
                        sc.nextLine();
                        System.out.print("new loginvalue:");
                        login = sc.nextLine();
                        stmt.executeUpdate(SQL_UPDATE + "login='" + login + "' WHERE login='" + wds[1] + "';");
                        break;
                    case 2:
                        sc.nextLine();
                        System.out.print("new passwordvalue:");
                        stmt.executeUpdate(SQL_UPDATE +"passwd = '" + sc.nextLine() + "' WHERE login = '" + wds[1] + "';");
                        break;
                    case 3:
                        sc.nextLine();
                        System.out.print("new loginvalue:");
                        login = sc.nextLine();
                        System.out.print("new passwordvalue:");
                        stmt.executeUpdate(SQL_UPDATE + "login='" + login + "', passwd='" + sc.nextLine() + "' WHERE login='" + wds[1] + "';");
                        break;
                    default:
                        l = false;
                        sc.nextLine();
                        break;
                }
                stmt.close();
            }catch (SQLException ex){
                l = false;
                System.out.println(ex.getMessage());
            }
            if (l) {
                System.out.println("Record updated successful:\n");
                readRecord(connect, "read " + login);
            }

        }
    }
    public static void deleteRecord(Connection connect, String usercommand) {
        String[] wds = usercommand.split(" ");

        if (wds.length < 2){
            System.out.println("Delete ALL(!!!) records?\n1 - YES;\nOther numbers - NO.");
            System.out.print("number:");
            if(sc.nextInt() == 1){
                try(Statement stmt = connect.createStatement()){
                    stmt.executeUpdate(SQL_DELETE_ALL);
                    stmt.close();
                    System.out.println("ALL records deleted successful.");
                }catch (SQLException ex){
                    System.out.println(ex.getMessage());
                }
            }
        }else {
            System.out.println("Delete record login = " + wds[1] + "?\n1 - YES;\nOther numbers - NO.");
            System.out.print("number:");
            if(sc.nextInt() == 1){
                try(PreparedStatement ptmt = connect.prepareStatement(SQL_DELETE)){
                    ptmt.setString(1, wds[1]);
                    ptmt.executeUpdate();
                    ptmt.close();
                    System.out.println("Record login = " + wds[1] + " deleted successful.");
                }catch (SQLException ex){
                    System.out.println(ex.getMessage());
                }
            }
        }
        sc.nextLine();
    }


}
