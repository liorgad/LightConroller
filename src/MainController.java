import com.danenergy.HttpServer;
import com.danenergy.SerialComm;
import com.danenergy.common.EventBusMessages.ClientRequest;
import com.danenergy.common.EventBusMessages.ResponseToClientRequest;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gnu.io.SerialPort;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dev on 16/10/2017.
 */
public class MainController {

    final static Logger logger = org.apache.logging.log4j.LogManager.getLogger();

    EventBus eventBus;
    LightHttpServer server;
    LightCommands commands;
    SerialComm lightComm;
    String lightPort;

    public MainController(String ip,int port,String lightPort)
    {
        commands = new LightCommands();
        eventBus = new EventBus("LightControllerEventBus");
        eventBus.register(this);
        server = new LightHttpServer(ip,port,eventBus);
        lightComm = new SerialComm();
        this.lightPort = lightPort;
    }

    public void start()
    {
        try {
            lightComm.initializePort(lightPort, 57600, SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
            lightComm.open();
            server.start();
        }
        catch (IOException e)
        {
            logger.error("Error",e);
        }
    }

    public void  stop()
    {
        server.Stop();
        lightComm.close();
    }

    public static void main(String[] args)
    {
        if(args.length == 0)
        {
            System.out.println("Usage: java -jar LightController [listen ip] [listen port] [light com port]");
            System.out.println("Usage: java -jar LightController -c for finding which com ports are available");
            return;
        }

        if(args[0].equals("-c"))
        {
            try {
                SerialComm comm = new SerialComm();
                String[] availPorts = comm.getAvailablePorts();
                for (int i = 0; i < availPorts.length; i++) {
                    System.out.println(i + 1 + ". " + availPorts[i]);
                }
                return;
            }
            catch(Exception e)
            {
                logger.error("Error",e);
            }
        }
        else if(args.length == 3)
        {
            MainController mainCtrl = new MainController(args[0],Integer.parseInt(args[1]),args[2]);

            mainCtrl.start();
            System.out.println("Server listening on " + args[0]+":"+args[1]+" light port is: "+ args[2]);
            //System.out.println("Press any key to stop server....");
            try {
                //System.in.read();

                //mainCtrl.stop();
            }
            catch (Exception e)
            {
                logger.error("Error",e);
            }
        }
        else
        {
            System.out.println("Usage: java -jar LightController [listen ip] [listen port] [light com port]");
            System.out.println("Usage: java -jar LightController -c for finding which com ports are available");
            return;
        }
    }

    @Subscribe
    public void handleClientRequest(ClientRequest request)
    {
        logger.info("MainLogic: received client request for name="+request.getName());
        String currentLight="";

       //Map<String,Object> result = new HashMap<String, Object>();
        if (request.getName().equals("off"))
        {
            byte[] cmd = commands.getLightsOffCommand();
            byte[] result = lightComm.sendReceive(cmd);
            if(null != result)
            {
                currentLight = "Lights On";
            }

        }
        else if(request.getName().equals("on"))
        {
            byte[] cmd = commands.getLightsFullCommand();
            byte[] result = lightComm.sendReceive(cmd);

            if(null != result)
            {
                currentLight = "Lights On";
            }
        }
        else if(StringUtils.isNumeric(request.getName()))
        {
            int dimLevel= Integer.parseInt(request.getName());

            byte[] cmd = commands.getLightsDimLevelPrecent(dimLevel);
            byte[] result = lightComm.sendReceive(cmd);
            if(null != result)
            {
                currentLight = "Lights are on " + request.getName() + "%";
            }
        }
        else
        {

        }

//        Gson gson = new GsonBuilder()
//                .excludeFieldsWithoutExposeAnnotation()
//                .setPrettyPrinting()
//                .create();

        ResponseToClientRequest msg = new ResponseToClientRequest(currentLight);

        logger.info("MainLogic: posting light" +
                " :\n" + msg.getResponse());

        eventBus.post(msg);
    }
}
