package com.company;

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
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        //получение настроек из файла конфигурации
        File configFile = new File("config.ini");
        List<String> usersConfig = readConfig(configFile);

        //проверка наличия данных для подключения
        if (usersConfig.size() != 3){
            System.out.println("incorrect configuration file");
            System.exit(1);
        }

        //запуск подключения сокета
        try (Socket socket = new Socket(usersConfig.get(0), 4321);
             DataOutputStream socketWriter = new DataOutputStream(socket.getOutputStream());
             DataInputStream socketReader = new DataInputStream(socket.getInputStream())) {

                //отправка имени пользователя и пароля (разделитель пробел) на сервер, получение подтверждения правильности введенных данных
                socketWriter.writeUTF(usersConfig.get(1) + " " + usersConfig.get(2));
                String currentString = socketReader.readUTF();

                /* если введенный логин и пароль верны получаем имя сервера и выводим сообщение для пользователя, если
                нет, закрываем приложение с кодом 1 */
                if (currentString.equals("correct password")) {
                    String serverName = socketReader.readUTF();
                    System.out.println("--------Successful connection to the server: " + serverName + "--------" + "\n" + "Now you can send message to the server." + "\n");
                    Thread.sleep(1000);
                }
                else {
                    System.out.println(currentString);
                    System.exit(1);
                }

                //считывание с консоли данных пользователя и сообщения для отправки на сервер
                List<String> usersData = new ArrayList<>();
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Please enter your name");
                String name = reader.readLine();
                System.out.println("Please enter your second name");
                String secondName = reader.readLine();
                System.out.println("Enter a message to send");
                String message = reader.readLine();
                usersData.add(name);
                usersData.add(secondName);
                usersData.add(message);
                System.out.println();

                //формирование файлв xml и получение данных в виде строки для отправки на сервер
                String toServer = dataToXML(usersData);

                /*отправка xml в виде строки на сервер
                если получили исключение в методе завершаем работу приложения */
                if(!(toServer == null))
                    socketWriter.writeUTF(toServer);
                else
                    System.exit(1);

                Thread.sleep(500);
                System.out.println("The message is send.");
                Thread.sleep(500);
                System.out.println("Waiting response from the server.");
                Thread.sleep(500);

                //отрисовка строки загрузки
                for (int i = 0; i < 5; i++){
                    System.out.print("-------");
                    Thread.sleep(500);
                }
                System.out.println();

                //получение ответа от сервера
                String serverResponse = socketReader.readUTF();
                String responseMessage = serverResponse.substring(serverResponse.indexOf("<message>") + 9,
                                                            serverResponse.indexOf("</message>"));
                System.out.println("Server response: " + responseMessage);
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    //чтение файла конфигурации и запись данных в список
    private static List<String> readConfig(File configFile) {
        List<String> config = new ArrayList<>();
        try {
            FileReader fileReader = new FileReader(configFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            while (line != null) {
                config.add(line);
                line = reader.readLine();
            }
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return config;
    }

    //создание xml документа по заданному шаблону
    private static String dataToXML(List<String> usersData){
        String xml = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            Element root = document.createElement("root");
            Element user = document.createElement("user");
            Element name = document.createElement("name");
            Element secondName = document.createElement("secondname");
            Element message = document.createElement("message");
            Element date = document.createElement("date");

            Text userName = document.createTextNode(usersData.get(0));
            Text userSecondName = document.createTextNode(usersData.get(1));
            Text userMessage = document.createTextNode(usersData.get(2));
            Text currentDate = document.createTextNode(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));

            document.appendChild(root);
            root.appendChild(user);
            user.appendChild(name);
            name.appendChild(userName);
            user.appendChild(secondName);
            secondName.appendChild(userSecondName);
            user.appendChild(message);
            message.appendChild(userMessage);
            user.appendChild(date);
            date.appendChild(currentDate);

            DOMImplementation impl = document.getImplementation();
            DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
            LSSerializer serializer = implLS.createLSSerializer();
            serializer.getDomConfig().setParameter("format-pretty-print", true);

            LSOutput out = implLS.createLSOutput();
            out.setEncoding("UTF-8");
            out.setByteStream(Files.newOutputStream(Paths.get("message.xml")));
            serializer.write(document, out);
            xml = serializer.writeToString(document);
        }
        catch (IOException | ParserConfigurationException c){
            System.out.println(c.getMessage());
        }
        return xml;
    }
}
