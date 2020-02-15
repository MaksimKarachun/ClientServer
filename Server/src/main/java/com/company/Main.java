package com.company;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {

    static final String serverName = "socket_server_v1.0";

    private static Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        //запуск сервера на указанном порту
        try (ServerSocket server = new ServerSocket(4321)) {

            while (true) {
                //ожидание клиента
                Socket client = server.accept();
                logger.info("client is connected");

                DataOutputStream out = new DataOutputStream(client.getOutputStream());
                DataInputStream in = new DataInputStream(client.getInputStream());

                String pass = in.readUTF();
                String[] stringArray = pass.split(" ");
                logger.info("user with login: " + stringArray[0] + " connected to server.");

                //проверка логина и пароля на наличие в базе данных
                if (usersAuthorization(stringArray[0], stringArray[1])) {
                    logger.info("user find in data base. authorization was successful");
                    out.writeUTF("correct password");
                    out.writeUTF(serverName);
                } else {
                    logger.info("authorization is not passed. the client entered an invalid password");
                    out.writeUTF("incorrect login or password. connection is closed.");
                    out.flush();
                    in.close();
                    out.close();
                    client.close();
                    continue;
                }

                //получение сообщения от пользователя
                String xml = in.readUTF();
                logger.info("received a message from the user");

                //получение текста сообщения
                String usersName = xml.substring(xml.indexOf("<name>") + 6, xml.indexOf("</name>"));

                //формирование ответного xml и получение его в виде строки для отправки
                String responseMessage = messageToXML(usersName);
                if (responseMessage != null) {
                    out.writeUTF(responseMessage);
                    logger.info("the message is processed.");
                } else {
                    out.writeUTF("your message will be processed later.");
                    logger.info("message is not processed");
                }

                in.close();
                out.close();
                client.close();
            }

        }

        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    //формирование ответного сообщения в заданном формате
    private static String messageToXML(String usersName){
        String xml = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            Element response = document.createElement("response");
            Element message = document.createElement("message");
            Element date = document.createElement("date");

            Text responseMessage = document.createTextNode("Добрый день, " + usersName + ", Ваше сообщение успешно обработано!");
            Text currentDate = document.createTextNode(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));

            document.appendChild(response);
            response.appendChild(message);
            response.appendChild(date);
            message.appendChild(responseMessage);
            date.appendChild(currentDate);

            DOMImplementation impl = document.getImplementation();
            DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
            LSSerializer serializer = implLS.createLSSerializer();
            serializer.getDomConfig().setParameter("format-pretty-print", true);

            LSOutput out = implLS.createLSOutput();
            out.setEncoding("UTF-8");
            out.setByteStream(Files.newOutputStream(Paths.get("response.xml")));
            serializer.write(document, out);
            xml = serializer.writeToString(document);
        }
        catch (IOException | ParserConfigurationException c){
            System.out.println(c.getMessage());
        }
        return xml;
    }


    //проверка логина и пароля на наличие в базе данных
    private static boolean usersAuthorization(String login, String password) {

        boolean authorization = false;

        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:users.db");
            String query = "SELECT COUNT(id) FROM users WHERE login = '" + login + "' AND password = '" + password + "'";
            PreparedStatement pr = connection.prepareStatement(query);

            //если нашли пользователя с данным логином и паролем отправляем true
            if (pr.executeQuery().getInt(1) == 1)
                authorization = true;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return authorization;
    }
}
