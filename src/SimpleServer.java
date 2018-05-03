/**
 * Description of class SimpleServer.
 *
 * @author Andrey Demjanov
 * @version dated Jan 17, 2018
 * @link NULL
 */

/**
 * Authors comments:
 * Доброго времени суток, коллега!
 * Чтобы было проще смотреть нижеприведенную реализацию широковещания привожу описание самой идеи:
 * Отправка сообщений клиенту все так же ведется в той же нити, которая работает с этим клиентом. Но пришедшее от
 * клиента сообщение помещается в общую переменную, и затем отправляется каждому клиенту своей нитью.
 * Для этих целей в класс были добавленны две переменные: MESSAGE_ALL - хранит сообщение, MESSAGE_FLAG_ALL - переменная
 * типа boolean. Переменная MESSAGE_FLAG_ALL в данном случае используется как циклический счетчик от 0 до 1 (или как
 * как объект, который имеет два разных состояния). Нить запоминает состояние данной переменной, когда инициализируется,
 * и когда обращается к общей переменной MESSAGE_ALL. Затем, в следующий момент времени, нить сравнивает запомненное
 * состояние с текущим состоянием MESSAGE_FLAG_ALL, если они не совпадают - значит MESSAGE_ALL обновленна, и пора
 * отсылать клиенту новое сообщение.
 * Если пришло сообщение от клиента, нить начинает его обработку: Если сообщение не является командой выхода и запросом
 * на аутентификацию (эти вещи в коде оставленны без изменения), то нить вызывает синхронизированный метод setMessage,
 * с помощью которого пришедшее сообщение помещается в переменную MESSAGE_ALL и обновляется состояние MESSAGE_FLAG_ALL.
 *
 * Не уверен, что все это "правильная" реализация, но пока опыта в java не много)))
 *
 * По модификатору volatile, который тут применяется, я уже писал в комментарии к файлу Threads_Study.java в задании №5,
 * но он, возможно, не был прочитан, поэтому на всякий случай привожу выдержку из этого комментария:
 * Данный модификатор обеспечивает приоритет операций записи к переменной над оперциями чтения, кроме того "заставляет" нить
 * перед выполнением операции чтения данной переменной актуализировать ее копию в своей локальной памяти из общей памяти.
 * Все это позволяет предотвратить ситуацию, когда нить при обращении к переменной читает ее старое значение из своей локальной памяти
 * в то время, когда значение переменной уже изменено другой нитью.
 */
import java.io.*;
import java.net.*;
import java.sql.*;

class SimpleServer implements IConstants {
    volatile boolean MESSAGE_FLAG_ALL = false;                      //---D---
    volatile String MESSAGE_ALL;                                    //---D---

    public static void main(String[] args) {
        new SimpleServer();
    }

    SimpleServer() {
        int clientCount = 0;
        System.out.println(SERVER_START);
        try (ServerSocket server = new ServerSocket(SERVER_PORT)) {
            while (true) {
                Socket socket = server.accept();
                System.out.println("#" + (++clientCount) + CLIENT_JOINED);
                new Thread(new ClientHandler(socket, clientCount, MESSAGE_FLAG_ALL)).start();       //---D---
            } 
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        System.out.println(SERVER_STOP);
    }

    synchronized private boolean setMessage (String message){       //---D---
        MESSAGE_ALL = message;
        MESSAGE_FLAG_ALL = !MESSAGE_FLAG_ALL; // Просто инверсия значения.

        try{
            wait(100);
        }catch (Exception ex){
            System.out.println(ex.getMessage());
        }

        return MESSAGE_FLAG_ALL;            // Возвращаем текущее состояния флага для запоминания
    }

    /**
     * checkAuthentication: check login and password
     *
     * @param  login for checking
     * @param  passwd for checking
     *
     * @return if the pair login/passwd is found in the database,
     *         authentication is successful
     */
    private boolean checkAuthentication(String login, String passwd) {
        boolean result = false;
        try {
            // loads a class, including running its static initializers
            Class.forName(DRIVER_NAME);
            // connect db
            Connection connect = DriverManager.getConnection(SQLITE_DB);
            // looking for login && passwd in db
            PreparedStatement pstmt = connect.prepareStatement(SQL_SELECT);
            // replace "?" to the login
            pstmt.setString(1, login);
            // returns ResultSet object generated by the query
            ResultSet rs = pstmt.executeQuery();
            // process rows from the query result
            while (rs.next())
                result = rs.getString(PASSWD_COL).equals(passwd);
            // close all
            rs.close();
            pstmt.close();
            connect.close();
        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
            return false;
        }
        return result;
    }

    /**
     * ClientHandler: service requests of clients
     */
    class ClientHandler implements Runnable {
        BufferedReader reader;
        PrintWriter writer;
        Socket socket;
        String name;
        boolean message_flag;

        ClientHandler(Socket clientSocket, int clientCount, boolean message_flag) {
            try {
                socket = clientSocket;
                reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream());
                name = "Client #" + clientCount;
            } catch(Exception ex) {
                System.out.println(ex.getMessage());
            }
            this.message_flag = message_flag;
        }

        @Override
        public void run() {
            String message = "";
            try {
                do {
                    if (reader.ready()) {                                       //---D---
                        message = reader.readLine();
                        if (message != null) {
                            System.out.println(name + ": " + message);
                            if (message.startsWith(AUTH_SIGN)) {
                                String[] wds = message.split(" ");
                                if (checkAuthentication(wds[1], wds[2])) {
                                    name = wds[1];
                                    writer.println("Hello, " + name);
                                    writer.println("\0");
                                } else {
                                    System.out.println(name + ": " + AUTH_FAIL);
                                    writer.println(AUTH_FAIL);
                                    message = EXIT_COMMAND;
                                }
                            } else if (!message.equalsIgnoreCase(EXIT_COMMAND)) {
//                                writer.println("echo: " + message);           //Т.к. чат идет в консоле, не вижу смысла
                                writer.println("\0");                           //посылать сообщение клиенту, которое он сам и написал
                                //---D---
                                message_flag = setMessage(name + ": " + message);  //помещаем сообщение в общую переменную.
                            }
                            writer.flush();
                        }
                    }

                    //---D---
                    if(message_flag != MESSAGE_FLAG_ALL){   // <-- Проверяем состояние общего флага: если не совпадает с запомненным, отсылаем клиенту сообщение.
                        message = MESSAGE_ALL;              // <-- В этой строке специально заложена возможность
                        writer.println(message);            // передать всем нитям через общую переменную exit_command
                        writer.println("\0");               // и тем самым "выгнать" всех с сервера. Но для клиента
                        writer.flush();                     // в текущей реализации эта возможность, конечно, заблокирована.
                        message_flag = MESSAGE_FLAG_ALL;    // <-- Запоминаем состояние общего флага.
                    }
                } while (!message.equalsIgnoreCase(EXIT_COMMAND));
                socket.close();
                System.out.println(name + CLIENT_DISCONNECTED);
            } catch(Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}