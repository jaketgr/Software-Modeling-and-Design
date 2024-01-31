import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
public class LoginAuthService {
    public static void main(String[] args) throws Exception {
        int port = 5054;

        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        ServerSocket ss = new ServerSocket(port);

        // running infinite loop for getting
        // client request

        System.out.println("Starting Login Authentication service at port = " + port);

        boolean result = register("localhost", 5000, "localhost", port); // Register with the Registry, so the client know how to find!

        if (!result) {
            System.out.println("Register unsuccessfully!");
            ss.close();
            return;
        }

        int nClients = 0;

        while (true)
        {
            Socket s = null;
            // socket object to receive incoming client requests
            s = ss.accept();
            nClients++;
            System.out.println("A new client is connected : " + s + " client number: " + nClients);
            serve(s, nClients);

        }
    }

    private static void serve(Socket socket, int clientID) throws Exception {
        DataInputStream reader = new DataInputStream(socket.getInputStream());
        Gson gson = new Gson();

        String msg = reader.readUTF();
        User user = gson.fromJson(msg, User.class);
        String username = user.getUserName();
        String password = user.getPassword();

        System.out.println("Username from client " + clientID + ": " + user.getUserName());

        //connect to database
        Class.forName("org.sqlite.JDBC");
        DataAccess adapter = new DataAdapter();
        adapter.connect("jdbc:sqlite:store.db");
        //load user from DB if it exists
        user = adapter.loadUser(username, password);
        String ans = gson.toJson(user);
        //send user back
        DataOutputStream printer = new DataOutputStream(socket.getOutputStream());
        printer.writeUTF(ans);
        printer.flush();
        adapter.disconnect();
        printer.close();
        reader.close();
        socket.close();
    }

    private static boolean register(String regHost, int regPort, String myHost, int myPort) throws IOException {
        ServiceInfoModel info = new ServiceInfoModel();
        info.serviceCode = ServiceInfoModel.AUTH_LOGIN_SERVICE;
        info.serviceHostAddress = myHost;
        info.serviceHostPort = myPort;

        Gson gson = new Gson();

        ServiceMessageModel req = new ServiceMessageModel();
        req.code = ServiceMessageModel.SERVICE_PUBLISH_REQUEST;
        req.data = gson.toJson(info);

        Socket socket = new Socket(regHost, regPort);

        DataOutputStream printer = new DataOutputStream(socket.getOutputStream());
        printer.writeUTF(gson.toJson(req));
        printer.flush();

        DataInputStream reader = new DataInputStream(socket.getInputStream());
        String msg = reader.readUTF();
        printer.close();
        reader.close();
        socket.close();


        System.out.println("Message from server: " + msg);
        ServiceMessageModel res = gson.fromJson(msg, ServiceMessageModel.class);

        if (res.code == ServiceMessageModel.SERVICE_PUBLISH_OK) {
            return true;
        }
        else { return false; }
    }

    private static void deregister() {

    }
}