import com.danenergy.HttpServer;
import com.danenergy.common.EventBusMessages.ClientRequest;
import com.google.common.eventbus.EventBus;
import org.apache.logging.log4j.Logger;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LightHttpServer extends HttpServer {
    final static Logger logger = org.apache.logging.log4j.LogManager.getLogger();

    EventBus eventBus;

    public LightHttpServer(String ip,int port,EventBus eventBus)
    {
        super(ip,port);
        this.eventBus = eventBus;
    }

    @Override
    public Response serve(IHTTPSession session) {

        Map<String, String> files = new HashMap<String, String>();
        Method method = session.getMethod();

//        if (Method.PUT.equals(method) || Method.POST.equals(method)) {
//            try {
//                session.parseBody(files);
//
//                // get the POST body
//                String postBody = session.getQueryParameterString();
//                // or you can access the POST request's parameters
//
//                List<String> postParameter = session.getParameters().get("parameter");
//
//                // return new Response(postBody); // Or postParameter.
//
//            } catch (IOException ioe) {
//                //return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
//            } catch (ResponseException re) {
//                //return new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
//            }
//        }
        //else if(Method.GET.equals(method))
        if(Method.GET.equals(method))
        {
            String queryString = "Empty";
            try {
                queryString = session.getQueryParameterString();
                Map map = session.getParameters();

                if(map.containsKey("light"))
                {
                    String nameVal = ((List<String>)map.get("light")).get(0);

                    eventBus.post(new ClientRequest(nameVal));

                    //responseWaitSignal.acquire();

                    //String msg = "<html><body><h1></h1>\n" + getResponseToClient() + "</body></html>\n";
                    String msg = getResponseToClient();
//        Map<String, List<String>> parms = session.getParameters();
//        if (parms.get("username") == null) {
//            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
//        } else {
//            msg += "<p>Hello, " + parms.get("username").get(0) + "!</p>";
//        }
                    return newFixedLengthResponse(Response.Status.OK,"application/json",msg);
                }

            }
            catch(Exception e)
            {
                logger.error(e);
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,"Parameter sent: " + queryString + " " + e);
            }
        }

        return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,"Received method :" + method);
    }
}
